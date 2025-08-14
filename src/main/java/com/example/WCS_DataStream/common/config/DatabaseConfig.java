package com.example.WCS_DataStream.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 데이터베이스 설정 클래스
 * 
 * WCS DB와 로컬 DB를 분리하여 설정
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Configuration
public class DatabaseConfig {
    
    /**
     * 단일 데이터소스 (현재는 cdc_test를 WCS DB로 사용)
     */
    @Bean(name = "wcsDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource wcsDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 로컬 DB 데이터소스 (이벤트 저장용)
     */
    @Bean(name = "localDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource localDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * WCS DB용 JdbcTemplate (현재는 단일 DB 사용)
     */
    @Bean(name = "wcsJdbcTemplate")
    public JdbcTemplate wcsJdbcTemplate(@Qualifier("wcsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    /**
     * 로컬 DB용 JdbcTemplate
     */
    @Bean(name = "localJdbcTemplate")
    @Primary
    public JdbcTemplate localJdbcTemplate(@Qualifier("localDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
} 