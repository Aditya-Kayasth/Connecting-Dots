// Feature: force-for-good-matchmaker-setup, Example 4: Spring application context loads successfully
package com.connectingdots;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "groq.api.key=test-key",
        "NEON_DB_URL=jdbc:postgresql://localhost:5432/test",
        "NEON_DB_USERNAME=test",
        "NEON_DB_PASSWORD=test",
        "UPSTASH_REDIS_HOST=localhost",
        "UPSTASH_REDIS_PORT=6379",
        "UPSTASH_REDIS_PASSWORD=test",
        "GROQ_API_KEY=test-key"
    }
)
class ConnectingDotsApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts without errors
    }
}
