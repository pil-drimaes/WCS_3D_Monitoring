package com.example.WCS_DataStream.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PostgreSQL 데이터베이스 설정 클래스
 * 
 * 실제 운영 DB로 사용할 PostgreSQL 연결 설정
 * JDBC 기반으로 설정 (JPA 제거)
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Configuration
public class PostgreSQLConfig {
    
    @Value("${spring.datasource.postgresql.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.postgresql.username}")
    private String username;
    
    @Value("${spring.datasource.postgresql.password}")
    private String password;
    
    @Value("${spring.datasource.postgresql.driver-class-name}")
    private String driverClassName;
    
    /**
     * PostgreSQL 데이터소스
     */
    @Bean(name = "postgresqlDataSource")
    @Primary
    public DataSource postgresqlDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("PostgreSQLHikariCP");
        
        return new HikariDataSource(config);
    }
    
    
    /**
     * PostgreSQL용 JdbcTemplate
     */
    @Bean(name = "postgresqlJdbcTemplate")
    public JdbcTemplate postgresqlJdbcTemplate(@Qualifier("postgresqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    /**
     * PostgreSQL용 TransactionManager
     */
    @Bean(name = "postgresqlTransactionManager")
    public org.springframework.transaction.PlatformTransactionManager postgresqlTransactionManager(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        return new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
    }
    
    /*
     * JPA 설정 (임시 비활성화)
     * 
     * 나중에 JPA가 필요하면 아래 코드를 활성화
     * 
    @Bean(name = "postgresqlEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean postgresqlEntityManagerFactory(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.example.WCS_DataStream.etl.model");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(true);
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        em.setJpaProperties(properties);
        
        return em;
    }
    
    @Bean(name = "postgresqlTransactionManager")
    public PlatformTransactionManager postgresqlTransactionManager(
            @Qualifier("postgresqlEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
    */
} 