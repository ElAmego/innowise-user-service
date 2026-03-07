package com.innowise.userservice.configuration;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfiguration {
    private static final String CHANGE_LOG_URL = "classpath:db/changelog/db.changelog-master.yaml";

    @Bean
    public SpringLiquibase liquibase(final DataSource dataSource) {
        final SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setChangeLog(CHANGE_LOG_URL);
        return springLiquibase;
    }
}