package org.patinanetwork.codebloom.common.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.patinanetwork.codebloom.common.db.models.question.Question;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.dto.question.topic.QuestionTopicDto;

@Getter
@Builder
@Jacksonized
@ToString
@EqualsAndHashCode
public class QuestionDto {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String questionSlug;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String questionTitle;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private QuestionDifficulty questionDifficulty;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long questionNumber;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String questionLink;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String description;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private Integer pointsAwarded;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private float acceptanceRate;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime submittedAt;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String runtime;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String memory;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String code;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String language;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String submissionId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<QuestionTopicDto> topics;

    public static QuestionDto fromQuestion(final Question question) {
        return QuestionDto.builder()
                .id(question.getId())
                .userId(question.getUserId())
                .questionSlug(question.getQuestionSlug())
                .questionTitle(question.getQuestionTitle())
                .questionDifficulty(question.getQuestionDifficulty())
                .questionNumber(question.getQuestionNumber())
                .questionLink(question.getQuestionLink())
                .description(question.getDescription().orElse(null))
                .pointsAwarded(question.getPointsAwarded().orElse(null))
                .acceptanceRate(question.getAcceptanceRate())
                .createdAt(question.getCreatedAt())
                .submittedAt(question.getSubmittedAt())
                .runtime(question.getRuntime().orElse(null))
                .memory(question.getMemory().orElse(null))
                .code(question.getCode().orElse(null))
                .language(question.getLanguage().orElse(null))
                .submissionId(question.getSubmissionId().orElse(null))
                .topics(question.getTopics().stream()
                        .map(q -> QuestionTopicDto.fromQuestionTopic(q))
                        .toList())
                .build();
    }
}
