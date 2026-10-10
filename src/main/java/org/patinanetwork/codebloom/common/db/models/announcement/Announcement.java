package org.patinanetwork.codebloom.common.db.models.announcement;

import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Announcement {

    private String id;

    private OffsetDateTime createdAt;

    private OffsetDateTime expiresAt;

    @Builder.Default
    private Optional<Boolean> showTimer = Optional.of(false);

    private String message;
}
