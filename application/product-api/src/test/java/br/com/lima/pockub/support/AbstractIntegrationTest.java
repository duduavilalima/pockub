package br.com.lima.pockub.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração: PostgreSQL 17 e MongoDB 7 reais, iniciados uma única vez
 * por execução da suíte (singleton) e compartilhados entre todas as classes.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    static {
        POSTGRES.start();
        MONGO.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
    }
}
