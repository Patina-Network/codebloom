package org.patinanetwork.codebloom.api.admin.body.jda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CreateDiscordAnnouncementBody {

    @NotBlank
    @Size(min = 1, max = 4096)
    private String message;
}
