package com.example.cdcqueue.etl;

import com.example.cdcqueue.etl.engine.PullingEngineConfig;
import com.example.cdcqueue.parser.ParserConfig;
import java.time.Duration;

/**
 * ETL 엔진 설정 클래스
 * 
 * ETL 프로세스의 동작을 제어하는 설정 정보를 담습니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public class ETLConfig {
    
    /**
     * 풀링 엔진 설정
     */
    private PullingEngineConfig pullingConfig;
    
    /**
     * 파서 설정
     */
    private ParserConfig parserConfig;
    
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
    
    /**
     * 오류 처리 모드 열거형
     */
    public enum ErrorHandlingMode {
        CONTINUE,   // 오류 발생 시 계속 진행
        STOP,       // 오류 발생 시 중단
        RETRY       // 오류 발생 시 재시도
    }
    
    // 생성자
    public ETLConfig() {}
    
    public ETLConfig(PullingEngineConfig pullingConfig, ParserConfig parserConfig) {
        this.pullingConfig = pullingConfig;
        this.parserConfig = parserConfig;
    }
    
    // Getter/Setter 메서드들
    public PullingEngineConfig getPullingConfig() { return pullingConfig; }
    public void setPullingConfig(PullingEngineConfig pullingConfig) { this.pullingConfig = pullingConfig; }
    
    public ParserConfig getParserConfig() { return parserConfig; }
    public void setParserConfig(ParserConfig parserConfig) { this.parserConfig = parserConfig; }
    
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
} 