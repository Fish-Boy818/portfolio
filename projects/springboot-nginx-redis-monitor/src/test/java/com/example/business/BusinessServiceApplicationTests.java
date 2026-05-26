package com.example.business;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.cache.type=simple",
    "spring.data.redis.host=localhost"
})
class BusinessServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
