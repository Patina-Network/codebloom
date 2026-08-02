package org.patinanetwork.codebloom.common.db.models.achievements;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.patinanetwork.codebloom.common.db.helper.annotations.NotNullColumn;
import org.patinanetwork.codebloom.common.db.helper.annotations.NullColumn;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;

@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Achievement {

    @NotNullColumn
    private String id;

    @NotNullColumn
    private String userId;

    @NotNullColumn
    private String leaderboardId;

    @NotNullColumn
    private AchievementPlaceEnum place;

    /** `null` indicates global leaderboard. */
    @NullColumn
    private Tag leaderboard;

    @NotNullColumn
    private String title;

    @NullColumn
    private String description;

    @Builder.Default
    @NotNullColumn
    private boolean isActive = true;

    @NotNullColumn
    private OffsetDateTime createdAt;

    @NullColumn
    private OffsetDateTime deletedAt;
}
