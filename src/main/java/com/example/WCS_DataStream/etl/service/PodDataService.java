package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.PodInfo;
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
 * POD 정보 데이터 서비스
 * 
 * POD 정보 테이블과 상호작용하는 서비스
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Service
public class PodDataService {
    
    private static final Logger log = LoggerFactory.getLogger(PodDataService.class);
    
    private final JdbcTemplate wcsJdbcTemplate;
    private PostgreSQLDataService postgresqlDataService;
    
    @Autowired
    public PodDataService(@Qualifier("wcsJdbcTemplate") JdbcTemplate wcsJdbcTemplate,
                          PostgreSQLDataService postgresqlDataService) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
        this.postgresqlDataService = postgresqlDataService;
    }

    
    /**
     * WCS DB에서 모든 POD 데이터를 조회
     * 
     * @return 모든 POD 데이터 리스트
     */
    public List<PodInfo> getAllPodData() {
        try {
            String sql = """
                SELECT uuid_no, pod_id, pod_face, location, report_time
                FROM pod_info 
                ORDER BY report_time DESC
                """;
            
            List<PodInfo> result = wcsJdbcTemplate.query(sql, (rs, rowNum) -> {
                PodInfo podInfo = new PodInfo();
                podInfo.setUuidNo(rs.getString("uuid_no"));
                podInfo.setPodId(rs.getString("pod_id"));
                podInfo.setPodFace(rs.getString("pod_face"));
                podInfo.setLocation(rs.getString("location"));
                podInfo.setReportTime(rs.getLong("report_time"));
                return podInfo;
            });
            
            log.debug("WCS DB에서 POD 데이터 {}개 조회 완료", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("WCS DB에서 POD 데이터 조회 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * WCS DB에서 최신 POD 데이터 10개를 조회
     * 
     * @return 최신 POD 데이터 10개 리스트
     */
    public List<PodInfo> getLatestPodData() {
        String sql = """
            SELECT TOP 10 uuid_no, pod_id, pod_face, location, report_time
            FROM pod_info 
            ORDER BY report_time DESC
            """;
        
        return wcsJdbcTemplate.query(sql, (rs, rowNum) -> {
            PodInfo podInfo = new PodInfo();
            podInfo.setUuidNo(rs.getString("uuid_no"));
            podInfo.setPodId(rs.getString("pod_id"));
            podInfo.setPodFace(rs.getString("pod_face"));
            podInfo.setLocation(rs.getString("location"));
            podInfo.setReportTime(rs.getLong("report_time"));
            return podInfo;
        });
    }
    
    /**
     * 특정 시간 이후의 POD 데이터를 조회
     * 
     * @param timestamp 기준 시간
     * @return 해당 시간 이후의 POD 데이터 리스트
     */
    public List<PodInfo> getPodDataAfterTimestamp(LocalDateTime timestamp) {
        // LocalDateTime을 시스템 기본 시간대의 Unix timestamp로 변환 (AgvDataService와 동일한 방식)
        long timestampMillis = timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        log.debug("Querying POD data after timestamp: {} ({} ms)", timestamp, timestampMillis);
        
        String sql = """
            SELECT uuid_no, pod_id, pod_face, location, report_time
            FROM pod_info 
            WHERE report_time > ? 
            ORDER BY report_time DESC
            """;
        
        return wcsJdbcTemplate.query(sql, (rs, rowNum) -> {
            PodInfo podInfo = new PodInfo();
            podInfo.setUuidNo(rs.getString("uuid_no"));
            podInfo.setPodId(rs.getString("pod_id"));
            podInfo.setPodFace(rs.getString("pod_face"));
            podInfo.setLocation(rs.getString("location"));
            podInfo.setReportTime(rs.getLong("report_time"));
            return podInfo;
        }, timestampMillis);
    }
    
    /**
     * PostgreSQL에 POD 데이터 저장
     * 
     * @param podInfo 저장할 POD 데이터
     * @return 저장 성공 여부
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public boolean savePodData(PodInfo podInfo) {
        // PostgreSQLDataService로 위임
        return postgresqlDataService.savePodData(podInfo);
    }
    
    /**
     * 배치로 POD 데이터 저장
     * 
     * @param podInfoList 저장할 POD 데이터 리스트
     * @return 성공적으로 저장된 레코드 수
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int savePodDataBatch(List<PodInfo> podInfoList) {
        // PostgreSQLDataService로 위임
        return postgresqlDataService.savePodDataBatch(podInfoList);
    }
    
    /**
     * WCS DB의 최신 타임스탬프를 조회
     * 
     * @return 최신 타임스탬프 (데이터가 없으면 1년 전)
     */
    public LocalDateTime getLatestTimestamp() {
        try {
            String sql = "SELECT MAX(report_time) as latest_timestamp FROM pod_info";
            
            Long timestamp = wcsJdbcTemplate.queryForObject(sql, Long.class);
            if (timestamp != null) {
                return java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
            } else {
                // 데이터가 없으면 1년 전 시간 반환 (AGV와 동일)
                log.info("POD 데이터가 없어 1년 전부터 처리하도록 설정");
                return LocalDateTime.now().minusYears(1);
            }
        } catch (Exception e) {
            log.error("Error getting latest timestamp from WCS DB: {}", e.getMessage(), e);
            // 오류 발생시 1년 전 시간 반환 (AGV와 동일)
            return LocalDateTime.now().minusYears(1);
        }
    }
    
    /**
     * WCS DB 연결 상태 확인
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