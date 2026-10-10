package org.patinanetwork.codebloom.common.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.models.usertag.UserTag;
import org.patinanetwork.codebloom.common.dto.achievement.AchievementDto;

@Getter
@Builder
@Jacksonized
@ToString
@EqualsAndHashCode
public class UserDto {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String discordId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String discordName;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String leetcodeUsername;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String nickname;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean admin;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String profileUrl;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UserTag> tags;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AchievementDto> achievements;

    public static UserDto fromUser(final User user) {
        return UserDto.builder()
                .id(user.getId())
                .discordId(user.getDiscordId())
                .discordName(user.getDiscordName())
                .leetcodeUsername(user.getLeetcodeUsername().orElse(null))
                .nickname(user.getNickname().orElse(null))
                .admin(user.isAdmin())
                .profileUrl(user.getProfileUrl().orElse(null))
                .tags(user.getTags())
                .achievements(user.getAchievements().stream()
                        .map(AchievementDto::fromAchievement)
                        .toList())
                .build();
    }
}
