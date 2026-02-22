/*
 * Copyright 2024 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.eva.server.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestDataLoader {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public TestDataLoader(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void load(String... jsonPaths) throws IOException {
        for (String path : jsonPaths) {
            load(path);
        }
    }

    public void load(String jsonPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(jsonPath.startsWith("/") ? jsonPath.substring(1) : jsonPath);
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, List<Map<String, Object>>> data = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, List<Map<String, Object>>>>() {}
            );
            for (Map.Entry<String, List<Map<String, Object>>> entry : data.entrySet()) {
                String collectionName = entry.getKey();
                for (Map<String, Object> doc : entry.getValue()) {
                    mongoTemplate.insert(new Document(convertExtendedJson(doc)), collectionName);
                }
            }
        }
    }

    public void cleanUp(String... collections) {
        for (String collection : collections) {
            mongoTemplate.dropCollection(collection);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertExtendedJson(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), convertValue(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object convertValue(Object value) {
        if (value instanceof Map) {
            Map<String, Object> mapValue = (Map<String, Object>) value;
            if (mapValue.size() == 2 && mapValue.containsKey("$binary") && mapValue.containsKey("$type")) {
                byte[] data = Base64.getDecoder().decode((String) mapValue.get("$binary"));
                byte subType = (byte) Integer.parseInt((String) mapValue.get("$type"), 16);
                return new Binary(subType, data);
            }
            if (mapValue.size() == 1) {
                if (mapValue.containsKey("$oid")) {
                    return new ObjectId((String) mapValue.get("$oid"));
                }
                if (mapValue.containsKey("$date")) {
                    Object dateValue = mapValue.get("$date");
                    if (dateValue instanceof Map) {
                        Map<String, Object> dateMap = (Map<String, Object>) dateValue;
                        if (dateMap.containsKey("$numberLong")) {
                            return new Date(Long.parseLong((String) dateMap.get("$numberLong")));
                        }
                    } else if (dateValue instanceof Long) {
                        return new Date((Long) dateValue);
                    } else if (dateValue instanceof String) {
                        try {
                            return Date.from(Instant.parse((String) dateValue));
                        } catch (DateTimeParseException e) {
                            return new Date(Long.parseLong((String) dateValue));
                        }
                    }
                }
                if (mapValue.containsKey("$numberLong")) {
                    return Long.parseLong((String) mapValue.get("$numberLong"));
                }
                if (mapValue.containsKey("$numberInt")) {
                    return Integer.parseInt((String) mapValue.get("$numberInt"));
                }
                if (mapValue.containsKey("$numberDouble")) {
                    return Double.parseDouble((String) mapValue.get("$numberDouble"));
                }
            }
            return convertExtendedJson(mapValue);
        } else if (value instanceof List) {
            List<Object> converted = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                converted.add(convertValue(item));
            }
            return converted;
        }
        return value;
    }
}
