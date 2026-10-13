package org.patinanetwork.codebloom.common.db.models.potd;

import java.time.LocalDateTime;
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
public class POTD {

    private String id;

    private String title;

    private String slug;

    private float multiplier;

    private LocalDateTime createdAt;
}
