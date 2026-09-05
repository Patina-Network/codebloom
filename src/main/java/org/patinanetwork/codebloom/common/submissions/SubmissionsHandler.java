package org.patinanetwork.codebloom.common.submissions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.potd.POTD;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.models.question.topic.QuestionTopic;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.user.UserWithScore;
import org.patinanetwork.codebloom.common.db.repos.job.JobRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.potd.POTDRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.questionbank.QuestionBankRepository;
import org.patinanetwork.codebloom.common.db.repos.question.topic.QuestionTopicRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.db.repos.user.options.UserFilterOptions;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeQuestion;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeTopicTag;
import org.patinanetwork.codebloom.common.leetcode.score.ScoreCalculator;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.reporter.Reporter;
import org.patinanetwork.codebloom.common.reporter.throttled.ThrottledReporter;
import org.patinanetwork.codebloom.common.submissions.object.AcceptedSubmission;
import org.patinanetwork.codebloom.common.utils.pair.Pair;
import org.springframework.stereotype.Component;

/**
 * The submission logic is abstracted because it gets reused in two different parts of the app: the submissions API and
 * the automated recurring task.
 */
@Component
public class SubmissionsHandler {

    private final QuestionRepository questionRepository;
    private final ThrottledLeetcodeClient leetcodeClient;
    private final LeaderboardRepository leaderboardRepository;
    private final POTDRepository potdRepository;
    private final UserRepository userRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final JobRepository jobRepository;
    private final Reporter throttledReporter;
    private final QuestionBankRepository questionBankRepository;

    private boolean isValid(final LocalDateTime createdAt) {
        // TODO - Replace EST locked functionality.
        ZoneId est = ZoneId.of("America/New_York");
        ZonedDateTime now = ZonedDateTime.now(est);
        ZonedDateTime cutoff = createdAt
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(est)
                .toLocalDate()
                .plusDays(1)
                .atTime(20, 0)
                .atZone(est);
        return now.isBefore(cutoff);
    }

    public SubmissionsHandler(
            final QuestionRepository questionRepository,
            final ThrottledLeetcodeClient throttledLeetcodeClient,
            final LeaderboardRepository leaderboardRepository,
            final POTDRepository potdRepository,
            final UserRepository userRepository,
            final QuestionTopicRepository questionTopicRepository,
            final ThrottledReporter throttledReporter,
            final JobRepository jobRepository,
            final QuestionBankRepository questionBankRepository) {
        this.questionRepository = questionRepository;
        this.leetcodeClient = throttledLeetcodeClient;
        this.leaderboardRepository = leaderboardRepository;
        this.potdRepository = potdRepository;
        this.userRepository = userRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.jobRepository = jobRepository;
        this.throttledReporter = throttledReporter;
        this.questionBankRepository = questionBankRepository;
    }

    private static QuestionTopic topicTagToQuestionTopic(final LeetcodeTopicTag topicTag) {
        return QuestionTopic.builder()
                .topicSlug(topicTag.getSlug())
                .topic(LeetcodeTopicEnum.fromValue(topicTag.getSlug()))
                .build();
    }

    public static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private QuestionBank refetchAndBackfillQuestion(
            final String slug, final QuestionBank existingBankQuestion, final boolean fast) {
        LeetcodeQuestion question =
                fast ? leetcodeClient.findQuestionBySlugFast(slug) : leetcodeClient.findQuestionBySlug(slug);

        QuestionBank refetchedQuestion = QuestionBank.builder()
                .questionSlug(question.getTitleSlug())
                .questionDifficulty(QuestionDifficulty.valueOf(question.getDifficulty()))
                .questionTitle(question.getQuestionTitle())
                .questionNumber(question.getQuestionId())
                .questionLink("https://leetcode.com/problems/" + question.getTitleSlug())
                .description(Optional.ofNullable(question.getQuestion()))
                .acceptanceRate(question.getAcceptanceRate())
                .topics(question.getTopics().stream()
                        .map(SubmissionsHandler::topicTagToQuestionTopic)
                        .toList())
                .build();

        if (existingBankQuestion != null) {
            refetchedQuestion.setId(existingBankQuestion.getId());
            questionBankRepository.updateQuestion(refetchedQuestion);
        }

        return refetchedQuestion;
    }

