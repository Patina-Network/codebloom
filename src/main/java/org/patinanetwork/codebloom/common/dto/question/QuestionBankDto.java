package org.patinanetwork.codebloom.common.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.dto.question.topic.QuestionTopicDto;

@Getter
@Builder
@Jacksonized
@ToString
@EqualsAndHashCode
public class QuestionBankDto {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String questionSlug;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private QuestionDifficulty questionDifficulty;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String questionTitle;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long questionNumber;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String questionLink;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String description;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private float acceptanceRate;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private OffsetDateTime createdAt;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<QuestionTopicDto> topics;

    public static QuestionBankDto fromQuestionBank(final QuestionBank questionBank) {
        return QuestionBankDto.builder()
                .id(questionBank.getId())
                .questionSlug(questionBank.getQuestionSlug())
                .questionDifficulty(questionBank.getQuestionDifficulty())
                .questionTitle(questionBank.getQuestionTitle())
                .questionNumber(questionBank.getQuestionNumber())
                .questionLink(questionBank.getQuestionLink())
                .description(questionBank.getDescription().orElse(null))
                .acceptanceRate(questionBank.getAcceptanceRate())
                .createdAt(questionBank.getCreatedAt())
                .topics(
                        questionBank.getTopics() != null
                                ? questionBank.getTopics().stream()
                                        .map(t -> QuestionTopicDto.fromQuestionTopic(t))
                                        .toList()
                                : List.of())
                .build();
    }
}
