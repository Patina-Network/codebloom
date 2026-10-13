package org.patinanetwork.codebloom.common.db.models.achievements;

import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;

@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Achievement {

    private String id;

    private String userId;

    private String leaderboardId;

    private AchievementPlaceEnum place;

    /** An empty value indicates the global leaderboard. */
    @Builder.Default
    private Optional<Tag> leaderboard = Optional.empty();

    private String title;

    @Builder.Default
    private Optional<String> description = Optional.empty();

    @Builder.Default
    private boolean isActive = true;

    private OffsetDateTime createdAt;

    @Builder.Default
    private Optional<OffsetDateTime> deletedAt = Optional.empty();
}
