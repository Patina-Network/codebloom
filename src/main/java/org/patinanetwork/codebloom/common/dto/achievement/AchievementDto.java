package org.patinanetwork.codebloom.common.dto.achievement;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.patinanetwork.codebloom.common.db.models.achievements.Achievement;
import org.patinanetwork.codebloom.common.db.models.achievements.AchievementPlaceEnum;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;

@Builder
@Getter
@Jacksonized
@ToString
@EqualsAndHashCode
public class AchievementDto {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private AchievementPlaceEnum place;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private Tag leaderboard;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String leaderboardId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String description;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isActive;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private OffsetDateTime createdAt;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private OffsetDateTime deletedAt;

    public static AchievementDto fromAchievement(final Achievement achievement) {
        return AchievementDto.builder()
                .id(achievement.getId())
                .userId(achievement.getUserId())
                .leaderboard(achievement.getLeaderboard())
                .place(achievement.getPlace())
                .title(achievement.getTitle())
                .leaderboardId(achievement.getLeaderboardId())
                .description(achievement.getDescription())
                .isActive(achievement.isActive())
                .createdAt(achievement.getCreatedAt())
                .deletedAt(achievement.getDeletedAt())
                .build();
    }
}
