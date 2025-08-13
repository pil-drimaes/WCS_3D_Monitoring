package com.example.cdcqueue.etl.service;

import com.example.cdcqueue.common.model.PodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
        private JdbcTemplate wcsJdbcTemplate;
    private final JdbcTemplate postgresqlJdbcTemplate;
    
    @Value("${etl.database.url:jdbc:sqlserver://localhost:1433;databaseName=cdc_test;encrypt=true;trustServerCertificate=true}")
    private String databaseUrl;
    
    @Value("${etl.database.username:sa}")
    private String username;
    
    @Value("${etl.database.password:nice2025!}")
    private String password;
    
    @Value("${etl.database.driver:com.microsoft.sqlserver.jdbc.SQLServerDriver}")
    private String driverClassName;
    
    public PodDataService(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }
    
    /**
     * 데이터베이스 연결 초기화
     */
    @PostConstruct
    public void initialize() {
        try {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(driverClassName);
            dataSource.setUrl(databaseUrl);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            
            this.wcsJdbcTemplate = new JdbcTemplate(dataSource);
            
            log.info("POD 정보 서비스 데이터베이스 연결 초기화 완료: {}", databaseUrl);
            
        } catch (Exception e) {
            log.error("POD 정보 서비스 데이터베이스 연결 초기화 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * WCS DB에서 모든 POD 데이터를 조회
     * 
     * @return 모든 POD 데이터 리스트
     */
    public List<PodInfo> getAllPodData() {
        String sql = """
            SELECT uuid_no, pod_id, pod_face, location, report_time
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
        try {
            String sql = """
                INSERT INTO pod_info (
                    uuid_no, pod_id, pod_face, location, report_time
                ) VALUES (?, ?, ?, ?, ?)
                """;
            
            int result = postgresqlJdbcTemplate.update(sql,
                podInfo.getUuidNo(),
                podInfo.getPodId(),
                podInfo.getPodFace(),
                podInfo.getLocation(),
                podInfo.getReportTime()
            );
            
            if (result > 0) {
                log.debug("PostgreSQL POD 데이터 저장 성공: pod_id={}, uuid_no={}", 
                    podInfo.getPodId(), podInfo.getUuidNo());
                return true;
            } else {
                log.warn("PostgreSQL POD 데이터 저장 실패 (0 rows affected): pod_id={}", 
                    podInfo.getPodId());
                return false;
            }
            
        } catch (Exception e) {
            log.error("PostgreSQL POD 데이터 저장 예외: pod_id={}, error={}", 
                podInfo.getPodId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 배치로 POD 데이터 저장
     * 
     * @param podInfoList 저장할 POD 데이터 리스트
     * @return 성공적으로 저장된 레코드 수
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int savePodDataBatch(List<PodInfo> podInfoList) {
        if (podInfoList == null || podInfoList.isEmpty()) {
            return 0;
        }
        
        try {
            String sql = """
                INSERT INTO pod_info (
                    uuid_no, pod_id, pod_face, location, report_time
                ) VALUES (?, ?, ?, ?, ?)
                """;
            
            int totalUpdated = 0;
            for (PodInfo podInfo : podInfoList) {
                int result = postgresqlJdbcTemplate.update(sql,
                    podInfo.getUuidNo(),
                    podInfo.getPodId(),
                    podInfo.getPodFace(),
                    podInfo.getLocation(),
                    podInfo.getReportTime()
                );
                totalUpdated += result;
            }
            
            log.info("배치 POD 데이터 저장 완료: 총 {}개 중 {}개 저장", podInfoList.size(), totalUpdated);
            return totalUpdated;
            
        } catch (Exception e) {
            log.error("배치 POD 데이터 저장 실패: error={}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * WCS DB의 최신 타임스탬프를 조회
     * 
     * @return 최신 타임스탬프 (데이터가 없으면 1시간 전)
     */
    public LocalDateTime getLatestTimestamp() {
        // WCS DB에서 최신 타임스탬프 조회 (PostgreSQL이 아닌)
        String sql = "SELECT MAX(report_time) as latest_timestamp FROM pod_info";
        
        try {
            Long timestamp = wcsJdbcTemplate.queryForObject(sql, Long.class);
            if (timestamp != null) {
                return LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);
            } else {
                // 데이터가 없으면 1시간 전 시간 반환
                return LocalDateTime.now().minusHours(1);
            }
        } catch (Exception e) {
            log.error("Error getting latest timestamp from WCS DB: {}", e.getMessage(), e);
            // 오류 발생시 1시간 전 시간 반환
            return LocalDateTime.now().minusHours(1);
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