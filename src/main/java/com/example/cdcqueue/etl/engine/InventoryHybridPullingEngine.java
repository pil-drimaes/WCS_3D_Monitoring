package com.example.cdcqueue.etl.engine;

import com.example.cdcqueue.common.model.InventoryInfo;
import com.example.cdcqueue.etl.service.InventoryDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Inventory 데이터 전용 하이브리드 풀링 엔진
 * 
 * HybridPullingEngine의 구체적인 구현체로 Inventory 데이터에 특화된 로직을 제공합니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class InventoryHybridPullingEngine extends HybridPullingEngine<InventoryInfo> {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryHybridPullingEngine.class);
    
    private final InventoryDataService inventoryDataService;
    
    @Autowired
    public InventoryHybridPullingEngine(InventoryDataService inventoryDataService) {
        this.inventoryDataService = inventoryDataService;
    }
    
    @Override
    protected List<InventoryInfo> getChangedData() {
        LocalDateTime lastCheck = getLastCheckTime();
        return inventoryDataService.getInventoryDataAfterTimestamp(lastCheck);
    }
    
    @Override
    protected List<InventoryInfo> getAllData() {
        return inventoryDataService.getAllInventoryData();
    }
    
    @Override
    protected String getDataKey(InventoryInfo data) {
        return data.getUuidNo();
    }
    
    @Override
    protected boolean isSameData(InventoryInfo data1, InventoryInfo data2) {
        // reportTime이 다르면 다른 데이터로 인식 (시간 기반 변경 감지)
        if (!Objects.equals(data1.getReportTime(), data2.getReportTime())) {
            return false;
        }
        
        // 다른 중요 필드들도 비교
        return Objects.equals(data1.getUuidNo(), data2.getUuidNo()) &&
               Objects.equals(data1.getInventory(), data2.getInventory()) &&
               Objects.equals(data1.getBatchNum(), data2.getBatchNum()) &&
               Objects.equals(data1.getUnitload(), data2.getUnitload()) &&
               Objects.equals(data1.getSku(), data2.getSku()) &&
               Objects.equals(data1.getPreQty(), data2.getPreQty()) &&
               Objects.equals(data1.getNewQty(), data2.getNewQty()) &&
               Objects.equals(data1.getStatus(), data2.getStatus());
    }
    
    @Override
    protected boolean isConnected() {
        return inventoryDataService.isConnected();
    }
    
    /**
     * 마지막 체크 시간 반환 (private 메서드를 protected로 오버라이드)
     */
    protected LocalDateTime getLastCheckTime() {
        long lastPull = getLastPullTime();
        if (lastPull == 0) {
            return LocalDateTime.now().minusHours(1);
        }
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastPull), 
            java.time.ZoneId.systemDefault()
        );
    }
} 