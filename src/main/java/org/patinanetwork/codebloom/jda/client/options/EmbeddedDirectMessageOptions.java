package org.patinanetwork.codebloom.jda.client.options;

import java.awt.Color;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmbeddedDirectMessageOptions {
    private String title;
    private String description;
    private String footerText;
    private String footerIcon;
    private Color color;
}
