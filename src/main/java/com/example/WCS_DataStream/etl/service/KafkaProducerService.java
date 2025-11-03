package com.example.WCS_DataStream.etl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Kafka Producer 서비스
 * ETL 엔진에서 처리된 데이터를 Kafka 토픽으로 전송
 */
@Service
public class KafkaProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topic.agv-data:agv-data-events}")
    private String agvDataTopic;
    
    @Value("${kafka.topic.etl-status:etl-status-events}")
    private String etlStatusTopic;
    
    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * 콜백 + (옵션) 동기 전송 지원
     */
    public boolean sendMessageWithCallback(String topic, String key, Object value, boolean sync) {
        try {
            var future = kafkaTemplate.send(topic, key, value);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka send failed: topic={}, key={}, error={}", topic, key, ex.getMessage(), ex);
                } else if (result != null && result.getRecordMetadata() != null) {
                    log.info("Kafka sent: topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.info("Kafka sent: topic={}, metadata is null", topic);
                }
            });
            if (sync) {
                future.get(5, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("Kafka send exception: topic={}, key={}, error={}", topic, key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * ETL 상태를 Kafka로 전송
     */
    public boolean sendETLStatus(String batchId, int totalCount, int successCount, 
                                int failureCount, long processingTime, String status) {
        try {
            // 간단한 상태 객체 생성
            ETLStatusMessage statusMessage = new ETLStatusMessage(
                batchId, totalCount, successCount, failureCount, processingTime, status
            );
            
            kafkaTemplate.send(etlStatusTopic, batchId, statusMessage);
            log.debug("ETL status sent to Kafka: batch_id={}", batchId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send ETL status to Kafka: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 일반 메시지를 Kafka로 전송 (다른 ETL 엔진들과의 호환성)
     */
    public boolean sendMessage(String topic, String message) {
        try {
            kafkaTemplate.send(topic, "message", message);
            log.debug("Message sent to Kafka topic {}: {}", topic, message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send message to Kafka topic {}: {}", topic, e.getMessage());
            return false;
        }
    }
    
    /**
     * 일반 메시지를 Kafka로 전송 (키와 값 포함)
     */
    public boolean sendMessage(String topic, String key, String message) {
        try {
            kafkaTemplate.send(topic, key, message);
            log.debug("Message sent to Kafka topic {} with key {}: {}", topic, key, message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send message to Kafka topic {} with key {}: {}", topic, key, e.getMessage());
            return false;
        }
    }
    
    /**
     * ETL 상태 메시지 클래스
     */
    public static class ETLStatusMessage {
        private String batchId;
        private int totalCount;
        private int successCount;
        private int failureCount;
        private long processingTime;
        private String status;
        
        // Jackson 역직렬화를 위한 기본 생성자
        public ETLStatusMessage() {
        }
        
        public ETLStatusMessage(String batchId, int totalCount, int successCount, 
                               int failureCount, long processingTime, String status) {
            this.batchId = batchId;
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.processingTime = processingTime;
            this.status = status;
        }
        
        // Getters
        public String getBatchId() { return batchId; }
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getProcessingTime() { return processingTime; }
        public String getStatus() { return status; }
        
        // Jackson 역직렬화를 위한 Setters
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
        public void setStatus(String status) { this.status = status; }
    }
} 