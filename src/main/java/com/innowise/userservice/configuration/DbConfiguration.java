package com.innowise.userservice.configuration;

import com.innowise.userservice.constant.EnvironmentValueName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DbConfiguration {
    @Autowired
    private Environment environment;

    @Bean
    public DataSource dataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(environment.getRequiredProperty(EnvironmentValueName.DB_DRIVER));
        dataSource.setUrl(environment.getRequiredProperty(EnvironmentValueName.DB_URL));
        dataSource.setUsername(environment.getRequiredProperty(EnvironmentValueName.DB_USERNAME));
        dataSource.setPassword(environment.getRequiredProperty(EnvironmentValueName.DB_PASSWORD));
        return dataSource;
    }
}