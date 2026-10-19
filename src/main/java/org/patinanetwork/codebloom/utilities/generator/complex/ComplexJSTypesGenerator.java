package org.patinanetwork.codebloom.utilities.generator.complex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.shared.tag.ParentTags;
import org.patinanetwork.codebloom.shared.tag.TagMetadataList;
import org.patinanetwork.codebloom.shared.topic.TopicMetadataList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Profile("dev | ci")
@Component
@Slf4j
public class ComplexJSTypesGenerator implements CommandLineRunner {

    @Autowired
    private ApplicationContext applicationContext;

    private final Set<String> imports = new HashSet<>();
    private final StringBuilder tsContent = new StringBuilder();

    @Override
    public void run(final String... args) throws Exception {
        log.info("Type generation command starting...");
        try {
            generateAll();
            writeToFile();
            log.info("Type generation command completed successfully.");
        } catch (Exception e) {
            log.error("Type generation command failed", e);
            int springExitCode = SpringApplication.exit(applicationContext, () -> -1);
            System.exit(springExitCode);
        }
    }

    private final JsonMapper objectMapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    private void generateAll() throws Exception {
        generateTypes();
        generate(Generator.builder()
                .name("PARENT_TAGS_TO_CHILD_TAGS")
                .data(ParentTags.ENUM_TO_ENUM_LIST)
                .dataShape(DataShape.ENUM_TO_ENUM_LIST)
                .build());
        generate(Generator.builder()
                .name("TAG_METADATA_LIST")
                .data(TagMetadataList.ENUM_TO_TAG_METADATA)
                .dataShape(DataShape.ENUM_TO_OBJECT)
                .build());
        generate(Generator.builder()
                .name("TOPIC_METADATA_LIST")
                .data(TopicMetadataList.ENUM_TO_TOPIC_METADATA)
                .dataShape(DataShape.ENUM_TO_OBJECT)
                .build());
    }

    private void generateTypes() {
        tsContent.append(TagMetadataList.TagMetadataObject.TS_TYPE);
        log.info("Generated type: TagMetadataObject");
        tsContent.append(TopicMetadataList.TopicMetadataObject.TS_TYPE);
        log.info("Generated type: TopicMetadataObject");
    }

    private void generate(Generator generator) throws Exception {
        if (generator.getDataShape() == DataShape.ENUM_TO_ENUM_LIST) {
            generateEnumToListOfEnums(generator);
        } else if (generator.getDataShape() == DataShape.ENUM_TO_OBJECT) {
            generateEnumToObject(generator);
        }
    }

    private void generateEnumToListOfEnums(Generator generator) {
        Map<?, List<?>> data = (Map<?, List<?>>) generator.getData();

        if (data.isEmpty()) {
            log.warn("Empty map provided for enum generation");
            return;
        }

        Object firstKey = data.keySet().iterator().next();
        if (!(firstKey instanceof Enum)) {
            throw new IllegalArgumentException("Expected Enum key, got: " + firstKey.getClass());
        }

        Enum<?> enumKey = (Enum<?>) firstKey;
        String enumClassName = enumKey.getDeclaringClass().getSimpleName();

        imports.add(enumClassName);

        tsContent
                .append("export const ")
                .append(generator.getName())
                .append(": Record<")
                .append(enumClassName)
                .append(", ")
                .append(enumClassName)
                .append("[]> = {\n");

        for (Map.Entry<?, List<?>> entry : data.entrySet()) {
            Enum<?> key = (Enum<?>) entry.getKey();
            List<?> values = entry.getValue();

            tsContent
                    .append("  [")
                    .append(enumClassName)
                    .append(".")
                    .append(key.name())
                    .append("]: [");

            String children = values.stream()
                    .map(v -> {
                        if (v instanceof Enum) {
                            return enumClassName + "." + ((Enum<?>) v).name();
                        }
                        throw new IllegalArgumentException("Expected Enum value in list");
                    })
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            tsContent.append(children);
            tsContent.append("],\n");
        }

        tsContent.append("} as const;\n\n");

        log.info("Generated constant: {}", generator.getName());
    }

    @VisibleForTesting
    void generateEnumToObject(Generator generator) throws Exception {
        Map<?, ?> data = (Map<?, ?>) generator.getData();

        if (data.isEmpty()) {
            log.warn("Empty map provided for object generation");
            return;
        }

        Object firstKey = data.keySet().iterator().next();
        if (!(firstKey instanceof Enum)) {
            throw new IllegalArgumentException("Expected Enum key, got: " + firstKey.getClass());
        }

        Enum<?> enumKey = (Enum<?>) firstKey;
        String enumClassName = enumKey.getDeclaringClass().getSimpleName();
        imports.add(enumClassName);

        Object firstValue = data.values().iterator().next();
        String typeName = firstValue.getClass().getSimpleName();

        tsContent
                .append("export const ")
                .append(generator.getName())
                .append(": Record<")
                .append(enumClassName)
                .append(", ")
                .append(typeName)
                .append("> = {\n");

        for (Map.Entry<?, ?> entry : data.entrySet()) {
            Enum<?> key = (Enum<?>) entry.getKey();
            String jsonValue = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entry.getValue());
            tsContent
                    .append("  ")
                    .append(key.name())
                    .append(": ")
                    .append(jsonValue)
                    .append(",\n");
        }

        tsContent.append("};\n\n");

        log.info("Generated constant: {}", generator.getName());
    }

    private void writeToFile() throws Exception {
        StringBuilder finalOutput = new StringBuilder();

        finalOutput.append("""
/**
 * This file was generated by the Codebloom backend.
 * DO NOT EDIT THIS FILE MANUALLY!!!
 */
            """);

        if (!imports.isEmpty()) {
            finalOutput.append("import { ");
            finalOutput.append(String.join(", ", imports));
            finalOutput.append(" } from \"@/lib/api/types/schema\";\n\n");
        }

        finalOutput.append(tsContent);

        Path outputPath = Paths.get("js/src/lib/api/types/complex.ts");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, finalOutput.toString());

        Path nodeExecutable = resolveNodeExecutable();
        Path prettierBin =
                Paths.get("js/node_modules/prettier/bin/prettier.cjs").toAbsolutePath();
        new ProcessBuilder(
                        nodeExecutable.toString(),
                        prettierBin.toString(),
                        "--write",
                        outputPath.toAbsolutePath().toString())
                .inheritIO()
                .start()
                .waitFor();

        log.info("Generated TypeScript file: {} with {} import(s)", outputPath, imports.size());
    }

    private static Path resolveNodeExecutable() throws IOException {
        boolean windows =
                System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        String[] executableNames = windows ? new String[] {"node.exe", "node"} : new String[] {"node"};
        return resolveExecutableOnPath(System.getenv("PATH"), executableNames);
    }

    @VisibleForTesting
    static Path resolveExecutableOnPath(String pathEnv, String... executableNames) throws IOException {
        if (pathEnv == null || pathEnv.isBlank()) {
            throw new IOException("PATH is unset, so node cannot be located");
        }
        for (String directory : pathEnv.split(Pattern.quote(File.pathSeparator))) {
            String trimmed = directory.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Path directoryPath = Paths.get(trimmed);
            if (!directoryPath.isAbsolute()) {
                continue;
            }
            for (String executableName : executableNames) {
                Path candidate = directoryPath.resolve(executableName);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    return candidate.toRealPath();
                }
            }
        }
        throw new IOException("node was not found in an absolute directory on PATH");
    }
}
