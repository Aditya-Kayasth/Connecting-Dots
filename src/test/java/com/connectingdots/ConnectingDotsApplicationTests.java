package com.connectingdots;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootTest
class ConnectingDotsApplicationTests {

    // Prevent Spring from trying to open a real Redis connection during tests
    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    @Test
    void contextLoads() {
    }
}
