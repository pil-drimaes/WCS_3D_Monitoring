package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.AgvData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 독립적인 WCS DB 서비스
 * 
 * 기존 CDC 시스템과 완전히 분리된 새로운 ETL 시스템용 서비스
 * 자체 데이터소스를 생성하여 사용합니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Service
public class AgvDataService {
    
    private static final Logger log = LoggerFactory.getLogger(AgvDataService.class);
    
    private final JdbcTemplate wcsJdbcTemplate;
    private PostgreSQLDataService postgresqlDataService;
    
    @Autowired
    public AgvDataService(@Qualifier("wcsJdbcTemplate") JdbcTemplate wcsJdbcTemplate,
                          PostgreSQLDataService postgresqlDataService) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;  
        this.postgresqlDataService = postgresqlDataService;
    }
    
    /**
     * 모든 AGV 데이터를 조회 (최신순 정렬)
     * 
     * @return 모든 AGV 데이터 리스트
     */
    public List<AgvData> getAllAgvData() {
        try {
            String sql = """
                SELECT uuid_no, robot_no, map_code, zone_code, status, manual, 
                       loaders, report_time, battery, node_id, pos_x, pos_y, 
                       speed, task_id, next_target, pod_id
                FROM robot_info 
                ORDER BY report_time DESC
                """;
            
            List<AgvData> result = wcsJdbcTemplate.query(sql, (rs, rowNum) -> {
                AgvData agvData = new AgvData();
                agvData.setUuidNo(rs.getString("uuid_no"));
                agvData.setRobotNo(rs.getString("robot_no"));
                agvData.setMapCode(rs.getString("map_code"));
                agvData.setZoneCode(rs.getString("zone_code"));
                agvData.setStatus(rs.getInt("status"));
                agvData.setManual(rs.getBoolean("manual"));
                agvData.setLoaders(rs.getString("loaders"));
                agvData.setReportTime(rs.getLong("report_time"));
                agvData.setBattery(rs.getBigDecimal("battery"));
                agvData.setNodeId(rs.getString("node_id"));
                agvData.setPosX(rs.getBigDecimal("pos_x"));
                agvData.setPosY(rs.getBigDecimal("pos_y"));
                agvData.setSpeed(rs.getBigDecimal("speed"));
                agvData.setTaskId(rs.getString("task_id"));
                agvData.setNextTarget(rs.getString("next_target"));
                agvData.setPodId(rs.getString("pod_id"));
                return agvData;
            });

            log.debug("WCS DB에서 AGV 데이터 {}개 조회 완료", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("WCS DB에서 AGV 데이터 조회 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    
    /**
     * 특정 시간 이후의 AGV 데이터를 조회
     * 
     * @param timestamp 기준 시간
     * @return 해당 시간 이후의 AGV 데이터 리스트
     */
    public List<AgvData> getAgvDataAfterTimestamp(LocalDateTime timestamp) {
            // LocalDateTime을 시스템 기본 시간대의 Unix timestamp로 변환
            long timestampMillis = timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            log.debug("Querying AGV data after timestamp: {} ({} ms)", timestamp, timestampMillis);
            
            String sql = """
                SELECT uuid_no, robot_no, map_code, zone_code, status, manual, 
                       loaders, report_time, battery, node_id, pos_x, pos_y, 
                       speed, task_id, next_target, pod_id
                FROM robot_info 
                WHERE report_time > ? 
                ORDER BY report_time DESC
                """;
            
            return wcsJdbcTemplate.query(sql, (rs, rowNum) -> {
                AgvData agvData = new AgvData();
                
                agvData.setUuidNo(rs.getString("uuid_no"));
                agvData.setRobotNo(rs.getString("robot_no"));
                agvData.setMapCode(rs.getString("map_code"));
                agvData.setZoneCode(rs.getString("zone_code"));
                agvData.setStatus(rs.getInt("status"));
                agvData.setManual(rs.getBoolean("manual"));
                agvData.setLoaders(rs.getString("loaders"));
                agvData.setReportTime(rs.getLong("report_time"));
                agvData.setBattery(rs.getBigDecimal("battery"));
                agvData.setNodeId(rs.getString("node_id"));
                agvData.setPosX(rs.getBigDecimal("pos_x"));
                agvData.setPosY(rs.getBigDecimal("pos_y"));
                agvData.setSpeed(rs.getBigDecimal("speed"));
                agvData.setTaskId(rs.getString("task_id"));
                agvData.setNextTarget(rs.getString("next_target"));
                agvData.setPodId(rs.getString("pod_id"));
                return agvData;
            }, timestampMillis);
        }

    /**
     * PostgreSQL에 AGV 데이터 저장
     * 
     * @param agvData 저장할 AGV 데이터
     * @return 저장 성공 여부
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public boolean saveAgvData(AgvData agvData) {
        return postgresqlDataService.saveAgvData(agvData);
    }

    /**
     * 배치로 AGV 데이터 저장
     * 
     * @param agvDataList 저장할 AGV 데이터 리스트
     * @return 성공적으로 저장된 레코드 수
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int saveAgvDataBatch(List<AgvData> agvDataList) {
        return postgresqlDataService.saveAgvDataBatch(agvDataList);
    }


    /**
     * 최신 타임스탬프 조회
     * 
     * @return 최신 타임스탬프
     */
    public LocalDateTime getLatestTimestamp() {
        try {
            String sql = "SELECT MAX(report_time) as LatestTime FROM robot_info";
            
            Long timestamp = wcsJdbcTemplate.queryForObject(sql, Long.class);
            if (timestamp != null) {
                return LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);
            } else {
                log.info("AGV 데이터가 없어 1년 전부터 처리하도록 설정");
                return LocalDateTime.now().minusYears(1);
            }
        } catch (Exception e) {
            log.error("AGV 데이터 최신 타임스탬프 조회 실패: {}", e.getMessage(), e);
            return LocalDateTime.now().minusYears(1);
        }
    }
    
    /**
     * 데이터베이스 연결 상태 확인
     * 
     * @return 연결 상태
     */
    public boolean isConnected() {
        try {
            wcsJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.error("WCS Database connection test failed: {}", e.getMessage());
            return false;
        }
    }
} 