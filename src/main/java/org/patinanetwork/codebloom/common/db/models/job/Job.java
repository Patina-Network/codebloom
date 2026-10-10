package org.patinanetwork.codebloom.common.db.models.job;

import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class Job {

    private String id;

    private OffsetDateTime createdAt;

    @Builder.Default
    private Optional<OffsetDateTime> processedAt = Optional.empty();

    @Builder.Default
    private Optional<OffsetDateTime> completedAt = Optional.empty();

    private OffsetDateTime nextAttemptAt;

    private JobStatus status;

    private String questionId;

    private int attempts;
}
