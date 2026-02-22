/*
 * Copyright 2025 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.eva.server.ws;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import uk.ac.ebi.eva.lib.Profiles;

@ActiveProfiles(Profiles.TEST_MONGO_FACTORY)
public abstract class AbstractIntegrationTest {

    static final MongoDBContainer mongoDBContainer =
            new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    static {
        mongoDBContainer.start();
    }

    /**
     * Override spring.data.mongodb.host with the Testcontainers MongoDB host:port so that
     * MongoConfiguration / DBAdaptorConnector connect to the container instead of localhost:27017.
     * The host value is formatted as "host:port" which DBAdaptorConnector parses correctly.
     */
    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.host",
                () -> mongoDBContainer.getHost() + ":" + mongoDBContainer.getMappedPort(27017));
        registry.add("spring.data.mongodb.database", () -> "test-db");
    }
}
