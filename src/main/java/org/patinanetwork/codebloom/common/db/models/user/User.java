package org.patinanetwork.codebloom.common.db.models.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.patinanetwork.codebloom.common.db.helper.annotations.JoinColumn;
import org.patinanetwork.codebloom.common.db.helper.annotations.NotNullColumn;
import org.patinanetwork.codebloom.common.db.helper.annotations.NullColumn;
import org.patinanetwork.codebloom.common.db.models.achievements.Achievement;
import org.patinanetwork.codebloom.common.db.models.usertag.UserTag;

@Getter
@Setter
@SuperBuilder
@ToString
@EqualsAndHashCode
public class User {

    @NotNullColumn
    private String id;

    // Even though discord IDs are integers, they are very large so we just use
    // String instead.
    @NotNullColumn
    private String discordId;

    @NotNullColumn
    private String discordName;

    @NullColumn
    private String leetcodeUsername;

    @NullColumn
    private String nickname;

    @NotNullColumn
    private boolean admin;

    @NullColumn
    private String profileUrl;

    @NotNullColumn
    private String verifyKey;

    @NullColumn
    private String schoolEmail;

    /** can be null if it's an old entry */
    @NullColumn
    private OffsetDateTime createdAt;

    /**
     * If you want to update tags in the database, you have to use the
     * {@link org.patinanetwork.codebloom.common.db.repos.usertag.UserTagRepository}
     */
    @JoinColumn
    @Builder.Default
    private List<UserTag> tags = new ArrayList<>();

    /**
     * If you want to update achievements in the database, you have to use the
     * {@link org.patinanetwork.codebloom.common.db.repos.achievements.AchievementRepository}
     */
    @JoinColumn
    @Builder.Default
    private List<Achievement> achievements = new ArrayList<>();

    /**
     * This operation is permitted, but the tag will not be used in update operations in the UserRepository. Instead
     * call this method with the parameter being the add method from
     * {@link org.patinanetwork.codebloom.common.db.repos.usertag.UserTagRepository}
     *
     * <p>Essentially, this operation should be used to keep the User model up-to-date with any Tag operations without
     * needlessly querying the database for the full User object.
     */
    public void addTag(final UserTag tag) {
        if (tag == null) {
            return;
        }

        tags.add(tag);
    }
}
