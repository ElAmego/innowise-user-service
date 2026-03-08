package com.innowise.userservice.configuration;

import com.innowise.userservice.constant.PackageUrl;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.innowise.userservice.model.dao")
public class HibernateConfiguration {

    @Value("${hibernate.dialect}")
    private String hibernateDialect;

    @Value("${hibernate.show_sql}")
    private String hibernateShowSql;

    @Value("${hibernate.hbm2ddl.auto}")
    private String hibernateHbm2DdlAuto;

    @Value("${hibernate.enable_lazy_load_no_trans}")
    private String hibernateEnableLazyLoadNoTrans;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final DataSource dataSource) {
        final LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource);
        entityManagerFactory.setPackagesToScan(PackageUrl.ENTITY_URL);
        entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactory.setJpaProperties(getHibernateProperties());
        return entityManagerFactory;
    }

    private Properties getHibernateProperties() {
        final Properties properties = new Properties();
        properties.setProperty(AvailableSettings.DIALECT, hibernateDialect);
        properties.setProperty(AvailableSettings.SHOW_SQL, hibernateShowSql);
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, hibernateHbm2DdlAuto);
        properties.setProperty(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, hibernateEnableLazyLoadNoTrans);
        return properties;
    }
}