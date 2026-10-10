package org.patinanetwork.codebloom.common.db.models;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class Session {

    @Builder.Default
    private Optional<String> id = Optional.empty();

    private String userId;

    private LocalDateTime expiresAt;

    /** can be empty if it's an old entry */
    @Builder.Default
    private Optional<OffsetDateTime> createdAt = Optional.empty();

    // public Session(final String userId, final LocalDateTime expiresAt) {
    // this.userId = userId;
    // this.expiresAt = expiresAt;
    // }
}
