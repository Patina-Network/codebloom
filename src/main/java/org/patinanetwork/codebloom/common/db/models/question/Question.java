package org.patinanetwork.codebloom.common.db.models.question;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.patinanetwork.codebloom.common.db.helper.annotations.JoinColumn;
import org.patinanetwork.codebloom.common.db.models.question.topic.QuestionTopic;

@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode
@ToString
public class Question {

    private String id;

    private String userId;

    private String questionSlug;

    private String questionTitle;

    private QuestionDifficulty questionDifficulty;

    private long questionNumber;

    private String questionLink;

    @Builder.Default
    private Optional<String> description = Optional.empty();

    /**
     * Optional for the case of future proofing. We might end up using AI to award some points, so there might be a case
     * where we create the DB entry and then pass it to a message queue to use AI and calculate a score.
     */
    @Builder.Default
    private Optional<Integer> pointsAwarded = Optional.empty();

    private float acceptanceRate;

    private LocalDateTime createdAt;

    private LocalDateTime submittedAt;

    @Builder.Default
    private Optional<String> runtime = Optional.empty();

    @Builder.Default
    private Optional<String> memory = Optional.empty();

    @Builder.Default
    private Optional<String> code = Optional.empty();

    @Builder.Default
    private Optional<String> language = Optional.empty();

    // Not every submission will have this.
    @Builder.Default
    private Optional<String> submissionId = Optional.empty();

    /** Join field, update/create with {@link QuestionTopicRepository} */
    @JoinColumn
    private List<QuestionTopic> topics;
}
