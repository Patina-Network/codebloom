package org.patinanetwork.codebloom.common.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.patinanetwork.codebloom.common.db.models.question.QuestionDifficulty;
import org.patinanetwork.codebloom.common.db.models.question.QuestionWithUser;
import org.patinanetwork.codebloom.common.dto.question.topic.QuestionTopicDto;

@Getter
@Builder
@Jacksonized
@ToString
@EqualsAndHashCode
public class QuestionWithUserDto {

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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String discordName;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String leetcodeUsername;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String nickname;

    public static QuestionWithUserDto fromQuestionWithUser(final QuestionWithUser questionWithUser) {
        return QuestionWithUserDto.builder()
                .id(questionWithUser.getId())
                .userId(questionWithUser.getUserId())
                .questionSlug(questionWithUser.getQuestionSlug())
                .questionTitle(questionWithUser.getQuestionTitle())
                .questionDifficulty(questionWithUser.getQuestionDifficulty())
                .questionNumber(questionWithUser.getQuestionNumber())
                .questionLink(questionWithUser.getQuestionLink())
                .description(questionWithUser.getDescription().orElse(null))
                .pointsAwarded(questionWithUser.getPointsAwarded().orElse(null))
                .acceptanceRate(questionWithUser.getAcceptanceRate())
                .createdAt(questionWithUser.getCreatedAt())
                .submittedAt(questionWithUser.getSubmittedAt())
                .runtime(questionWithUser.getRuntime().orElse(null))
                .memory(questionWithUser.getMemory().orElse(null))
                .code(questionWithUser.getCode().orElse(null))
                .language(questionWithUser.getLanguage().orElse(null))
                .submissionId(questionWithUser.getSubmissionId().orElse(null))
                .topics(
                        questionWithUser.getTopics() != null
                                ? questionWithUser.getTopics().stream()
                                        .map(QuestionTopicDto::fromQuestionTopic)
                                        .toList()
                                : List.of())
                .discordName(questionWithUser.getDiscordName().orElse(null))
                .leetcodeUsername(questionWithUser.getLeetcodeUsername().orElse(null))
                .nickname(questionWithUser.getNickname().orElse(null))
                .build();
    }
}
