///*
// * Copyright 2025 LY Corporation
// *
// * LY Corporation licenses this file to you under the Apache License,
// * version 2.0 (the "License"); you may not use this file except in compliance
// * with the License. You may obtain a copy of the License at:
// *
// *   https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations
// * under the License.
// */
//
//package com.linecorp.decaton.centraldogma.jsonschema;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.networknt.schema.JsonSchema;
//import com.networknt.schema.JsonSchemaFactory;
//import com.networknt.schema.SpecVersionDetector;
//import com.networknt.schema.ValidationMessage;
//import org.junit.jupiter.api.Test;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.List;
//import java.util.Set;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class GeneratedSchemaValidationTest {
//    private static final ObjectMapper MAPPER = new ObjectMapper();
//    private static final List<String> DRAFTS =
//            List.of("draft_7", "draft_2019_09", "draft_2020_12");
//    private static final List<String> VARIANTS =
//            List.of("", "-allow-additional-properties");
//
//    private static Path distDir() {
//        // centraldogma モジュールの作業ディレクトリで実行される想定
//        return Path.of("jsonschema", "dist");
//    }
//
//    private static JsonSchema loadSchema(Path path) throws Exception {
//        JsonNode schemaNode = MAPPER.readTree(Files.readString(path));
//        var version = SpecVersionDetector.detect(schemaNode);
//        return JsonSchemaFactory.getInstance(version).getSchema(schemaNode);
//    }
//
//    @Test
//    void compilesAndValidatesExampleForAllVariants() throws Exception {
//        Path dir = distDir();
//        JsonNode example = MAPPER.readTree(
//                Files.readString(dir.resolve("decaton-processor-properties-central-dogma-example.json")));
//
//        for (String draft : DRAFTS) {
//            for (String variant : VARIANTS) {
//                String name = "decaton-processor-properties-central-dogma-schema-%s%s.json"
//                        .formatted(draft, variant);
//                Path schemaPath = dir.resolve(name);
//                assertTrue(Files.exists(schemaPath), "Missing schema: " + schemaPath);
//
//                JsonSchema schema = loadSchema(schemaPath);
//                Set<ValidationMessage> errors = schema.validate(example);
//                assertTrue(errors.isEmpty(), name + " errors: " + errors);
//            }
//        }
//    }
//
//    @Test
//    void rejectsWrongType() throws Exception {
//        Path dir = distDir();
//        JsonNode example = MAPPER.readTree(
//                Files.readString(dir.resolve("decaton-processor-properties-central-dogma-example.json")));
//
//        // 既存プロパティの型を壊してエラーになることを確認
//        ObjectNode bad = example.deepCopy();
//        bad.put("decaton.partition.concurrency", "oops");
//
//        for (String draft : DRAFTS) {
//            for (String variant : VARIANTS) {
//                String name = "decaton-processor-properties-central-dogma-schema-%s%s.json"
//                        .formatted(draft, variant);
//                JsonSchema schema = loadSchema(dir.resolve(name));
//                Set<ValidationMessage> errors = schema.validate(bad);
//                assertFalse(errors.isEmpty(), "Should fail: " + name);
//            }
//        }
//    }
//}