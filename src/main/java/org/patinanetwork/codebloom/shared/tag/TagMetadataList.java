package org.patinanetwork.codebloom.shared.tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.shared.TypableObject;

public class TagMetadataList {
    public static final Map<Tag, TagMetadataObject> ENUM_TO_TAG_METADATA = Collections.unmodifiableMap(generate());

    private static Map<Tag, TagMetadataObject> generate() {
        return Arrays.stream(Tag.values())
                .collect(Collectors.toMap(tag -> tag, TagMetadataList::buildMetadata, (a, b) -> a, LinkedHashMap::new));
    }

    private static TagMetadataObject buildMetadata(Tag tag) {
        return TagMetadataObject.builder()
                .shortName(tag.getShortName())
                .name(tag.getName())
                .apiKey(tag.getApiKey())
                .alt(tag.getAlt())
                .build();
    }

    @Getter
    @Builder
    public static class TagMetadataObject implements TypableObject {
        private static final String TS_TYPE = """
                export type TagMetadataObject = {
                  shortName: string;
                  name: string;
                  apiKey: string;
                  alt: string;
                };

                """.stripIndent();

        private final String shortName;
        private final String name;
        private final String apiKey;
        private final String alt;

        @Override
        public String tsType() {
            return TS_TYPE;
        }
    }
}
