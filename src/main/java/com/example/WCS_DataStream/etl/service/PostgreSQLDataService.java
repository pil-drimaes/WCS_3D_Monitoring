package com.example.WCS_DataStream.etl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * PostgreSQL 데이터 쓰기 서비스
 * 
 * ETL에서 처리된 AGV 데이터를 PostgreSQL에 저장하는 서비스
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Service
public class PostgreSQLDataService {
    
    private static final Logger log = LoggerFactory.getLogger(PostgreSQLDataService.class);
    
    private final JdbcTemplate postgresqlJdbcTemplate;
    
    public PostgreSQLDataService(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }
    
    
    /**
     * ETL 처리 이력 저장
     * 
     * @param batchId 배치 ID
     * @param processedCount 처리된 레코드 수
     * @param successCount 성공한 레코드 수
     * @param failedCount 실패한 레코드 수
     * @param skippedCount 건너뛴 레코드 수
     * @param processingTimeMs 처리 시간 (밀리초)
     * @param status 처리 상태
     * @param errorMessage 오류 메시지
     */
    public void saveETLHistory(String batchId, int processedCount, int successCount, 
                              int failedCount, int skippedCount, long processingTimeMs, 
                              String status, String errorMessage) {
        try {
            String sql = """
                INSERT INTO etl_processing_history (
                    batch_id, processed_count, success_count, failed_count, skipped_count,
                    processing_time_ms, end_time, status, error_message
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)
                """;
            
            postgresqlJdbcTemplate.update(sql,
                batchId,
                processedCount,
                successCount,
                failedCount,
                skippedCount,
                processingTimeMs,
                status,
                errorMessage
            );
            
            log.debug("ETL 처리 이력 저장 완료: batch_id={}, status={}", batchId, status);
            
        } catch (Exception e) {
            log.error("ETL 처리 이력 저장 실패: batch_id={}, error={}", batchId, e.getMessage(), e);
        }
    }
    
    /**
     * Kafka 메시지 이력 저장
     * 
     * @param topic 토픽명
     * @param partition 파티션 번호
     * @param offset 오프셋
     * @param key 메시지 키
     * @param message 메시지 내용
     * @param status 상태
     * @param errorMessage 오류 메시지
     */
    public void saveKafkaMessageHistory(String topic, Integer partition, Long offset, 
                                       String key, String message, String status, String errorMessage) {
        try {
            String sql = """
                INSERT INTO kafka_message_history (
                    topic, partition, message_offset, key, message, status, error_message
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            
            postgresqlJdbcTemplate.update(sql,
                topic,
                partition,
                offset,
                key,
                message,
                status,
                errorMessage
            );
            
            log.debug("Kafka 메시지 이력 저장 완료: topic={}, status={}", topic, status);
            
        } catch (Exception e) {
            log.error("Kafka 메시지 이력 저장 실패: topic={}, error={}", topic, e.getMessage(), e);
        }
    }
    
    /**
     * 데이터베이스 연결 상태 확인
     * 
     * @return 연결 상태
     */
    public boolean isConnected() {
        try {
            if (postgresqlJdbcTemplate == null) {
                log.error("PostgreSQL JdbcTemplate is null");
                return false;
            }
            
            // 간단한 쿼리로 연결 테스트
            postgresqlJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.debug("PostgreSQL 연결 상태 확인 성공");
            return true;
        } catch (Exception e) {
            log.error("PostgreSQL 연결 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }
    


    
} 