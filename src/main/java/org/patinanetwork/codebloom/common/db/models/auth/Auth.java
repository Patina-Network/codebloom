package org.patinanetwork.codebloom.common.db.models.auth;

import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
@EqualsAndHashCode
@ToString
public class Auth {

    private String id;

    private String token;

    @Builder.Default
    private Optional<String> csrf = Optional.empty();

    private OffsetDateTime createdAt;
}
