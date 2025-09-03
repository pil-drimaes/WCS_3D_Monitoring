package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.InventoryInfo;
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
    
    private final JdbcTemplate wcsJdbcTemplate;
    private PostgreSQLDataService postgresqlDataService;
    
    @Autowired
    public InventoryDataService(@Qualifier("wcsJdbcTemplate") JdbcTemplate wcsJdbcTemplate,
                                PostgreSQLDataService postgresqlDataService) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
        this.postgresqlDataService = postgresqlDataService;
    }
        
    
    /**
     * WCS DB에서 모든 재고 데이터를 조회
     * 
     * @return 모든 재고 데이터 리스트
     */
    public List<InventoryInfo> getAllInventoryData() {
        try {
            String sql = """
                SELECT uuid_no, inventory, batch_num, unitload, sku, pre_qty, new_qty, 
                       origin_order, status, report_time
                FROM inventory_info 
                ORDER BY report_time DESC
                """;
            
            List<InventoryInfo> result = wcsJdbcTemplate.query(sql, (rs, rowNum) -> {
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
            
            log.debug("WCS DB에서 재고 데이터 {}개 조회 완료", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("WCS DB에서 재고 데이터 조회 실패: {}", e.getMessage(), e);
            return List.of();
        }
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
        // PostgreSQLDataService로 위임
        return postgresqlDataService.saveInventoryData(inventoryInfo);
    }
    
    /**
     * 배치로 재고 데이터 저장
     * 
     * @param inventoryInfoList 저장할 재고 데이터 리스트
     * @return 성공적으로 저장된 레코드 수
     */
    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int saveInventoryDataBatch(List<InventoryInfo> inventoryInfoList) {
        // PostgreSQLDataService로 위임
        return postgresqlDataService.saveInventoryDataBatch(inventoryInfoList);
    }
    
    /**
     * WCS DB의 최신 타임스탬프를 조회
     * 
     * @return 최신 타임스탬프 (데이터가 없으면 1년 전)
     */
    public LocalDateTime getLatestTimestamp() {
        try {
            String sql = "SELECT MAX(report_time) as latest_timestamp FROM inventory_info";
            
            Long timestamp = wcsJdbcTemplate.queryForObject(sql, Long.class);
            if (timestamp != null) {
                return java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
            } else {
                // 데이터가 없으면 1년 전 시간 반환 (AGV와 동일)
                log.info("재고 데이터가 없어 1년 전부터 처리하도록 설정");
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