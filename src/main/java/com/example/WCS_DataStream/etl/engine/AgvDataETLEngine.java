package com.example.WCS_DataStream.etl.engine;

import com.example.WCS_DataStream.etl.ETLEngineException;
import com.example.WCS_DataStream.etl.ETLStatistics;
import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.model.AgvData;
import com.example.WCS_DataStream.etl.service.AgvDataService;

import com.example.WCS_DataStream.etl.service.KafkaProducerService;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// WebSocket 관련 import 제거
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AGV 데이터 ETL 엔진
 * 
 * AGV 데이터의 Extract(추출), Transform(변환), Load(적재) 과정을 관리하는 엔진
 * 
 * 동작 방식:
 * 1. Extract: 풀링 엔진을 통해 새로운 데이터 추출
 * 2. Transform: 파서를 통해 데이터 변환 및 검증
 * 3. Load: 처리된 데이터를 Kafka와 PostgreSQL에 적재
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class AgvDataETLEngine extends ETLEngine<AgvData> {
    
    private static final Logger log = LoggerFactory.getLogger(AgvDataETLEngine.class);
    
    /**
     * AGV 데이터 서비스
     */
    private final AgvDataService agvDataService;
    
    /**
     * Kafka Producer 서비스
     */
    private final KafkaProducerService kafkaProducerService;
    
    /**
     * PostgreSQL 데이터 서비스
     */
    private final PostgreSQLDataService postgreSQLDataService;
    
    /**
     * 생성자
     * 
     * @param agvDataService AGV 데이터 서비스
     * @param kafkaProducerService Kafka Producer 서비스
     * @param postgreSQLDataService PostgreSQL 데이터 서비스
     */
    @Autowired
    public AgvDataETLEngine(AgvDataService agvDataService,
                           KafkaProducerService kafkaProducerService,
                           PostgreSQLDataService postgreSQLDataService) {
        this.agvDataService = agvDataService;
        this.kafkaProducerService = kafkaProducerService;
        this.postgreSQLDataService = postgreSQLDataService;
    }
    
    @Override
    public void initialize(ETLConfig config) {
        this.config = config;
        
        // PostgreSQL 연결 상태 확인
        if (!postgreSQLDataService.isConnected()) {
            log.error("PostgreSQL 연결 실패. ETL 엔진 초기화 중단.");
            status.set(EngineStatus.ERROR);
            return;
        }
        
        // PostgreSQL 테이블 존재 확인
        if (!postgreSQLDataService.isTableExists()) {
            log.error("PostgreSQL robot_info 테이블이 존재하지 않음. ETL 엔진 초기화 중단.");
            status.set(EngineStatus.ERROR);
            return;
        }
        
        status.set(EngineStatus.RUNNING);
        log.info("AgvDataETLEngine initialized successfully with PostgreSQL connection verified");
    }
    
    @Override
    public List<AgvData> executeETL() throws ETLEngineException {
        long startTime = System.currentTimeMillis();
        List<AgvData> processedData = new ArrayList<>();
        
        try {
            status.set(EngineStatus.RUNNING);
            
            // 설정이 없으면 기본 설정으로 초기화
            if (config == null) {
                log.info("ETL config is null, initializing with default configuration");
                initializeWithDefaultConfig();
            }
            
            // 1. Extract: 새로운 데이터 추출
            log.debug("Starting ETL process - Extract phase");
            List<AgvData> extractedData = extractData();
            
            if (extractedData.isEmpty()) {
                log.debug("No new data to process");
                return processedData;
            }
            
            // 2. Transform: 데이터 변환 및 검증
            log.debug("Starting ETL process - Transform phase");
            List<AgvData> transformedData = transformData(extractedData);
            
            // 3. Load: 데이터 적재
            log.debug("Starting ETL process - Load phase");
            processedData = loadData(transformedData);
            
            // 통계 업데이트
            updateStatistics(extractedData.size(), processedData.size(), startTime);
            
            lastExecutionTime.set(System.currentTimeMillis());
            
            log.info("ETL process completed: {} records processed", processedData.size());
            
        } catch (Exception e) {
            status.set(EngineStatus.ERROR);
            statistics.setErrorCount(statistics.getErrorCount() + 1);
            
            log.error("Error in ETL process: {}", e.getMessage(), e);
            
            // 오류 처리 모드에 따른 처리
            handleError(e);
        }
        
        return processedData;
    }
    
    @Override
    protected List<AgvData> extractData() throws ETLEngineException {
        try {
            // AGV 데이터 서비스에서 직접 데이터 추출
            List<AgvData> data = agvDataService.getAllAgvData();
            log.debug("Extracted {} records from AGV data service", data.size());
            return data;
        } catch (Exception e) {
            log.error("Error during data extraction: {}", e.getMessage(), e);
            throw new ETLEngineException("Error during data extraction", e);
        }
    }
    
    /**
     * 중복 데이터 필터링
     */
    private List<AgvData> filterDuplicateData(List<AgvData> data) {
        // PostgreSQL에 이미 저장된 데이터인지 확인
        return data.stream()
            .filter(agvData -> {
                try {
                    // UUID와 report_time으로 중복 체크
                    String dataId = agvData.getUuidNo() + "_" + agvData.getReportTime();
                    
                    // PostgreSQL에서 해당 데이터가 이미 존재하는지 확인
                    boolean exists = postgreSQLDataService.isAgvDataExists(agvData.getUuidNo(), agvData.getReportTime());
                    
                    if (exists) {
                        log.debug("Duplicate data filtered out: {}", dataId);
                        return false;
                    }
                    
                    return true;
                } catch (Exception e) {
                    log.warn("Error checking duplicate data: {}", e.getMessage());
                    return true; // 오류 발생 시 처리하도록 함
                }
            })
            .toList();
    }

    /**
     * 데이터 변환 (Transform)
     */
    private List<AgvData> transformData(List<AgvData> data) throws ETLEngineException {
        List<AgvData> transformedData = new ArrayList<>();
        
        for (AgvData agvData : data) {
            try {
                // 데이터 검증
                if (config.isValidationEnabled() && !isValidData(agvData)) {
                    log.warn("Invalid data detected, skipping: {}", agvData);
                    statistics.setSkippedRecords(statistics.getSkippedRecords() + 1);
                    continue;
                }
                
                // 데이터 변환
                if (config.isTransformationEnabled()) {
                    agvData = transformAgvData(agvData);
                }
                
                transformedData.add(agvData);
                statistics.setSuccessfulRecords(statistics.getSuccessfulRecords() + 1);
                
            } catch (Exception e) {
                log.error("Error transforming data: {}", e.getMessage());
                statistics.setFailedRecords(statistics.getFailedRecords() + 1);
                
                if (config.getErrorHandlingMode() == ETLConfig.ErrorHandlingMode.STOP) {
                    throw new ETLEngineException("Error during data transformation", e);
                }
            }
        }
        
        log.debug("Transformed {} records", transformedData.size());
        return transformedData;
    }
    
    /**
     * 데이터 적재 (Load) - 즉시 처리
     * 
     * 처리된 데이터를 PostgreSQL에 즉시 저장하고 Kafka로 전송
     * 실시간성을 위해 PostgreSQL을 우선적으로 처리
     */
    private List<AgvData> loadData(List<AgvData> data) throws ETLEngineException {
        try {
            if (data == null || data.isEmpty()) {
                log.debug("No data to load");
                return data;
            }
            
            // 중복 데이터 필터링
            List<AgvData> filteredData = filterDuplicateData(data);
            
            if (filteredData.isEmpty()) {
                log.debug("No new data to load after duplicate filtering");
                return filteredData;
            }
            
            log.info("Loading {} records to PostgreSQL and Kafka (immediate processing)", filteredData.size());
            
            int kafkaSuccessCount = 0;
            int postgresSuccessCount = 0;
            
            // PostgreSQL 우선 처리 (실시간성 보장)
            for (AgvData agvData : filteredData) {
                try {
                    // PostgreSQL에 데이터 즉시 저장 (우선순위)
                    boolean postgresSuccess = postgreSQLDataService.saveAgvData(agvData);
                    if (postgresSuccess) {
                        postgresSuccessCount++;
                        log.debug("PostgreSQL saved: robot_no={}", agvData.getRobotNo());
                    } else {
                        log.warn("PostgreSQL save failed: robot_no={}", agvData.getRobotNo());
                    }
                    
                    // Kafka에 데이터 전송 (비동기)
                    boolean kafkaSuccess = kafkaProducerService.sendAgvData(agvData);
                    if (kafkaSuccess) {
                        kafkaSuccessCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("Error loading data for robot_no={}: {}", agvData.getRobotNo(), e.getMessage(), e);
                }
            }
            
            log.info("Data loading completed: PostgreSQL={}/{}, Kafka={}/{}", 
                postgresSuccessCount, filteredData.size(), kafkaSuccessCount, filteredData.size());
            
            // ETL 상태를 Kafka로 전송
            try {
                kafkaProducerService.sendETLStatus(
                    "batch-" + System.currentTimeMillis(),
                    filteredData.size(),
                    postgresSuccessCount,
                    filteredData.size() - postgresSuccessCount,
                    System.currentTimeMillis() - lastExecutionTime.get(),
                    "COMPLETED"
                );
            } catch (Exception e) {
                log.error("Error sending ETL status to Kafka: {}", e.getMessage(), e);
            }
            
            // WebSocket으로 즉시 실시간 업데이트 전송 (비활성화)
            try {
                Map<String, Object> updateEvent = new HashMap<>();
                updateEvent.put("type", "ETL_UPDATE");
                updateEvent.put("timestamp", System.currentTimeMillis());
                updateEvent.put("processedCount", filteredData.size());
                updateEvent.put("kafkaSuccessCount", kafkaSuccessCount);
                updateEvent.put("postgresSuccessCount", postgresSuccessCount);
                updateEvent.put("data", filteredData);
                
                // WebSocket 기능 비활성화
                // messagingTemplate.convertAndSend("/topic/agv-updates", updateEvent);
                log.debug("WebSocket update disabled, data processed: {} records", filteredData.size());
            } catch (Exception e) {
                log.debug("WebSocket update skipped: {}", e.getMessage());
            }
            
            return filteredData;
            
        } catch (Exception e) {
            log.error("Error during data loading: {}", e.getMessage(), e);
            throw new ETLEngineException("Error during data loading", e);
        }
    }
    
    /**
     * 데이터 검증
     */
    private boolean isValidData(AgvData agvData) {
        if (agvData == null) {
            log.debug("Data validation failed: agvData is null");
            return false;
        }
        
        // 기본 필수 필드만 검사
        if (agvData.getRobotNo() == null || agvData.getRobotNo().trim().isEmpty()) {
            log.debug("Data validation failed: robotNo is null or empty");
            return false;
        }
        
        // 로봇 번호 형식 검사
        if (!agvData.getRobotNo().startsWith("ROBOT_")) {
            log.debug("Data validation failed: invalid robotNo format {} (should start with ROBOT_)", agvData.getRobotNo());
            return false;
        }
        
        // 상태 검사 (null 허용, 있으면 1-10 범위)
        if (agvData.getStatus() != null && (agvData.getStatus() < 1 || agvData.getStatus() > 10)) {
            log.debug("Data validation failed: invalid status {} for robot {}", agvData.getStatus(), agvData.getRobotNo());
            return false;
        }
        
        // 배터리 검사 (NULL 허용, 있으면 0-100 범위)
        if (agvData.getBattery() != null) {
            if (agvData.getBattery().compareTo(BigDecimal.ZERO) < 0 || 
                agvData.getBattery().compareTo(BigDecimal.valueOf(100)) > 0) {
                log.debug("Data validation failed: invalid battery {} for robot {}", agvData.getBattery(), agvData.getRobotNo());
                return false;
            }
        }
        
        // 속도 검사 (NULL 허용, 있으면 0-50 범위)
        if (agvData.getSpeed() != null) {
            if (agvData.getSpeed().compareTo(BigDecimal.ZERO) < 0 || 
                agvData.getSpeed().compareTo(BigDecimal.valueOf(50)) > 0) {
                log.debug("Data validation failed: invalid speed {} for robot {}", agvData.getSpeed(), agvData.getRobotNo());
                return false;
            }
        }
        
        // 좌표 검사 (NULL 허용, 있으면 합리적인 범위)
        if (agvData.getPosX() != null) {
            if (agvData.getPosX().compareTo(BigDecimal.valueOf(-10000)) < 0 || 
                agvData.getPosX().compareTo(BigDecimal.valueOf(10000)) > 0) {
                log.debug("Data validation failed: invalid posX {} for robot {}", agvData.getPosX(), agvData.getRobotNo());
                return false;
            }
        }
        
        if (agvData.getPosY() != null) {
            if (agvData.getPosY().compareTo(BigDecimal.valueOf(-10000)) < 0 || 
                agvData.getPosY().compareTo(BigDecimal.valueOf(10000)) > 0) {
                log.debug("Data validation failed: invalid posY {} for robot {}", agvData.getPosY(), agvData.getRobotNo());
                return false;
            }
        }
        
        log.debug("Data validation passed for robot: {}", agvData.getRobotNo());
        return true;
    }
    
    /**
     * AGV 데이터 변환
     */
    private AgvData transformAgvData(AgvData agvData) {
        // UUID가 없는 경우에만 생성 (원본 UUID 유지)
        if (agvData.getUuidNo() == null || agvData.getUuidNo().isEmpty()) {
            agvData.setUuidNo(UUID.randomUUID().toString());
        }
        
        // 좌표 정밀도 조정 (소수점 4자리)
        if (agvData.getPosX() != null) {
            agvData.setPosX(agvData.getPosX().setScale(4, BigDecimal.ROUND_HALF_UP));
        }
        if (agvData.getPosY() != null) {
            agvData.setPosY(agvData.getPosY().setScale(4, BigDecimal.ROUND_HALF_UP));
        }
        
        // 배터리 정밀도 조정 (소수점 4자리)
        if (agvData.getBattery() != null) {
            agvData.setBattery(agvData.getBattery().setScale(4, BigDecimal.ROUND_HALF_UP));
        }
        
        // 속도 정밀도 조정 (소수점 4자리)
        if (agvData.getSpeed() != null) {
            agvData.setSpeed(agvData.getSpeed().setScale(4, BigDecimal.ROUND_HALF_UP));
        }
        
        return agvData;
    }
    
    /**
     * 통계 업데이트
     */
    protected void updateStatistics(int extractedCount, int processedCount, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        
        statistics.setTotalProcessedRecords(statistics.getTotalProcessedRecords() + extractedCount);
        statistics.setTotalExecutionTime(statistics.getTotalExecutionTime() + executionTime);
        
        // 평균 처리 시간 계산
        if (statistics.getTotalProcessedRecords() > 0) {
            statistics.setAverageProcessingTime(
                (double) statistics.getTotalExecutionTime() / statistics.getTotalProcessedRecords()
            );
        }
        
        statistics.setLastExecutionTime(LocalDateTime.now());
    }
    
    /**
     * 오류 처리
     */
    private void handleError(Exception e) throws ETLEngineException {
        switch (config.getErrorHandlingMode()) {
            case CONTINUE:
                log.warn("Continuing despite error: {}", e.getMessage());
                break;
            case STOP:
                throw new ETLEngineException("ETL process stopped due to error", e);
            case RETRY:
                handleRetry(e);
                break;
        }
    }
    
    /**
     * 재시도 처리
     */
    private void handleRetry(Exception e) throws ETLEngineException {
        int currentRetries = 0;
        while (currentRetries < config.getRetryCount()) {
            try {
                log.info("Retrying ETL process (attempt {}/{})", currentRetries + 1, config.getRetryCount());
                Thread.sleep(config.getRetryInterval().toMillis());
                
                // 재시도 실행
                executeETL();
                return;
                
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new ETLEngineException("ETL retry interrupted", ie);
            } catch (Exception retryException) {
                currentRetries++;
                statistics.setRetryCount(statistics.getRetryCount() + 1);
                log.error("Retry attempt {} failed: {}", currentRetries, retryException.getMessage());
            }
        }
        
        throw new ETLEngineException("ETL process failed after " + config.getRetryCount() + " retries", e);
    }
    
    @Override
    protected List<AgvData> transformAndLoad(List<AgvData> data) throws ETLEngineException {
        return transformData(data);
    }
    
    @Override
    public boolean isConnected() {
        return postgreSQLDataService.isConnected();
    }
    
    @Override
    protected String getDataKey(AgvData data) {
        return data.getRobotNo();
    }
    
    @Override
    protected boolean isSameData(AgvData data1, AgvData data2) {
        return data1.getRobotNo().equals(data2.getRobotNo()) &&
               data1.getTimestamp().equals(data2.getTimestamp());
    }
    
    @Override
    public boolean isHealthy() {
        return status.get() == EngineStatus.RUNNING && isConnected();
    }
    
    @Override
    public long getLastExecutionTime() {
        return lastExecutionTime.get();
    }
    
    @Override
    public ETLStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * 기본 설정으로 ETL 엔진 초기화
     */
    private void initializeWithDefaultConfig() {
        // 기본 ETL 설정 생성
        ETLConfig defaultConfig = new ETLConfig();
        defaultConfig.setValidationEnabled(true);
        defaultConfig.setTransformationEnabled(true);
        defaultConfig.setErrorHandlingMode(ETLConfig.ErrorHandlingMode.CONTINUE);
        
        // 기본 풀링 설정 (0.1초 주기 - 실시간 데이터 감지)
        defaultConfig.setPullInterval(java.time.Duration.ofMillis(100)); // 0.1초 (100ms)
        defaultConfig.setBatchSize(100);
        
        // 초기화 실행
        initialize(defaultConfig);
    }
} 