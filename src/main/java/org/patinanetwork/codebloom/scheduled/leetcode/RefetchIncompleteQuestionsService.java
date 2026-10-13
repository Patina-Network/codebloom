package org.patinanetwork.codebloom.scheduled.leetcode;

import java.util.concurrent.TimeUnit;
import org.patinanetwork.codebloom.common.components.IncompleteQuestionRequeuer;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!ci")
public class RefetchIncompleteQuestionsService {

    private final IncompleteQuestionRequeuer incompleteQuestionRequeuer;

    public RefetchIncompleteQuestionsService(final IncompleteQuestionRequeuer incompleteQuestionRequeuer) {
        this.incompleteQuestionRequeuer = incompleteQuestionRequeuer;
    }

    @Scheduled(initialDelay = 0, fixedDelay = 24, timeUnit = TimeUnit.HOURS)
    public void refetchIncompleteQuestions() {
        incompleteQuestionRequeuer.requeueIncompleteQuestions();
    }
}
