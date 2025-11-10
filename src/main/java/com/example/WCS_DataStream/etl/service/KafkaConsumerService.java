package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.service.KafkaProducerService.ETLStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka Consumer 서비스
 * Kafka 토픽에서 메시지를 수신하여 처리
 */
@Service
public class KafkaConsumerService {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);
    

    /**
     * ETL 상태 토픽 수신
     */
    @KafkaListener(topics = "${kafka.topic.etl-status:etl-status-events}", 
                   groupId = "${spring.kafka.consumer.group-id:agv-etl-group}")
    public void consumeETLStatus(ETLStatusMessage statusMessage) {
        try {
            log.info("Received ETL status from Kafka: batch_id={}, status={}, processed={}/{}", 
                     statusMessage.getBatchId(), statusMessage.getStatus(), 
                     statusMessage.getSuccessCount(), statusMessage.getTotalCount());
            
            // 여기에 ETL 상태 처리 로직 추가
            processETLStatus(statusMessage);
            
        } catch (Exception e) {
            log.error("Error processing ETL status from Kafka: {}", e.getMessage(), e);
        }
    }

    /**
     * ETL 상태 처리
     */
    private void processETLStatus(ETLStatusMessage statusMessage) {
        // TODO: ETL 상태 처리 로직 구현
        log.debug("Processing ETL status: batch_id={}, status={}", 
                 statusMessage.getBatchId(), statusMessage.getStatus());
    }
} 