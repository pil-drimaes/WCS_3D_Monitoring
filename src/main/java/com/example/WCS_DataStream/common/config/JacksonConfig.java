package com.example.WCS_DataStream.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 설정 클래스
 * 
 * JSON 직렬화/역직렬화를 위한 ObjectMapper 설정
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Configuration
public class JacksonConfig {
    
    /**
     * ObjectMapper Bean 설정
     * 
     * @return 설정된 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Java 8 시간 모듈 등록
        objectMapper.registerModule(new JavaTimeModule());
        
        // 날짜를 ISO-8601 형식으로 직렬화
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 빈 객체도 직렬화
        objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
        
        return objectMapper;
    }
}
