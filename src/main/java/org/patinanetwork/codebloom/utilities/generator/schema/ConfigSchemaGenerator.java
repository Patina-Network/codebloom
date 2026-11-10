package org.patinanetwork.codebloom.utilities.generator.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.config.json.schema.service.JsonSchemaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Regenerates config-schema on every dev boot */
@Profile("dev")
@Component
@Slf4j
@RequiredArgsConstructor
public class ConfigSchemaGenerator implements CommandLineRunner {

    private static final Path OUTPUT_PATH = Path.of("src/main/resources/config-schema.json");

    private final JsonSchemaService jsonSchemaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(final String... args) {
        try {
            JsonNode schema = objectMapper.readTree(jsonSchemaService.generateFullSchemaJson());
            relax(schema);
            Files.writeString(
                    OUTPUT_PATH, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema) + "\n");
            log.info("Wrote config schema to {}", OUTPUT_PATH);
        } catch (Exception e) {
            log.error("Config schema generation failed", e);
        }
    }

    private static final Set<String> STRING_COERCIBLE_TYPES = Set.of("boolean", "integer", "number", "array");

    /**
     * Loosens the library output to match what Spring actually accepts:
     *
     * <ul>
     *   <li>Drops {@code $anchor}: values are class names containing {@code $}/{@code :}, which violate the 2020-12
     *       anchor pattern. All refs use {@code #/$defs/...}, so anchors are unused. Only string values are removed; an
     *       object under a {@code $anchor} key is a property definition, not the keyword.
     *   <li>Widens boolean/integer/number/array to also allow string, so {@code ${ENV_VAR}} placeholders and
     *       comma-separated lists validate.
     * </ul>
     */
    private static void relax(final JsonNode node) {
        if (node instanceof ObjectNode object) {
            if (object.path("$anchor").isTextual()) {
                object.remove("$anchor");
            }
            JsonNode type = object.path("type");
            if (type.isTextual() && STRING_COERCIBLE_TYPES.contains(type.asText())) {
                object.putArray("type").add(type.asText()).add("string");
            }
        }
        node.forEach(ConfigSchemaGenerator::relax);
    }
}
