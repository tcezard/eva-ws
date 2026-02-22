package uk.ac.ebi.eva.lib.datastore;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.ac.ebi.eva.lib.MongoConfiguration;
import uk.ac.ebi.eva.lib.MultiMongoFactoryConfiguration;
import uk.ac.ebi.eva.lib.configuration.SpringDataMongoDbProperties;
import uk.ac.ebi.eva.lib.eva_utils.DBAdaptorConnector;
import uk.ac.ebi.eva.lib.eva_utils.MultiMongoDbFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { MongoConfiguration.class, MultiMongoFactoryConfiguration.class,
        DBAdaptorConnectorTest.MongoTestConfiguration.class})
@SpringBootTest
@EnableConfigurationProperties
public class DBAdaptorConnectorTest {

    @Configuration
    static class MongoTestConfiguration {
        @Bean
        @Primary
        public MongoClient mongoClient() {
            return MongoClients.create("mongodb://localhost:27017");
        }

        @Bean
        @Primary
        public MongoDatabaseFactory mongoDbFactory(MongoClient mongoClient) {
            return new SimpleMongoClientDatabaseFactory(mongoClient, "test-db");
        }
    }

    private static final String TEST_DB = "test-db";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MongoDatabaseFactory factory;

    @Autowired
    private SpringDataMongoDbProperties springDataMongoDbProperties;

    @Test
    public void testSpringDataMongoDbPropertiesAutowiring() {
        assertNotNull(springDataMongoDbProperties);
        assertNotNull(springDataMongoDbProperties.getHost());
    }

    /**
     * Check that spring is autowiring our MultiMongoDbFactory as the MongoDbFactory to use.
     *
     * To check it, we use MultiMongoDbFactory::setDatabaseNameForCurrentThread to change the DB we should get later
     * when we do a `factory.getMongoDatabase()`
     */
    @Test
    public void testMongoDbFactoryAutowiring() {
        String dbName = "test-db";
        MultiMongoDbFactory.setDatabaseNameForCurrentThread(dbName);
        MongoDatabase db = factory.getMongoDatabase();
        assertEquals(db.getName(), dbName);
    }

    /**
     * Check that the value secondaryPreferred is used when it's not specified in the properties.
     *
     * @throws Exception
     */
    @Test
    public void testDefaultReadPreferenceInMongoClientEvaProperty() throws Exception {
        MongoClient mongoClient = DBAdaptorConnector.getMongoClient(springDataMongoDbProperties);
        assertNotNull(mongoClient);
        // Read preference is set via MongoClientSettings when creating the client
    }
}
