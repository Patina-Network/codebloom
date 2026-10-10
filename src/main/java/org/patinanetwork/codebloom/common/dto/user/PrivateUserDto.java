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

/** This should only ever be used to do a verification on a user's Leetcode profile. Do NOT leak the key. */
@Getter
@Builder
@Jacksonized
@ToString
@EqualsAndHashCode
public class PrivateUserDto {

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
    private String verifyKey;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private String schoolEmail;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AchievementDto> achievements;

    public static PrivateUserDto fromUser(final User user) {
        return PrivateUserDto.builder()
                .id(user.getId())
                .discordId(user.getDiscordId())
                .discordName(user.getDiscordName())
                .leetcodeUsername(user.getLeetcodeUsername().orElse(null))
                .nickname(user.getNickname().orElse(null))
                .admin(user.isAdmin())
                .profileUrl(user.getProfileUrl().orElse(null))
                .tags(user.getTags())
                .verifyKey(user.getVerifyKey())
                .schoolEmail(user.getSchoolEmail().orElse(null))
                .achievements(user.getAchievements().stream()
                        .map(AchievementDto::fromAchievement)
                        .toList())
                .build();
    }
}
