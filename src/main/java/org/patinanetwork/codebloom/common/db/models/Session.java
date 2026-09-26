package org.patinanetwork.codebloom.common.db.models;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.patinanetwork.codebloom.common.db.helper.annotations.NotNullColumn;
import org.patinanetwork.codebloom.common.db.helper.annotations.NullColumn;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class Session {

    @Builder.Default
    private Optional<String> id = Optional.empty();

    @NotNullColumn
    private String userId;

    @NotNullColumn
    private LocalDateTime expiresAt;

    /** can be null if it's an old entry */
    @NullColumn
    private OffsetDateTime createdAt;

    // public Session(final String userId, final LocalDateTime expiresAt) {
    // this.userId = userId;
    // this.expiresAt = expiresAt;
    // }
}
