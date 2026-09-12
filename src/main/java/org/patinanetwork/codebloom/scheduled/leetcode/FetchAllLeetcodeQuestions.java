package org.patinanetwork.codebloom.scheduled.leetcode;

import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTask;
import org.patinanetwork.codebloom.common.db.models.task.BackgroundTaskEnum;
import org.patinanetwork.codebloom.common.db.repos.question.questionbank.QuestionBankRepository;
import org.patinanetwork.codebloom.common.db.repos.task.BackgroundTaskRepository;
import org.patinanetwork.codebloom.common.env.Env;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!ci")
public class FetchAllLeetcodeQuestions {

    private final BackgroundTaskRepository backgroundTaskRepository;
    private final LeetcodeClient leetcodeClient;
    private final QuestionBankRepository questionBankRepository;
    private final Env env;

    public FetchAllLeetcodeQuestions(
            final BackgroundTaskRepository backgroundTaskRepository,
            final LeetcodeClient leetcodeClient,
            final QuestionBankRepository questionBankRepository,
            final Env env) {
        this.backgroundTaskRepository = backgroundTaskRepository;
        this.leetcodeClient = leetcodeClient;
        this.questionBankRepository = questionBankRepository;
        this.env = env;
    }

    @PostConstruct
    public void init() {
        if (env.isDev()) {
            log.info("Running instant question bank sync");
            runStartupSynchronization();
        }
    }

    @Async
    private void runStartupSynchronization() {
        updateQuestionBank();
    }

    @Scheduled(initialDelay = 1, fixedDelay = 3, timeUnit = TimeUnit.HOURS)
    public void updateQuestionBank() {
        Optional<BackgroundTask> recentLeetcodeTask =
                backgroundTaskRepository.getMostRecentlyCompletedBackgroundTaskByTaskEnum(
                        BackgroundTaskEnum.LEETCODE_QUESTION_BANK);

        if (recentLeetcodeTask.isPresent()) {
            if (StandardizedOffsetDateTime.now()
                    .isBefore(recentLeetcodeTask.get().getCompletedAt().plusHours(16))) {
                log.error("Not time yet to resync question bank");
                return;
            }
        }

        List<LeetcodeQuestion> leetcodeQuestions = leetcodeClient.getAllProblems();

        List<QuestionBank> bankLeetcodeQuestion = new ArrayList<>();
        for (LeetcodeQuestion question : leetcodeQuestions) {
            QuestionDifficulty difficulty;

            switch (question.getDifficulty().toUpperCase()) {
                case "EASY":
                    difficulty = QuestionDifficulty.Easy;
                    break;
                case "MEDIUM":
                    difficulty = QuestionDifficulty.Medium;
                    break;
                case "HARD":
                    difficulty = QuestionDifficulty.Hard;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown difficulty: " + question.getDifficulty());
            }

            QuestionBank bankQuestion = QuestionBank.builder()
                    .questionSlug(question.getTitleSlug())
                    .questionDifficulty(difficulty)
                    .questionTitle(question.getQuestionTitle())
                    .questionNumber(question.getQuestionId())
                    .questionLink(question.getLink())
                    .acceptanceRate(question.getAcceptanceRate())
                    .createdAt(StandardizedOffsetDateTime.now())
                    .build();

            bankLeetcodeQuestion.add(bankQuestion);
        }

        List<QuestionBank> bankQuestion = questionBankRepository.getAllQuestions();

        Set<String> slugsFromLeetcode =
                bankLeetcodeQuestion.stream().map(QuestionBank::getQuestionSlug).collect(Collectors.toSet());

        Set<String> slugsFromDb =
                bankQuestion.stream().map(QuestionBank::getQuestionSlug).collect(Collectors.toSet());

        List<QuestionBank> missingInDb = bankLeetcodeQuestion.stream()
                .filter(q -> !slugsFromDb.contains(q.getQuestionSlug()))
                .collect(Collectors.toList());

        for (QuestionBank question : missingInDb) {
            questionBankRepository.createQuestion(question);
        }

        List<QuestionBank> deletedFromLeetcode = bankQuestion.stream()
                .filter(q -> !q.isPaidOnly())
                .filter(q -> !slugsFromLeetcode.contains(q.getQuestionSlug()))
                .collect(Collectors.toList());

        for (QuestionBank question : deletedFromLeetcode) {
            questionBankRepository.deleteQuestionById(question.getId());
        }

        backgroundTaskRepository.createBackgroundTask(BackgroundTask.builder()
                .task(BackgroundTaskEnum.LEETCODE_QUESTION_BANK)
                .completedAt(OffsetDateTime.now())
                .build());
    }
}
