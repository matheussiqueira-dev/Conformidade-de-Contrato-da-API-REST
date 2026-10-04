package com.swee.ordermanagementspring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Tag("integration")
@ActiveProfiles("integration")
class OrderManagementSpringApplicationTests {

    @Test
    void contextLoads() {
    }

}
