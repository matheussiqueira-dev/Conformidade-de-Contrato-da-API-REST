package com.swee.ordermanagementspring.config;

import com.swee.ordermanagementspring.entities.StoreTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Clock da loja (America/Fortaleza, B-10); injetavel para testes de bloqueio com hora controlada. */
@Configuration
public class TimeConfig {
    @Bean
    public Clock clock() {
        return StoreTime.clock();
    }
}
