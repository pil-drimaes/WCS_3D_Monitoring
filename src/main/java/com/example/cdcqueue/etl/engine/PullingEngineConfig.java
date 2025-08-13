package com.example.cdcqueue.etl.engine;

import java.time.Duration;

/**
 * 풀링 엔진 설정 클래스
 * 
 * 데이터 풀링 엔진의 동작을 제어하는 설정 정보를 담습니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public class PullingEngineConfig {
    
    /**
     * 풀링 간격 (기본값: 0.1초)
     */
    private Duration pullInterval = Duration.ofMillis(100);
    
    /**
     * 배치 크기 (기본값: 100)
     */
    private int batchSize = 100;
    
    /**
     * 타임아웃 (기본값: 30초)
     */
    private Duration timeout = Duration.ofSeconds(30);
    
    /**
     * 재시도 횟수 (기본값: 3)
     */
    private int retryCount = 3;
    
    /**
     * 재시도 간격 (기본값: 5초)
     */
    private Duration retryInterval = Duration.ofSeconds(5);
    
    /**
     * 데이터베이스 연결 설정
     */
    private DatabaseConnectionConfig databaseConfig;
    
    /**
     * 풀링 전략 (FULL, CONDITIONAL, HYBRID)
     */
    private PullingStrategy strategy = PullingStrategy.HYBRID;
    
    /**
     * 풀링 전략 열거형
     */
    public enum PullingStrategy {
        FULL,           // 전체 데이터 비교
        CONDITIONAL,    // 조건부 쿼리
        HYBRID          // 하이브리드
    }
    
    // 생성자
    public PullingEngineConfig() {}
    
    public PullingEngineConfig(Duration pullInterval, int batchSize, DatabaseConnectionConfig databaseConfig) {
        this.pullInterval = pullInterval;
        this.batchSize = batchSize;
        this.databaseConfig = databaseConfig;
    }
    
    // Getter/Setter 메서드들
    public Duration getPullInterval() { return pullInterval; }
    public void setPullInterval(Duration pullInterval) { this.pullInterval = pullInterval; }
    
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public Duration getRetryInterval() { return retryInterval; }
    public void setRetryInterval(Duration retryInterval) { this.retryInterval = retryInterval; }
    
    public DatabaseConnectionConfig getDatabaseConfig() { return databaseConfig; }
    public void setDatabaseConfig(DatabaseConnectionConfig databaseConfig) { this.databaseConfig = databaseConfig; }
    
    public PullingStrategy getStrategy() { return strategy; }
    public void setStrategy(PullingStrategy strategy) { this.strategy = strategy; }
} 