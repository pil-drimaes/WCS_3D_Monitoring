package com.example.cdcqueue.etl.service;

import com.example.cdcqueue.common.model.AgvData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
     * 단일 AGV 데이터 저장 (UPSERT) - 강화된 로깅
     * 
     * @param agvData 저장할 AGV 데이터
     * @return 저장 성공 여부
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public boolean saveAgvData(AgvData agvData) {
        try {
            // PostgreSQL 연결 상태 확인
            if (!isConnected()) {
                log.error("PostgreSQL 연결 실패: robot_no={}", agvData.getRobotNo());
                return false;
            }
            
            String sql = """
                INSERT INTO robot_info (
                    uuid_no, robot_no, map_code, zone_code, status, manual, 
                    loaders, report_time, battery, node_id, pos_x, pos_y, 
                    speed, task_id, next_target, pod_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            int result = postgresqlJdbcTemplate.update(sql,
                agvData.getUuidNo(),
                agvData.getRobotNo(),
                agvData.getMapCode(),
                agvData.getZoneCode(),
                agvData.getStatus(),
                agvData.getManual(),
                agvData.getLoaders(),
                agvData.getReportTime(),
                agvData.getBattery(),
                agvData.getNodeId(),
                agvData.getPosX(),
                agvData.getPosY(),
                agvData.getSpeed(),
                agvData.getTaskId(),
                agvData.getNextTarget(),
                agvData.getPodId()
            );
            
            if (result > 0) {
                log.debug("PostgreSQL AGV 데이터 저장 성공: robot_no={}, uuid_no={}", 
                    agvData.getRobotNo(), agvData.getUuidNo());
                return true;
            } else {
                log.warn("PostgreSQL AGV 데이터 저장 실패 (0 rows affected): robot_no={}", 
                    agvData.getRobotNo());
                return false;
            }
            
        } catch (Exception e) {
            log.error("PostgreSQL AGV 데이터 저장 예외: robot_no={}, error={}", 
                agvData.getRobotNo(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 배치로 AGV 데이터 저장
     * 
     * @param agvDataList 저장할 AGV 데이터 리스트
     * @return 성공적으로 저장된 레코드 수
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int saveAgvDataBatch(List<AgvData> agvDataList) {
        if (agvDataList == null || agvDataList.isEmpty()) {
            return 0;
        }
        
        try {
            String sql = """
                INSERT INTO robot_info (
                    uuid_no, robot_no, map_code, zone_code, status, manual, 
                    loaders, report_time, battery, node_id, pos_x, pos_y, 
                    speed, task_id, next_target, pod_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            int totalUpdated = 0;
            for (AgvData agvData : agvDataList) {
                int result = postgresqlJdbcTemplate.update(sql,
                    agvData.getUuidNo(),
                    agvData.getRobotNo(),
                    agvData.getMapCode(),
                    agvData.getZoneCode(),
                    agvData.getStatus(),
                    agvData.getManual(),
                    agvData.getLoaders(),
                    agvData.getReportTime(),
                    agvData.getBattery(),
                    agvData.getNodeId(),
                    agvData.getPosX(),
                    agvData.getPosY(),
                    agvData.getSpeed(),
                    agvData.getTaskId(),
                    agvData.getNextTarget(),
                    agvData.getPodId()
                );
                totalUpdated += result;
            }
            
            log.info("배치 AGV 데이터 저장 완료: 총 {}개 중 {}개 저장", agvDataList.size(), totalUpdated);
            return totalUpdated;
            
        } catch (Exception e) {
            log.error("배치 AGV 데이터 저장 실패: error={}", e.getMessage(), e);
            return 0;
        }
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
    
    /**
     * robot_info 테이블 존재 여부 확인
     * 
     * @return 테이블 존재 여부
     */
    public boolean isTableExists() {
        try {
            if (!isConnected()) {
                log.error("PostgreSQL 연결이 없어 테이블 존재 여부를 확인할 수 없음");
                return false;
            }
            
            String sql = """
                SELECT EXISTS (
                    SELECT FROM information_schema.tables 
                    WHERE table_schema = 'public' 
                    AND table_name = 'robot_info'
                )
                """;
            
            Boolean exists = postgresqlJdbcTemplate.queryForObject(sql, Boolean.class);
            boolean tableExists = exists != null && exists;
            
            if (tableExists) {
                log.info("PostgreSQL robot_info 테이블이 존재함");
            } else {
                log.warn("PostgreSQL robot_info 테이블이 존재하지 않음");
            }
            
            return tableExists;
            
        } catch (Exception e) {
            log.error("PostgreSQL 테이블 존재 여부 확인 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * robot_info 테이블의 레코드 수 조회
     * 
     * @return 레코드 수
     */
    public int getRobotInfoCount() {
        try {
            if (!isConnected()) {
                log.error("PostgreSQL 연결이 없어 레코드 수를 조회할 수 없음");
                return 0;
            }
            
            String sql = "SELECT COUNT(*) FROM robot_info";
            Integer count = postgresqlJdbcTemplate.queryForObject(sql, Integer.class);
            
            int recordCount = count != null ? count : 0;
            log.debug("PostgreSQL robot_info 테이블 레코드 수: {}", recordCount);
            
            return recordCount;
            
        } catch (Exception e) {
            log.error("PostgreSQL 레코드 수 조회 실패: {}", e.getMessage(), e);
            return 0;
        }
    }
} 