    public ArrayList<AcceptedSubmission> handleSubmissions(
            final List<LeetcodeSubmission> leetcodeSubmissions, final User user, boolean fast) {
        ArrayList<AcceptedSubmission> acceptedSubmissions = new ArrayList<>();
        Optional<POTD> potd = potdRepository.getCurrentPOTD();

        var questionMap = leetcodeSubmissions.parallelStream()
                .filter(distinctByKey(LeetcodeSubmission::getTitleSlug))
                .map(s -> {
                    String slug = s.getTitleSlug();

                    Optional<QuestionBank> existingBankQuestion = questionBankRepository.getQuestionBySlug(slug);

                    boolean hasDescription = existingBankQuestion
                            .flatMap(QuestionBank::getDescription)
                            .filter(description -> !description.isBlank())
                            .isPresent();

                    QuestionBank bankQuestion = hasDescription
                            ? existingBankQuestion.get()
                            : refetchAndBackfillQuestion(slug, existingBankQuestion.orElse(null), fast);

                    return Pair.of(slug, bankQuestion);
                })
                .collect(Collectors.toMap(p -> p.getLeft(), p -> p.getRight()));

        for (LeetcodeSubmission leetcodeSubmission : leetcodeSubmissions) {
            if (!leetcodeSubmission.getStatusDisplay().equals("Accepted")) {
                continue;
            }

            if (questionRepository.questionExistsBySubmissionId(String.valueOf(leetcodeSubmission.getId()))) {
                continue;
            }

            boolean questionExists = questionRepository
                    .getQuestionBySlugAndUserId(leetcodeSubmission.getTitleSlug(), user.getId())
                    .isPresent();

            float multiplier = potd.filter(currentPotd -> isValid(currentPotd.getCreatedAt())
                            && Objects.equals(currentPotd.getSlug(), leetcodeSubmission.getTitleSlug()))
                    .map(POTD::getMultiplier)
                    .orElse(1.0f);

            QuestionBank bankQuestion = questionMap.get(leetcodeSubmission.getTitleSlug());

            // If the submission is before the leaderboard started, points awarded = 0
            Optional<Leaderboard> recentLeaderboard = leaderboardRepository.getRecentLeaderboardMetadata();

            // This should never be happening as there should always be an existing
            // leaderboard to fall on. Howerver, race conditions could trigger this problem.
            if (recentLeaderboard.isEmpty()) {
                throw new RuntimeException("No recent leaderboard found.");
            }

            boolean isTooLate = recentLeaderboard.get().getCreatedAt().isAfter(leetcodeSubmission.getTimestamp());

            // int basePoints = switch (leetcodeQuestion.getDifficulty().toUpperCase()) {
            // case "EASY" -> 100;
            // case "MEDIUM" -> 300;
            // case "HARD" -> 600;
            // default -> 0;
            // };

            int points;
            if (questionExists || isTooLate) {
                points = 0;
            } else {
                points = ScoreCalculator.calculateScore(
                        bankQuestion.getQuestionDifficulty(), bankQuestion.getAcceptanceRate(), multiplier);
            }

            // throttledReporter.log(Report.builder()
            // .data(String.format("""
            // Score Distribution Report

            // Leetcode Username: %s
            // Difficulty: %s (%d pts)
            // Acceptance Rate: %.2f
            // Question Multiplier: %.2f
            // Total: %d
            // """,
            // user.getLeetcodeUsername(),
            // leetcodeQuestion.getDifficulty(),
            // basePoints,
            // leetcodeQuestion.getAcceptanceRate(),
            // multiplier,
            // points
            // ))
            // .build());

            Question newQuestion = Question.builder()
                    .userId(user.getId())
                    .questionSlug(bankQuestion.getQuestionSlug())
                    .questionDifficulty(bankQuestion.getQuestionDifficulty())
                    .questionNumber(bankQuestion.getQuestionNumber())
                    .questionLink("https://leetcode.com/problems/" + bankQuestion.getQuestionSlug())
                    .questionTitle(bankQuestion.getQuestionTitle())
                    .description(bankQuestion.getDescription())
                    .pointsAwarded(Optional.of(points))
                    .acceptanceRate(bankQuestion.getAcceptanceRate())
                    .submittedAt(leetcodeSubmission.getTimestamp())
                    .runtime(Optional.empty())
                    .memory(Optional.empty())
                    .code(Optional.empty())
                    .language(Optional.empty())
                    .submissionId(Optional.of(String.valueOf(leetcodeSubmission.getId())))
                    .build();

            var createdQuestion = questionRepository.createQuestion(newQuestion);

            Job newJob = Job.builder()
                    .questionId(newQuestion.getId())
                    .status(JobStatus.INCOMPLETE)
                    .build();

            jobRepository.createJob(newJob);

            bankQuestion.getTopics().stream()
                    .forEach(topic -> questionTopicRepository.createQuestionTopic(QuestionTopic.builder()
                            .questionId(newQuestion.getId())
                            .topicSlug(topic.getTopicSlug())
                            .topic(LeetcodeTopicEnum.fromValue(topic.getTopicSlug()))
                            .build()));

            acceptedSubmissions.add(
                    new AcceptedSubmission(bankQuestion.getQuestionTitle(), createdQuestion.getId(), points));

            UserWithScore recentUserMetadata = userRepository.getUserWithScoreByIdAndLeaderboardId(
                    user.getId(), recentLeaderboard.get().getId(), UserFilterOptions.DEFAULT);

            leaderboardRepository.updateUserPointsFromLeaderboard(
                    recentLeaderboard.get().getId(), user.getId(), recentUserMetadata.getTotalScore() + points);
        }

        return acceptedSubmissions;
    }
}
