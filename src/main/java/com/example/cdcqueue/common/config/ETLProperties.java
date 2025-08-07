package com.example.cdcqueue.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * ETL 설정 Properties 클래스
 * 
 * application.properties에서 ETL 관련 설정을 읽어오는 클래스
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
@ConfigurationProperties(prefix = "etl")
public class ETLProperties {
    
    /**
     * 풀링 설정
     */
    private Pulling pulling = new Pulling();
    
    /**
     * 검증 설정
     */
    private Validation validation = new Validation();
    
    /**
     * 변환 설정
     */
    private Transformation transformation = new Transformation();
    
    /**
     * 오류 처리 설정
     */
    private ErrorHandling errorHandling = new ErrorHandling();
    
    /**
     * 풀링 설정 클래스
     */
    public static class Pulling {
        private Duration interval = Duration.ofSeconds(1);
        private int batchSize = 100;
        private String strategy = "HYBRID";
        
        // Getter/Setter
        public Duration getInterval() { return interval; }
        public void setInterval(Duration interval) { this.interval = interval; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
    }
    
    /**
     * 검증 설정 클래스
     */
    public static class Validation {
        private boolean enabled = true;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    /**
     * 변환 설정 클래스
     */
    public static class Transformation {
        private boolean enabled = true;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    /**
     * 오류 처리 설정 클래스
     */
    public static class ErrorHandling {
        private String mode = "CONTINUE";
        private int retryCount = 3;
        private Duration retryInterval = Duration.ofSeconds(5);
        
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        
        public Duration getRetryInterval() { return retryInterval; }
        public void setRetryInterval(Duration retryInterval) { this.retryInterval = retryInterval; }
    }
    
    // Getter/Setter
    public Pulling getPulling() { return pulling; }
    public void setPulling(Pulling pulling) { this.pulling = pulling; }
    
    public Validation getValidation() { return validation; }
    public void setValidation(Validation validation) { this.validation = validation; }
    
    public Transformation getTransformation() { return transformation; }
    public void setTransformation(Transformation transformation) { this.transformation = transformation; }
    
    public ErrorHandling getErrorHandling() { return errorHandling; }
    public void setErrorHandling(ErrorHandling errorHandling) { this.errorHandling = errorHandling; }
} 