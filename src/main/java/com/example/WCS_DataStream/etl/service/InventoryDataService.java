package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.InventoryInfo;
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
 * 재고 정보 데이터 서비스
 * 
 * 재고 정보 테이블과 상호작용하는 서비스
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Service
public class InventoryDataService {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryDataService.class);
    
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
    
    public InventoryDataService(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
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
            
            log.info("재고 정보 서비스 데이터베이스 연결 초기화 완료: {}", databaseUrl);
            
        } catch (Exception e) {
            log.error("재고 정보 서비스 데이터베이스 연결 초기화 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * WCS DB에서 모든 재고 데이터를 조회
     * 
     * @return 모든 재고 데이터 리스트
     */
    public List<InventoryInfo> getAllInventoryData() {
        String sql = """
            SELECT uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, 
                   origin_order, status, report_time
            FROM inventory_info 
            ORDER BY report_time DESC
            """;
        
        return wcsJdbcTemplate.query(sql, (rs, rowNum) -> {
            InventoryInfo inventoryInfo = new InventoryInfo();
            inventoryInfo.setUuidNo(rs.getString("uuid_no"));
            inventoryInfo.setInventory(rs.getString("inventory"));
            inventoryInfo.setBatchNum(rs.getString("batch_num"));
            inventoryInfo.setUnitload(rs.getString("unitload"));
            inventoryInfo.setSku(rs.getString("sku"));
            inventoryInfo.setPreQty(rs.getInt("pre_qty"));
            inventoryInfo.setNewQty(rs.getInt("new_qty"));
            inventoryInfo.setOriginOrder(rs.getString("origin_order"));
            inventoryInfo.setStatus(rs.getInt("status"));
            inventoryInfo.setReportTime(rs.getLong("report_time"));
            return inventoryInfo;
        });
    }
    
    /**
     * WCS DB에서 최신 재고 데이터 10개를 조회
     * 
     * @return 최신 재고 데이터 10개 리스트
     */
    public List<InventoryInfo> getLatestInventoryData() {
        String sql = """
            SELECT TOP 10 uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, 
                   origin_order, status, report_time
            FROM inventory_info 
            ORDER BY report_time DESC
            """;
        
        return wcsJdbcTemplate.query(sql, (rs, rowNum) -> {
            InventoryInfo inventoryInfo = new InventoryInfo();
            inventoryInfo.setUuidNo(rs.getString("uuid_no"));
            inventoryInfo.setInventory(rs.getString("inventory"));
            inventoryInfo.setBatchNum(rs.getString("batch_num"));
            inventoryInfo.setUnitload(rs.getString("unitload"));
            inventoryInfo.setSku(rs.getString("sku"));
            inventoryInfo.setPreQty(rs.getInt("pre_qty"));
            inventoryInfo.setNewQty(rs.getInt("new_qty"));
            inventoryInfo.setOriginOrder(rs.getString("origin_order"));
            inventoryInfo.setStatus(rs.getInt("status"));
            inventoryInfo.setReportTime(rs.getLong("report_time"));
            return inventoryInfo;
        });
    }
    
    /**
     * 특정 시간 이후의 재고 데이터를 조회
     * 
     * @param timestamp 기준 시간
     * @return 해당 시간 이후의 재고 데이터 리스트
     */
    public List<InventoryInfo> getInventoryDataAfterTimestamp(LocalDateTime timestamp) {
        // LocalDateTime을 시스템 기본 시간대의 Unix timestamp로 변환 (AgvDataService와 동일한 방식)
        long timestampMillis = timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        log.debug("Querying inventory data after timestamp: {} ({} ms)", timestamp, timestampMillis);
        
        String sql = """
            SELECT uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, 
                   origin_order, status, report_time
            FROM inventory_info 
            WHERE report_time > ? 
            ORDER BY report_time DESC
            """;
        
        return wcsJdbcTemplate.query(sql, (rs, rowNum) -> {
            InventoryInfo inventoryInfo = new InventoryInfo();
            inventoryInfo.setUuidNo(rs.getString("uuid_no"));
            inventoryInfo.setInventory(rs.getString("inventory"));
            inventoryInfo.setBatchNum(rs.getString("batch_num"));
            inventoryInfo.setUnitload(rs.getString("unitload"));
            inventoryInfo.setSku(rs.getString("sku"));
            inventoryInfo.setPreQty(rs.getInt("pre_qty"));
            inventoryInfo.setNewQty(rs.getInt("new_qty"));
            inventoryInfo.setOriginOrder(rs.getString("origin_order"));
            inventoryInfo.setStatus(rs.getInt("status"));
            inventoryInfo.setReportTime(rs.getLong("report_time"));
            return inventoryInfo;
        }, timestampMillis);
    }
    
    /**
     * PostgreSQL에 재고 데이터 저장
     * 
     * @param inventoryInfo 저장할 재고 데이터
     * @return 저장 성공 여부
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public boolean saveInventoryData(InventoryInfo inventoryInfo) {
        try {
            String sql = """
                INSERT INTO inventory_info (
                    uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, 
                    origin_order, status, report_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            int result = postgresqlJdbcTemplate.update(sql,
                inventoryInfo.getUuidNo(),
                inventoryInfo.getInventory(),
                inventoryInfo.getBatchNum(),
                inventoryInfo.getUnitload(),
                inventoryInfo.getSku(),
                inventoryInfo.getPreQty(),
                inventoryInfo.getNewQty(),
                inventoryInfo.getOriginOrder(),
                inventoryInfo.getStatus(),
                inventoryInfo.getReportTime()
            );
            
            if (result > 0) {
                log.debug("PostgreSQL 재고 데이터 저장 성공: inventory={}, uuid_no={}", 
                    inventoryInfo.getInventory(), inventoryInfo.getUuidNo());
                return true;
            } else {
                log.warn("PostgreSQL 재고 데이터 저장 실패 (0 rows affected): inventory={}", 
                    inventoryInfo.getInventory());
                return false;
            }
            
        } catch (Exception e) {
            log.error("PostgreSQL 재고 데이터 저장 예외: inventory={}, error={}", 
                inventoryInfo.getInventory(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 배치로 재고 데이터 저장
     * 
     * @param inventoryInfoList 저장할 재고 데이터 리스트
     * @return 성공적으로 저장된 레코드 수
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int saveInventoryDataBatch(List<InventoryInfo> inventoryInfoList) {
        if (inventoryInfoList == null || inventoryInfoList.isEmpty()) {
            return 0;
        }
        
        try {
            String sql = """
                INSERT INTO inventory_info (
                    uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, 
                    origin_order, status, report_time
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            int totalUpdated = 0;
            for (InventoryInfo inventoryInfo : inventoryInfoList) {
                int result = postgresqlJdbcTemplate.update(sql,
                    inventoryInfo.getUuidNo(),
                    inventoryInfo.getInventory(),
                    inventoryInfo.getBatchNum(),
                    inventoryInfo.getUnitload(),
                    inventoryInfo.getSku(),
                    inventoryInfo.getPreQty(),
                    inventoryInfo.getNewQty(),
                    inventoryInfo.getOriginOrder(),
                    inventoryInfo.getStatus(),
                    inventoryInfo.getReportTime()
                );
                totalUpdated += result;
            }
            
            log.info("배치 재고 데이터 저장 완료: 총 {}개 중 {}개 저장", inventoryInfoList.size(), totalUpdated);
            return totalUpdated;
            
        } catch (Exception e) {
            log.error("배치 재고 데이터 저장 실패: error={}", e.getMessage(), e);
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
        String sql = "SELECT MAX(report_time) as latest_timestamp FROM inventory_info";
        
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