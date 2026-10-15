package org.patinanetwork.codebloom.utilities.generator.complex;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.patinanetwork.codebloom.common.db.models.question.topic.LeetcodeTopicEnum;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.shared.tag.TagMetadataList;
import org.patinanetwork.codebloom.shared.topic.TopicMetadataList;

class ComplexJSTypesGeneratorTest {

    @Test
    void tagMetadataListContainsAllTags() {
        assertEquals(Tag.values().length, TagMetadataList.ENUM_TO_TAG_METADATA.size());
    }

    @Test
    void tagMetadataListHasShortNameField() {
        for (var metadata : TagMetadataList.ENUM_TO_TAG_METADATA.values()) {
            assertNotNull(metadata.getShortName());
        }
    }

    @Test
    void tagMetadataListHasNameField() {
        for (var metadata : TagMetadataList.ENUM_TO_TAG_METADATA.values()) {
            assertNotNull(metadata.getName());
        }
    }

    @Test
    void tagMetadataListHasApiKeyField() {
        for (var metadata : TagMetadataList.ENUM_TO_TAG_METADATA.values()) {
            assertNotNull(metadata.getApiKey());
        }
    }

    @Test
    void tagMetadataListHasAltField() {
        for (var metadata : TagMetadataList.ENUM_TO_TAG_METADATA.values()) {
            assertNotNull(metadata.getAlt());
        }
    }

    @Test
    void tagMetadataListAllFieldsNonEmpty() {
        for (var metadata : TagMetadataList.ENUM_TO_TAG_METADATA.values()) {
            assertFalse(metadata.getShortName().isBlank());
            assertFalse(metadata.getName().isBlank());
            assertFalse(metadata.getApiKey().isBlank());
            assertFalse(metadata.getAlt().isBlank());
        }
    }

    @Test
    void generateEnumToObjectRunsWithoutExceptionForTagMetadata() {
        var generator = new ComplexJSTypesGenerator();
        assertDoesNotThrow(() -> generator.generateEnumToObject(Generator.builder()
                .name("TAG_METADATA_LIST")
                .data(TagMetadataList.ENUM_TO_TAG_METADATA)
                .dataShape(DataShape.ENUM_TO_OBJECT)
                .build()));
    }

    @Test
    void generateEnumToObjectEmptyMapDoesNotThrow() {
        var generator = new ComplexJSTypesGenerator();
        assertDoesNotThrow(() -> generator.generateEnumToObject(Generator.builder()
                .name("TAG_METADATA_LIST")
                .data(new LinkedHashMap<>())
                .dataShape(DataShape.ENUM_TO_OBJECT)
                .build()));
    }

    @Test
    void generateEnumToObjectThrowsOnNonEnumKey() {
        var invalidData = new LinkedHashMap<String, Object>();
        invalidData.put("not-an-enum", Map.of());
        var generator = new ComplexJSTypesGenerator();
        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generateEnumToObject(Generator.builder()
                        .name("TEST")
                        .data(invalidData)
                        .dataShape(DataShape.ENUM_TO_OBJECT)
                        .build()));
    }

    @Test
    void generateEnumToObjectRunsWithoutExceptionForTopicMetadata() {
        var generator = new ComplexJSTypesGenerator();
        assertDoesNotThrow(() -> generator.generateEnumToObject(Generator.builder()
                .name("TOPIC_METADATA_LIST")
                .data(TopicMetadataList.ENUM_TO_TOPIC_METADATA)
                .dataShape(DataShape.ENUM_TO_OBJECT)
                .build()));
    }

    @Test
    void topicMetadataListContainsAllTopics() {
        assertEquals(LeetcodeTopicEnum.values().length, TopicMetadataList.ENUM_TO_TOPIC_METADATA.size());
    }

    @Test
    void topicMetadataListAllHaveNonEmptyName() {
        for (var metadata : TopicMetadataList.ENUM_TO_TOPIC_METADATA.values()) {
            assertNotNull(metadata.getName());
            assertFalse(metadata.getName().isBlank());
        }
    }

    @Test
    void resolveExecutableOnPathReturnsAbsoluteExecutable(@TempDir Path root) throws Exception {
        Path blocked = root.resolve("blocked");
        Path allowed = root.resolve("allowed");
        Files.createDirectories(blocked);
        Files.createDirectories(allowed);
        Path blockedNode = blocked.resolve("node");
        Path allowedNode = allowed.resolve("node");
        Files.writeString(blockedNode, "");
        Files.writeString(allowedNode, "");
        Files.setPosixFilePermissions(blockedNode, Set.of(OWNER_READ, OWNER_WRITE));
        Files.setPosixFilePermissions(allowedNode, Set.of(OWNER_READ, OWNER_EXECUTE));

        String pathEnv = blocked + File.pathSeparator + "relative-bin" + File.pathSeparator + allowed;

        assertEquals(allowedNode.toRealPath(), ComplexJSTypesGenerator.resolveExecutableOnPath(pathEnv, "node"));
    }

    @Test
    void resolveExecutableOnPathRejectsRelativeOrMissingEntries() {
        assertThrows(IOException.class, () -> ComplexJSTypesGenerator.resolveExecutableOnPath(null, "node"));
        assertThrows(IOException.class, () -> ComplexJSTypesGenerator.resolveExecutableOnPath("   ", "node"));
        assertThrows(IOException.class, () -> ComplexJSTypesGenerator.resolveExecutableOnPath("relative-bin", "node"));
    }
}
