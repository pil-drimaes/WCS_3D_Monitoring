package com.example.cdcqueue.etl.service;

import com.example.cdcqueue.common.model.AgvData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

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
    
    /**
     * 독립적인 JdbcTemplate
     */
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 데이터베이스 연결 설정
     */
    @Value("${etl.database.url:jdbc:sqlserver://localhost:1433;databaseName=cdc_test;encrypt=true;trustServerCertificate=true}")
    private String databaseUrl;
    
    @Value("${etl.database.username:sa}")
    private String username;
    
    @Value("${etl.database.password:nice2025!}")
    private String password;
    
    @Value("${etl.database.driver:com.microsoft.sqlserver.jdbc.SQLServerDriver}")
    private String driverClassName;
    
    /**
     * 초기화
     */
    @PostConstruct
    public void initialize() {
        try {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(driverClassName);
            dataSource.setUrl(databaseUrl);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            
            this.jdbcTemplate = new JdbcTemplate(dataSource);
            
            // 연결 테스트
            if (isConnected()) {
                log.info("IndependentWcsService initialized successfully");
            } else {
                log.error("Failed to connect to WCS database");
            }
        } catch (Exception e) {
            log.error("Error initializing IndependentWcsService: {}", e.getMessage(), e);
        }
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
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
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
        } catch (Exception e) {
            log.error("Error getting all AGV data: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * 최신 AGV 데이터 10개를 조회
     * 
     * @return 최신 AGV 데이터 10개 리스트
     */
    public List<AgvData> getLatestAgvData() {
        try {
            String sql = """
                SELECT TOP 10 uuid_no, robot_no, map_code, zone_code, status, manual, 
                       loaders, report_time, battery, node_id, pos_x, pos_y, 
                       speed, task_id, next_target, pod_id
                FROM robot_info 
                ORDER BY report_time DESC
                """;
            
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
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
        } catch (Exception e) {
            log.error("Error getting latest AGV data: {}", e.getMessage(), e);
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
        try {
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
            
            List<AgvData> result = jdbcTemplate.query(sql, (rs, rowNum) -> {
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
            
            log.debug("Found {} AGV data records after timestamp {}", result.size(), timestamp);
            return result;
        } catch (Exception e) {
            log.error("Error getting AGV data after timestamp: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * 최신 타임스탬프 조회
     * 
     * @return 최신 타임스탬프
     */
    public LocalDateTime getLatestTimestamp() {
        try {
            String sql = "SELECT MAX(report_time) as LatestTime FROM robot_info";
            
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Long timestamp = rs.getLong("LatestTime");
                return timestamp != null ? 
                    LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC) : 
                    LocalDateTime.now();
            });
        } catch (Exception e) {
            log.error("Error getting latest timestamp: {}", e.getMessage(), e);
            return LocalDateTime.now();
        }
    }
    
    /**
     * 데이터베이스 연결 상태 확인
     * 
     * @return 연결 상태
     */
    public boolean isConnected() {
        try {
            if (jdbcTemplate == null) {
                return false;
            }
            
            // 간단한 쿼리로 연결 테스트
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 테이블 구조 조회
     * 
     * @return 테이블 구조 정보
     */
    public List<java.util.Map<String, Object>> getTableStructure() {
        try {
            String sql = """
                SELECT 
                    COLUMN_NAME,
                    DATA_TYPE,
                    IS_NULLABLE,
                    COLUMN_DEFAULT
                FROM INFORMATION_SCHEMA.COLUMNS 
                WHERE TABLE_NAME = 'robot_info'
                ORDER BY ORDINAL_POSITION
                """;
            
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting table structure: {}", e.getMessage(), e);
            return List.of();
        }
    }
} 