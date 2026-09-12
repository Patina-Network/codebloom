package org.patinanetwork.codebloom.common.db.models.question.bank;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.patinanetwork.codebloom.common.db.helper.annotations.JoinColumn;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.topic.QuestionTopic;

@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class QuestionBank {

    private String id;

    private String questionSlug;

    private QuestionDifficulty questionDifficulty;

    private String questionTitle;

    private int questionNumber;

    private String questionLink;

    @Builder.Default
    private Optional<String> description = Optional.empty();

    private boolean isPaidOnly;

    private float acceptanceRate;

    private OffsetDateTime createdAt;

    /** Join field, update/create with {@link QuestionTopicRepository} */
    @JoinColumn
    private List<QuestionTopic> topics;
}
