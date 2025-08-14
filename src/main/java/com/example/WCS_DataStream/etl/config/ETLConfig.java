package com.example.WCS_DataStream.etl.config;

import java.time.Duration;

/**
 * ETL 엔진 설정 클래스
 * 
 * ETL 프로세스와 풀링 엔진의 동작을 제어하는 통합 설정 정보를 담습니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public class ETLConfig {
    
    // ETL 프로세스 설정
    /**
     * ETL 실행 간격 (기본값: 1초)
     */
    private Duration executionInterval = Duration.ofSeconds(1);
    
    /**
     * 배치 크기 (기본값: 100)
     */
    private int batchSize = 100;
    
    /**
     * 데이터 검증 활성화 여부
     */
    private boolean validationEnabled = true;
    
    /**
     * 데이터 변환 활성화 여부
     */
    private boolean transformationEnabled = true;
    
    /**
     * 오류 처리 모드 (CONTINUE, STOP, RETRY)
     */
    private ErrorHandlingMode errorHandlingMode = ErrorHandlingMode.CONTINUE;
    
    /**
     * 재시도 횟수
     */
    private int retryCount = 3;
    
    /**
     * 재시도 간격
     */
    private Duration retryInterval = Duration.ofSeconds(5);
    
    // 풀링 엔진 설정
    /**
     * 풀링 간격 (밀리초)
     */
    private long pullIntervalMs = 5000;
    
    /**
     * 최대 풀링 간격 (밀리초)
     */
    private long maxPullIntervalMs = 30000;
    
    /**
     * 연결 재시도 횟수
     */
    private int maxRetryCount = 3;
    
    /**
     * 재시도 간격 (밀리초)
     */
    private long retryIntervalMs = 1000;
    
    /**
     * 타임아웃 (밀리초)
     */
    private long timeoutMs = 30000;
    
    /**
     * 캐시 사용 여부
     */
    private boolean useCache = true;
    
    /**
     * 캐시 만료 시간 (밀리초)
     */
    private long cacheExpirationMs = 300000;
    
    /**
     * 로깅 레벨
     */
    private String logLevel = "INFO";
    
    /**
     * 풀링 전략
     */
    private PullingStrategy strategy = PullingStrategy.HYBRID;
    
    /**
     * 오류 처리 모드 열거형
     */
    public enum ErrorHandlingMode {
        CONTINUE,   // 오류 발생 시 계속 진행
        STOP,       // 오류 발생 시 중단
        RETRY       // 오류 발생 시 재시도
    }
    
    /**
     * 풀링 전략 열거형
     */
    public enum PullingStrategy {
        PULL_ONLY,      // 풀링만 사용
        PUSH_ONLY,      // 푸시만 사용
        HYBRID          // 하이브리드 방식
    }
    
    // 생성자
    public ETLConfig() {}
    
    // ETL 프로세스 관련 Getter/Setter
    public Duration getExecutionInterval() { return executionInterval; }
    public void setExecutionInterval(Duration executionInterval) { this.executionInterval = executionInterval; }
    
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    
    public boolean isValidationEnabled() { return validationEnabled; }
    public void setValidationEnabled(boolean validationEnabled) { this.validationEnabled = validationEnabled; }
    
    public boolean isTransformationEnabled() { return transformationEnabled; }
    public void setTransformationEnabled(boolean transformationEnabled) { this.transformationEnabled = transformationEnabled; }
    
    public ErrorHandlingMode getErrorHandlingMode() { return errorHandlingMode; }
    public void setErrorHandlingMode(ErrorHandlingMode errorHandlingMode) { this.errorHandlingMode = errorHandlingMode; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public Duration getRetryInterval() { return retryInterval; }
    public void setRetryInterval(Duration retryInterval) { this.retryInterval = retryInterval; }
    
    // 풀링 엔진 관련 Getter/Setter
    public long getPullIntervalMs() { return pullIntervalMs; }
    public void setPullIntervalMs(long pullIntervalMs) { this.pullIntervalMs = pullIntervalMs; }
    
    public long getMaxPullIntervalMs() { return maxPullIntervalMs; }
    public void setMaxPullIntervalMs(long maxPullIntervalMs) { this.maxPullIntervalMs = maxPullIntervalMs; }
    
    public int getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(int maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    
    public long getRetryIntervalMs() { return retryIntervalMs; }
    public void setRetryIntervalMs(long retryIntervalMs) { this.retryIntervalMs = retryIntervalMs; }
    
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    
    public boolean isUseCache() { return useCache; }
    public void setUseCache(boolean useCache) { this.useCache = useCache; }
    
    public long getCacheExpirationMs() { return cacheExpirationMs; }
    public void setCacheExpirationMs(long cacheExpirationMs) { this.cacheExpirationMs = cacheExpirationMs; }
    
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
    
    public PullingStrategy getStrategy() { return strategy; }
    public void setStrategy(PullingStrategy strategy) { this.strategy = strategy; }
    
    // Duration을 밀리초로 변환하는 편의 메서드
    public void setPullInterval(Duration duration) { 
        this.pullIntervalMs = duration.toMillis(); 
    }
    
    @Override
    public String toString() {
        return "ETLConfig{" +
                "executionInterval=" + executionInterval +
                ", batchSize=" + batchSize +
                ", validationEnabled=" + validationEnabled +
                ", transformationEnabled=" + transformationEnabled +
                ", errorHandlingMode=" + errorHandlingMode +
                ", retryCount=" + retryCount +
                ", retryInterval=" + retryInterval +
                ", pullIntervalMs=" + pullIntervalMs +
                ", maxPullIntervalMs=" + maxPullIntervalMs +
                ", maxRetryCount=" + maxRetryCount +
                ", retryIntervalMs=" + retryIntervalMs +
                ", timeoutMs=" + timeoutMs +
                ", useCache=" + useCache +
                ", cacheExpirationMs=" + cacheExpirationMs +
                ", logLevel='" + logLevel + '\'' +
                ", strategy=" + strategy +
                '}';
    }
} 