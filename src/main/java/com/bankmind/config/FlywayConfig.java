package com.bankmind.config;

import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> configuration.callbacks(new BaseCallback() {
            @Override
            public boolean supports(Event event, Context context) {
                return event == Event.AFTER_MIGRATE;
            }

            @Override
            public void handle(Event event, Context context) {
                // Post-migration callback logic can go here
            }
        });
    }
}
