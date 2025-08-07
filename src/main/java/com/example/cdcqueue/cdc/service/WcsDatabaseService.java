package com.example.cdcqueue.cdc.service;

import com.example.cdcqueue.common.model.AgvData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WCS 데이터베이스 서비스
 * 
 * 기존 CDC 시스템용 WCS DB 서비스
 * 실제 AGV 데이터 테이블 구조에 맞춰 구현
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Service
public class WcsDatabaseService {
    
    private static final Logger log = LoggerFactory.getLogger(WcsDatabaseService.class);
    
    /**
     * WCS 데이터베이스용 JdbcTemplate
     */
    private final JdbcTemplate wcsJdbcTemplate;
    
    /**
     * 생성자
     * 
     * @param wcsJdbcTemplate WCS DB용 JdbcTemplate
     */
    public WcsDatabaseService(@Qualifier("wcsJdbcTemplate") JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }
    
    /**
     * 모든 AGV 데이터를 조회 (최신순 정렬)
     * 
     * @return 모든 AGV 데이터 리스트
     */
    public List<AgvData> getAllAgvData() {
        String sql = """
            SELECT uuid_no, robot_no, map_code, zone_code, status, manual, 
                   loaders, report_time, battery, node_id, pos_x, pos_y, 
                   speed, task_id, next_target, pod_id
            FROM agv_data 
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
        });
    }
    
    /**
     * 최신 AGV 데이터 10개를 조회
     * 
     * @return 최신 AGV 데이터 10개 리스트
     */
    public List<AgvData> getLatestAgvData() {
        String sql = """
            SELECT TOP 10 uuid_no, robot_no, map_code, zone_code, status, manual, 
                   loaders, report_time, battery, node_id, pos_x, pos_y, 
                   speed, task_id, next_target, pod_id
            FROM agv_data 
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
        });
    }
    
    /**
     * 특정 시간 이후의 AGV 데이터를 조회
     * 
     * @param timestamp 기준 시간
     * @return 기준 시간 이후의 AGV 데이터 리스트
     */
    public List<AgvData> getAgvDataAfterTimestamp(LocalDateTime timestamp) {
        // LocalDateTime을 Unix timestamp로 변환
        long timestampMillis = timestamp.toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
        
        String sql = """
            SELECT uuid_no, robot_no, map_code, zone_code, status, manual, 
                   loaders, report_time, battery, node_id, pos_x, pos_y, 
                   speed, task_id, next_target, pod_id
            FROM agv_data 
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
     * 데이터베이스의 최신 타임스탬프를 조회
     * 
     * @return 최신 타임스탬프 (데이터가 없으면 현재 시간)
     */
    public LocalDateTime getLatestTimestamp() {
        String sql = "SELECT MAX(report_time) as latest_timestamp FROM agv_data";
        
        try {
            Long timestamp = wcsJdbcTemplate.queryForObject(sql, Long.class);
            return timestamp != null ? 
                LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC) : 
                LocalDateTime.now();
        } catch (Exception e) {
            log.error("Error getting latest timestamp: {}", e.getMessage(), e);
            return LocalDateTime.now();
        }
    }
    
    /**
     * 테이블 구조 조회
     * 
     * @return 테이블 구조 정보
     */
    public List<java.util.Map<String, Object>> getTableStructure() {
        String sql = """
            SELECT 
                COLUMN_NAME,
                DATA_TYPE,
                IS_NULLABLE,
                COLUMN_DEFAULT
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_NAME = 'agv_data'
            ORDER BY ORDINAL_POSITION
            """;
        
        return wcsJdbcTemplate.queryForList(sql);
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