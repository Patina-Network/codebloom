package org.patinanetwork.codebloom.utilities.generator.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.alexmond.config.json.schema.service.JsonSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigSchemaGeneratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    private Path tempDir;

    private Path outputPath;
    private JsonSchemaService jsonSchemaService;
    private ConfigSchemaGenerator generator;

    @BeforeEach
    void setUp() {
        outputPath = tempDir.resolve("config-schema.json");
        jsonSchemaService = mock(JsonSchemaService.class);
        generator = new ConfigSchemaGenerator(jsonSchemaService, outputPath);
    }

    private JsonNode generate(final String librarySchema) throws Exception {
        when(jsonSchemaService.generateFullSchemaJson()).thenReturn(librarySchema);
        generator.run();
        return MAPPER.readTree(Files.readString(outputPath));
    }

    @Test
    void stripsStringAnchorsAtEveryDepth() throws Exception {
        JsonNode schema = generate("""
                {
                  "$defs": {
                    "a.B:C": {"type": "object", "$anchor": "a.B:C"}
                  },
                  "properties": {
                    "server": {
                      "type": "object",
                      "$anchor": "x.Y$Z",
                      "properties": {
                        "list": {"type": "array", "items": {"type": "object", "$anchor": "p.Q$R"}},
                        "map": {"type": "object", "additionalProperties": {"type": "object", "$anchor": "m.N"}}
                      }
                    }
                  }
                }
                """);

        assertFalse(schema.at("/$defs/a.B:C").has("$anchor"));
        JsonNode server = schema.at("/properties/server");
        assertFalse(server.has("$anchor"));
        assertFalse(server.at("/properties/list/items").has("$anchor"));
        assertFalse(server.at("/properties/map/additionalProperties").has("$anchor"));
    }

    @Test
    void keepsPropertyNamedAnchor() throws Exception {
        JsonNode schema = generate("""
                {"properties": {"$anchor": {"type": "string"}}}
                """);

        assertEquals("string", schema.at("/properties/$anchor/type").asText());
    }

    @Test
    void widensCoercibleTypesToAlsoAcceptString() throws Exception {
        JsonNode schema = generate("""
                {
                  "properties": {
                    "flag": {"type": "boolean", "default": false},
                    "port": {"type": "integer"},
                    "ratio": {"type": "number"},
                    "scopes": {"type": "array", "items": {"type": "integer"}}
                  }
                }
                """);

        assertEquals(MAPPER.readTree("[\"boolean\", \"string\"]"), schema.at("/properties/flag/type"));
        assertFalse(schema.at("/properties/flag/default").asBoolean());
        assertEquals(MAPPER.readTree("[\"integer\", \"string\"]"), schema.at("/properties/port/type"));
        assertEquals(MAPPER.readTree("[\"number\", \"string\"]"), schema.at("/properties/ratio/type"));
        assertEquals(MAPPER.readTree("[\"array\", \"string\"]"), schema.at("/properties/scopes/type"));
        assertEquals(MAPPER.readTree("[\"integer\", \"string\"]"), schema.at("/properties/scopes/items/type"));
    }

    @Test
    void leavesStringAndObjectTypesAlone() throws Exception {
        JsonNode schema = generate("""
                {
                  "type": "object",
                  "properties": {
                    "host": {"type": "string"},
                    "type": {"type": "object", "properties": {}}
                  }
                }
                """);

        assertEquals("object", schema.at("/type").asText());
        assertEquals("string", schema.at("/properties/host/type").asText());
        assertEquals("object", schema.at("/properties/type/type").asText());
    }

    @Test
    void writesPrettyPrintedJsonEndingInNewline() throws Exception {
        when(jsonSchemaService.generateFullSchemaJson()).thenReturn("{\"type\":\"object\"}");

        generator.run();

        String written = Files.readString(outputPath);
        assertTrue(written.endsWith("}\n"));
        assertTrue(written.contains("\"type\" : \"object\""));
    }

    @Test
    void sortsObjectKeysAndArraysAtEveryDepth() throws Exception {
        when(jsonSchemaService.generateFullSchemaJson()).thenReturn("""
                        {"z":1,"a":{"y":{"d":1,"c":2},"b":3},"m":[{"q":1,"p":2},"x","w",{"p":1,"q":9}],"enum":["b","a"]}
                        """);

        generator.run();

        assertEquals(
                "{\"a\":{\"b\":3,\"y\":{\"c\":2,\"d\":1}},\"enum\":[\"a\",\"b\"],"
                        + "\"m\":[\"w\",\"x\",{\"p\":1,\"q\":9},{\"p\":2,\"q\":1}],\"z\":1}",
                MAPPER.writeValueAsString(MAPPER.readTree(Files.readString(outputPath))));
    }

    @Test
    void generationFailureDoesNotThrowOrWriteFile() {
        when(jsonSchemaService.generateFullSchemaJson()).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> generator.run());
        assertFalse(Files.exists(outputPath));
    }

    @Test
    void invalidJsonFromLibraryLeavesExistingFileUntouched() throws Exception {
        Files.writeString(outputPath, "previous\n");
        when(jsonSchemaService.generateFullSchemaJson()).thenReturn("not json");

        assertDoesNotThrow(() -> generator.run());
        assertEquals("previous\n", Files.readString(outputPath));
    }
}
