package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.ETLStatistics;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ETL과 풀링을 통합한 추상 엔진 클래스
 * 
 * 데이터 추출, 변환, 적재와 풀링 로직을 하나로 통합하여 관리합니다.
 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
public abstract class ETLEngine<T> {
    
    protected static final Logger log = LoggerFactory.getLogger(ETLEngine.class);
    
    // ETL 설정
    protected ETLConfig config;
    
    // 마지막 실행 시간
    protected final AtomicLong lastExecutionTime = new AtomicLong(0);
    
    // 엔진 상태
    protected final AtomicReference<EngineStatus> status = new AtomicReference<>(EngineStatus.STOPPED);
    
    // ETL 통계
    protected final ETLStatistics statistics = new ETLStatistics();
    
    // 마지막 풀링 시간
    protected final AtomicLong lastPullTime = new AtomicLong(0);
    
    // 마지막 전체 동기화 시간
    protected final AtomicLong lastFullSyncTime = new AtomicLong(0);

    protected volatile boolean tableExists = false;

    // PostgreSQL 데이터 서비스 (공통)
    protected PostgreSQLDataService postgreSQLDataService;

    /**
     * 테이블 존재 여부 확인 (공통 메서드)
     */
    protected abstract boolean checkTableExists();
    
    /**
     * 테이블 존재 여부 조회
     */
    public boolean isTableExists() {
        return tableExists;
    }
    
    /**
     * 엔진 상태 열거형
     */
    public enum EngineStatus {
        RUNNING, STOPPED, ERROR
    }
    
    /**
     * ETL 프로세스 실행
     * 
     * @return 처리된 데이터 리스트
     * @throws ETLEngineException ETL 처리 중 오류 발생 시
     */
    public List<T> executeETL() throws ETLEngineException {
        try {
            
            long startTime = System.currentTimeMillis();
            status.set(EngineStatus.RUNNING);
            
            // 데이터 추출
            List<T> extractedData = extractData();
            int extractedCount = extractedData.size();
            
            // 데이터 변환 및 적재
            List<T> processedData = transformAndLoad(extractedData);
            int processedCount = processedData.size();
            
            // 통계 업데이트
            updateStatistics(extractedCount, processedCount, startTime);
            
            // 마지막 실행 시간 업데이트
            lastExecutionTime.set(System.currentTimeMillis());
            
            if (processedCount > 0 || extractedCount > 0) {
                log.info("ETL process completed: extracted={}, processed={}", extractedCount, processedCount);
            } else {
                log.debug("ETL process completed: extracted=0, processed=0");
            }
            return processedData;
            
        } catch (Exception e) {
            status.set(EngineStatus.ERROR);
            throw new ETLEngineException("Error in ETL process: " + e.getMessage(), e);
        } finally {
            status.set(EngineStatus.STOPPED);
        }
    }
    
    /**
     * 데이터 추출 (추상 메서드)
     */
    protected abstract List<T> extractData() throws ETLEngineException;
    
    /**
     * 데이터 변환 및 적재 (추상 메서드)
     */
    protected abstract List<T> transformAndLoad(List<T> data) throws ETLEngineException;
    
    /**
     * 데이터베이스 연결 상태 확인 (추상 메서드)
     */
    public abstract boolean isConnected();
    
    /**
     * 데이터 키 생성 (추상 메서드)
     */
    protected abstract String getDataKey(T data);
    
    /**
     * 데이터 비교 (추상 메서드)
     */
    protected abstract boolean isSameData(T data1, T data2);

    
    
    /**
     * ETL 엔진 초기화
     */
    public void initialize(ETLConfig config, PostgreSQLDataService postgreSQLDataService) {
        this.config = config;
        this.postgreSQLDataService = postgreSQLDataService;
        
        try {
            // 공통: PostgreSQL 연결 상태 확인
            if (!postgreSQLDataService.isConnected()) {
                log.error("PostgreSQL is not connected. ETL engine initialization failed.");
                status.set(EngineStatus.ERROR);
                return;
            }
            
            // 공통: 테이블 존재 여부 확인
            this.tableExists = checkTableExists();
            if (!this.tableExists) {
                log.error("Required table does not exist. ETL engine initialization failed.");
                status.set(EngineStatus.ERROR);
                return;
            }
            
            status.set(EngineStatus.STOPPED);
            log.info("ETL engine initialized successfully with config: {}", config);
        } catch (Exception e) {
            log.error("Error initializing ETL engine: {}", e.getMessage(), e);
            status.set(EngineStatus.ERROR);
        }
    }
    
    
    /**
     * ETL 엔진 상태 확인
     */
    public boolean isHealthy() {
        return status.get() == EngineStatus.RUNNING && isConnected();
    }
    
    /**
     * 마지막 ETL 실행 시간 조회
     */
    public long getLastExecutionTime() {
        return lastExecutionTime.get();
    }
    
    /**
     * 처리된 데이터 통계 조회
     */
    public ETLStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * 마지막 풀링 시간 조회
     */
    public long getLastPullTime() {
        return lastPullTime.get();
    }
    
    /**
     * 통계 업데이트
     */
    protected void updateStatistics(int extractedCount, int processedCount, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        statistics.setTotalProcessedRecords(statistics.getTotalProcessedRecords() + extractedCount);
        statistics.setSuccessfulRecords(statistics.getSuccessfulRecords() + processedCount);
        statistics.setTotalExecutionTime(statistics.getTotalExecutionTime() + executionTime);
        
        if (statistics.getTotalProcessedRecords() > 0) {
            statistics.setAverageProcessingTime(
                (double) statistics.getTotalExecutionTime() / statistics.getTotalProcessedRecords()
            );
        }
        
        statistics.setLastExecutionTime(LocalDateTime.now());
    }
    
    /**
     * 풀링 시간 업데이트
     */
    protected void updatePullTime() {
        lastPullTime.set(System.currentTimeMillis());
    }
    
    /**
     * 전체 동기화 시간 업데이트
     */
    protected void updateFullSyncTime() {
        lastFullSyncTime.set(System.currentTimeMillis());
    }
} 