Java 8 → Java 21 / Spring Boot 2.1 → 3.4 Migration Summary

Overview

The eva-ws project (6 Maven modules) was migrated from Java 8 + Spring Boot 2.1 to Java 21 + Spring Boot 3.4. All 208 tests now pass.

  ---
POM Changes

Root pom.xml
- Java compile target: 1.8 → 21 (<compileSource>)
- Spring Boot parent: 2.1.0.RELEASE → 3.4.3
- Removed springfox-swagger2 / springfox-swagger-ui from dependency management; added springdoc-openapi-starter-webmvc-ui:2.7.0
- Removed spring-security-oauth2:2.1.0.RELEASE (incompatible with Spring Security 6)
- Replaced hypersistence-utils-hibernate-52 with hypersistence-utils-hibernate-63
- Updated postgresql driver: 9.4.1212 → 42.7.4
- Updated maven-resources-plugin: 2.6 → 3.3.1
- Added <release>21</release> to the compiler plugin 
- variation-commons: Version 0.8.x → 1.0.0 (requires publishing)

Per-module POMs: removed SpringFox dependencies; added springdoc-openapi-starter-webmvc-ui; removed spring-hateoas:0.25.1.RELEASE pin in eva-server; removed
explicit Testcontainers versions in count-stats (now managed by Spring Boot BOM)

  ---
javax → jakarta Namespace Migration

All source files with javax.* imports were updated to jakarta.*:
- javax.persistence.* → jakarta.persistence.* — all JPA entity classes across eva-lib, dbsnp-import, eva-release, count-stats
- javax.servlet.* → jakarta.servlet.* — CORSResponseFilter classes, servlet listeners, request handlers
- javax.validation.* → jakarta.validation.* — GA4GH Beacon v2 generated files in eva-server
- javax.xml.bind.* → jakarta.xml.bind.* — GA4GH generated files
- javax.annotation.* → jakarta.annotation.* — GA4GH generated files

  ---
Swagger: SpringFox → Springdoc

Configuration classes replaced across eva-server, dgva-server, eva-release:
- Deleted Docket-based @EnableSwagger2 config
- Replaced with a @Bean OpenAPI method (Springdoc style)

Annotations replaced in all *WSServer.java files and controllers:
- @Api(tags = {...}) → @Tag(name = "...")
- @ApiParam(value = "...") → @Parameter(description = "...")
- import io.swagger.annotations.* → import io.swagger.v3.oas.annotations.*

Properties removed from application.properties:
- springfox.documentation.swagger.v2.path=...

  ---
Spring Security Migration

eva-server (Oauth2Configuration, UnsecureConfiguration):
- Removed ResourceServerConfigurerAdapter + @EnableResourceServer (deleted in Spring Security 6)
- Replaced with SecurityFilterChain beans using the lambda DSL
- Replaced spring-security-oauth2 with spring-boot-starter-oauth2-resource-server
- Changed JWT validation from the old TokenStore approach to .oauth2ResourceServer(oauth2 -> oauth2.jwt(...))

count-stats (SecurityConfiguration):
- Removed WebSecurityConfigurerAdapter (deleted in Spring Security 6)
- Replaced with a SecurityFilterChain bean + UserDetailsService bean
- InMemoryUserDetailsManager with {noop} password encoder for Basic Auth

  ---
Spring HATEOAS API Migration (eva-server)

Five *WSServerV2.java files updated:
- Resource<T> → EntityModel<T>
- Resources<T> → CollectionModel<T>
- new Resource<>(content, links) → EntityModel.of(content, links)
- new Resources<>(content, links) → CollectionModel.of(content, links)

  ---
JUnit 4 → JUnit 5

All test files across eva-lib, eva-server, dgva-server, dbsnp-import updated:
- @RunWith(SpringRunner.class) → @ExtendWith(SpringExtension.class)
- import org.junit.Test → import org.junit.jupiter.api.Test
- @Before / @After → @BeforeEach / @AfterEach
- import org.junit.Assert.* → import org.junit.jupiter.api.Assertions.*
- import org.mockito.Matchers.* → import org.mockito.ArgumentMatchers.*

  ---
Integration Test Infrastructure

eva-server: Created AbstractIntegrationTest base class providing a Testcontainers MongoDB instance via @DynamicPropertySource, replacing embedded Flapdoodle
MongoDB (removed in Spring Boot 3). All integration tests extended this base.

MongoConfiguration circular dependency fix: Removed @Autowired private MongoDatabaseFactory mongoDatabaseFactory field from MongoConfiguration; changed
mappingMongoConverter() to accept it as a method parameter instead, breaking a self-referencing circular dependency active under the test-mongo-factory profile.

count-stats: Added src/test/resources/application.properties using the Testcontainers TC JDBC URL scheme (jdbc:tc:postgresql:13:///testdb) so a PostgreSQL
container is started automatically on first connection — no @DynamicPropertySource or @Container boilerplate needed. Also fixed Count.identifier column definition
to include columnDefinition = "jsonb" for Hibernate 6 DDL generation.

dbsnp-import: Added spring.jpa.defer-datasource-initialization=true to test properties so data.sql runs after JPA creates the schema (Spring Boot 3 changed this
ordering).

  ---
Other Spring Boot 3 Compatibility Fixes

- Removed spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true from production properties (not needed in Hibernate 6)
- Removed management.metrics.binders.jvm.enabled=false (property removed in Micrometer 2)
- Fixed trailing-slash URL match: Spring Boot 3 no longer matches trailing slashes by default; removed trailing / from one test URL in
  VariantWSServerIntegrationTest
- Replaced all spring.main.allow-bean-definition-overriding usages where needed (kept where legitimate bean override was intentional)
