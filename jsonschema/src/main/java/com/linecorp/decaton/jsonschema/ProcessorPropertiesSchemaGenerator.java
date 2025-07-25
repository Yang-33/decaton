/*
 * Copyright 2025 LY Corporation
 *
 * LY Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.decaton.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.*;
import com.linecorp.decaton.processor.runtime.ProcessorProperties;
import com.linecorp.decaton.processor.runtime.PropertyDefinition;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Generates JSON schema files for Decaton ProcessorProperties.
 * The generated schemas are compatible with some JSON Schema
 */
@Slf4j
public final class ProcessorPropertiesSchemaGenerator {

    private static final List<SchemaVersion> TARGET_VERSIONS = List.of(
            SchemaVersion.DRAFT_6,
            SchemaVersion.DRAFT_7,
            SchemaVersion.DRAFT_2019_09,
            SchemaVersion.DRAFT_2020_12
    );
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<PropertyDefinition<?>, Type> TYPE_CACHE = buildTypeCache();

    private static Map<PropertyDefinition<?>, Type> buildTypeCache() {
        Map<PropertyDefinition<?>, Type> map = new HashMap<>();
        for (Field field : ProcessorProperties.class.getDeclaredFields()) {
            if (!PropertyDefinition.class.isAssignableFrom(field.getType())) continue;
            try {
                PropertyDefinition<?> def = (PropertyDefinition<?>) field.get(null);

                Type valueType = switch (field.getGenericType()) {
                    case ParameterizedType pt -> pt.getActualTypeArguments()[0];  // List<String> etc
                    default                 -> def.runtimeType();                // Long.class etc
                };
                map.put(def, valueType);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to access field " + field.getName(), e);
            }
        }
        return map;
    }

    /**
     * Main method to generate JSON schema files for Decaton ProcessorProperties.
     * @param args
     * args[0] should be the output directory where the schema files will be written.
     * @throws IOException if an I/O error occurs while writing the schema files.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: <outDir>");
            System.exit(1);
        }
        Path outDir = Paths.get(args[0]);
        Files.createDirectories(outDir);

        for (SchemaVersion draft : TARGET_VERSIONS) {
            generateForDraft(outDir, draft, false);
            generateForDraft(outDir, draft, true);
        }
    }

    private static void generateForDraft(Path dir, SchemaVersion draft, boolean allowAdditional) throws IOException {
        String fileName = String.format(
                "decaton-processor-properties-schema-%s%s.json",
                draft.name().toLowerCase(),
                allowAdditional ? "-allow-additional-properties" : ""
        );
        Path file = dir.resolve(fileName);

        JsonNode schema = buildSchema(draft, allowAdditional);
        Files.writeString(file, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema));
        log.info("wrote {}", file);
    }

    private static JsonNode buildSchema(SchemaVersion draft, boolean allowAdditional) {
        SchemaGenerator generator = new SchemaGenerator(
                new SchemaGeneratorConfigBuilder(MAPPER, draft, OptionPreset.PLAIN_JSON)
                        .without(Option.SCHEMA_VERSION_INDICATOR)
                        .build());

        var root = MAPPER.createObjectNode();
        root.put("$schema", draft.getIdentifier());
        root.put("title", "Decaton ProcessorProperties");
        root.put("type", "object");
        root.put("additionalProperties", allowAdditional);
        var required = root.putArray("required");

        var props = root.putObject("properties");
        // Allow instance to write $schema property
        props.putObject("$schema").put("type", "string");

        for (PropertyDefinition<?> def : ProcessorProperties.PROPERTY_DEFINITIONS) {
            Type valueType = TYPE_CACHE.get(def);
            var node = generator.generateSchema(valueType);
            if (def.defaultValue() != null) {
                node.set("default", MAPPER.valueToTree(def.defaultValue()));
            } else {
                required.add(def.name());
            }
            props.set(def.name(), node);
        }
        return root;
    }
    private ProcessorPropertiesSchemaGenerator() {}
}
