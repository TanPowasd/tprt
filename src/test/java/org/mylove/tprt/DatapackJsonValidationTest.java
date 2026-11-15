package org.mylove.tprt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DatapackJsonValidationTest {

    @Test
    @DisplayName("Verify all datapack and asset JSON files are syntactically valid JSON")
    void testAllJsonFilesAreValid() throws Exception {
        Path resourcesRoot = Paths.get("src", "main", "resources");
        assertTrue(Files.exists(resourcesRoot), "src/main/resources directory must exist");

        List<Path> jsonFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(resourcesRoot)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().endsWith(".json"))
                  .forEach(jsonFiles::add);
        }

        assertFalse(jsonFiles.isEmpty(), "Must find JSON files in resources");
        assertTrue(jsonFiles.size() >= 1000, "Should find at least 1000 JSON files, found: " + jsonFiles.size());

        List<String> parseErrors = new ArrayList<>();
        for (Path file : jsonFiles) {
            try (var reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
                JsonElement elem = JsonParser.parseReader(reader);
                assertNotNull(elem, "Parsed JSON element should not be null for " + file);
            } catch (Exception e) {
                parseErrors.add(file.toString() + " -> " + e.getMessage());
            }
        }

        assertTrue(parseErrors.isEmpty(), "Found JSON syntax errors:\n" + String.join("\n", parseErrors));
    }

    @Test
    @DisplayName("Verify conditions array format in critical modifiers")
    void testSpecificConditionsStructure() throws Exception {
        Path pyriumPath = Paths.get("src", "main", "resources", "data", "tprt", "tinkering", "modifiers", "pyrium.json");
        assertTrue(Files.exists(pyriumPath), "pyrium.json must exist");
        try (var reader = new InputStreamReader(Files.newInputStream(pyriumPath), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("conditions")) {
                assertTrue(obj.get("conditions").isJsonArray(), "conditions in pyrium.json must be a JSON array");
            }
        }

        Path aquaAttrPath = Paths.get("src", "main", "resources", "data", "tprt", "tinkering", "modifiers", "aqua_attr.json");
        assertTrue(Files.exists(aquaAttrPath), "aqua_attr.json must exist");
        try (var reader = new InputStreamReader(Files.newInputStream(aquaAttrPath), StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("conditions")) {
                assertTrue(obj.get("conditions").isJsonArray(), "conditions in aqua_attr.json must be a JSON array");
            }
        }
    }
}
