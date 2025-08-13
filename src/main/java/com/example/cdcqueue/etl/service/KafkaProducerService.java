package com.example.cdcqueue.etl.service;

import com.example.cdcqueue.common.model.AgvData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer 서비스
 * 
 * ETL에서 처리된 AGV 데이터를 Kafka 토픽으로 전송하는 서비스
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Service
public class KafkaProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PostgreSQLDataService postgreSQLDataService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    @Qualifier("agvDataTopic")
    private String agvDataTopic;
    
    @Autowired
    @Qualifier("etlStatusTopic")
    private String etlStatusTopic;
    
    @Autowired
    @Qualifier("errorEventsTopic")
    private String errorEventsTopic;
    
    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate,
                               PostgreSQLDataService postgreSQLDataService,
                               ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.postgreSQLDataService = postgreSQLDataService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 일반 메시지를 Kafka로 전송
     * 
     * @param topic 토픽명
     * @param message 전송할 메시지
     * @return 전송 성공 여부
     */
    public boolean sendMessage(String topic, String message) {
        try {
            String key = UUID.randomUUID().toString();
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
            
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.debug("메시지 Kafka 전송 성공: topic={}, partition={}, offset={}", 
                        result.getRecordMetadata().topic(), 
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    
                    // 성공 이력 저장
                    postgreSQLDataService.saveKafkaMessageHistory(
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key,
                        message,
                        "SENT",
                        null
                    );
                } else {
                    log.error("메시지 Kafka 전송 실패: topic={}, error={}", 
                        topic, throwable.getMessage(), throwable);
                    
                    // 실패 이력 저장
                    postgreSQLDataService.saveKafkaMessageHistory(
                        topic,
                        null,
                        null,
                        key,
                        message,
                        "FAILED",
                        throwable.getMessage()
                    );
                }
            });
            
            return true;
            
        } catch (Exception e) {
            log.error("메시지 Kafka 전송 예외: topic={}, error={}", topic, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 단일 AGV 데이터를 Kafka로 전송
     * 
     * @param agvData 전송할 AGV 데이터
     * @return 전송 성공 여부
     */
    public boolean sendAgvData(AgvData agvData) {
        try {
            String key = agvData.getRobotNo();
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(agvDataTopic, key, agvData);
            
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.debug("AGV 데이터 Kafka 전송 성공: robot_no={}, topic={}, partition={}, offset={}", 
                        agvData.getRobotNo(), result.getRecordMetadata().topic(), 
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    
                    // 성공 이력 저장
                    postgreSQLDataService.saveKafkaMessageHistory(
                        agvDataTopic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key,
                        serializeToJson(agvData),
                        "SENT",
                        null
                    );
                } else {
                    log.error("AGV 데이터 Kafka 전송 실패: robot_no={}, error={}", 
                        agvData.getRobotNo(), throwable.getMessage(), throwable);
                    
                    // 실패 이력 저장
                    postgreSQLDataService.saveKafkaMessageHistory(
                        agvDataTopic,
                        null,
                        null,
                        key,
                        serializeToJson(agvData),
                        "FAILED",
                        throwable.getMessage()
                    );
                }
            });
            
            return true;
            
        } catch (Exception e) {
            log.error("AGV 데이터 Kafka 전송 중 오류: robot_no={}, error={}", 
                agvData.getRobotNo(), e.getMessage(), e);
            
            // 오류 이력 저장
            postgreSQLDataService.saveKafkaMessageHistory(
                agvDataTopic,
                null,
                null,
                agvData.getRobotNo(),
                serializeToJson(agvData),
                "ERROR",
                e.getMessage()
            );
            
            return false;
        }
    }
    
    /**
     * 배치로 AGV 데이터를 Kafka로 전송
     * 
     * @param agvDataList 전송할 AGV 데이터 리스트
     * @return 성공적으로 전송된 레코드 수
     */
    public int sendAgvDataBatch(List<AgvData> agvDataList) {
        if (agvDataList == null || agvDataList.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        
        for (AgvData agvData : agvDataList) {
            if (sendAgvData(agvData)) {
                successCount++;
            }
        }
        
        log.info("배치 AGV 데이터 Kafka 전송 완료: 총 {}개 중 {}개 전송", agvDataList.size(), successCount);
        return successCount;
    }
    
    /**
     * ETL 상태 정보를 Kafka로 전송
     * 
     * @param batchId 배치 ID
     * @param processedCount 처리된 레코드 수
     * @param successCount 성공한 레코드 수
     * @param failedCount 실패한 레코드 수
     * @param processingTimeMs 처리 시간 (밀리초)
     * @param status 처리 상태
     */
    public void sendETLStatus(String batchId, int processedCount, int successCount, 
                             int failedCount, long processingTimeMs, String status) {
        try {
            ETLStatusMessage statusMessage = new ETLStatusMessage();
            statusMessage.setBatchId(batchId);
            statusMessage.setProcessedCount(processedCount);
            statusMessage.setSuccessCount(successCount);
            statusMessage.setFailedCount(failedCount);
            statusMessage.setProcessingTimeMs(processingTimeMs);
            statusMessage.setStatus(status);
            statusMessage.setTimestamp(LocalDateTime.now());
            
            String key = "etl-status-" + batchId;
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(etlStatusTopic, key, statusMessage);
            
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.debug("ETL 상태 Kafka 전송 성공: batch_id={}, status={}", batchId, status);
                } else {
                    log.error("ETL 상태 Kafka 전송 실패: batch_id={}, error={}", batchId, throwable.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("ETL 상태 Kafka 전송 중 오류: batch_id={}, error={}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * 에러 이벤트를 Kafka로 전송
     * 
     * @param errorType 에러 타입
     * @param errorMessage 에러 메시지
     * @param source 소스 정보
     * @param details 상세 정보
     */
    public void sendErrorEvent(String errorType, String errorMessage, String source, Object details) {
        try {
            ErrorEventMessage errorEvent = new ErrorEventMessage();
            errorEvent.setErrorType(errorType);
            errorEvent.setErrorMessage(errorMessage);
            errorEvent.setSource(source);
            errorEvent.setDetails(details);
            errorEvent.setTimestamp(LocalDateTime.now());
            
            String key = "error-" + UUID.randomUUID().toString();
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(errorEventsTopic, key, errorEvent);
            
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.debug("에러 이벤트 Kafka 전송 성공: error_type={}, source={}", errorType, source);
                } else {
                    log.error("에러 이벤트 Kafka 전송 실패: error_type={}, error={}", errorType, throwable.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("에러 이벤트 Kafka 전송 중 오류: error_type={}, error={}", errorType, e.getMessage(), e);
        }
    }
    
    /**
     * 객체를 JSON 문자열로 직렬화
     * 
     * @param obj 직렬화할 객체
     * @return JSON 문자열
     */
    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * ETL 상태 메시지 클래스
     */
    public static class ETLStatusMessage {
        private String batchId;
        private int processedCount;
        private int successCount;
        private int failedCount;
        private long processingTimeMs;
        private String status;
        private LocalDateTime timestamp;
        
        // Getter/Setter
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        
        public int getProcessedCount() { return processedCount; }
        public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 에러 이벤트 메시지 클래스
     */
    public static class ErrorEventMessage {
        private String errorType;
        private String errorMessage;
        private String source;
        private Object details;
        private LocalDateTime timestamp;
        
        // Getter/Setter
        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public Object getDetails() { return details; }
        public void setDetails(Object details) { this.details = details; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
} 