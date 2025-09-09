package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.engine.InventoryDataETLEngine;
import com.example.WCS_DataStream.etl.model.InventoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;

/**
 * 재고 정보 ETL 스케줄러
 * 
 * 재고 정보 데이터의 ETL 처리를 독립적으로 스케줄링하는 스케줄러
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class InventoryDataScheduler extends BaseETLScheduler<InventoryInfo> {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryDataScheduler.class);
    
    private final InventoryDataETLEngine etlEngine;
    
    /**
     * 처리된 데이터 ID 추적 (중복 방지)
     */
    private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * 초기 데이터 처리 완료 여부
     */
    private boolean initialDataProcessed = false;
    
    @Autowired
    public InventoryDataScheduler(InventoryDataETLEngine etlEngine, PostgreSQLDataService postgreSQLDataService) {
        super(postgreSQLDataService);  // 부모 생성자 호출
        this.etlEngine = etlEngine;
    }


    @Override
    @org.springframework.scheduling.annotation.Scheduled(
        fixedRateString = "${etl.inventory.interval}",
        initialDelayString = "${etl.inventory.initialDelay}"
    )
    public void executeETLProcess() {
        super.executeETLProcess();
    }
    
    @Override
    protected ETLEngine<InventoryInfo> getETLEngine() {
        return etlEngine;
    }
    
    @Override
    protected String getSchedulerName() {
        return "Inventory Data";
    }
    
    /**
     * 초기 데이터 처리 (애플리케이션 시작 시 한 번만)
     */
    @Override
    protected void processInitialData() {
        if (initialDataProcessed) return;
        try {
            etlEngine.resetCache();
            List<InventoryInfo> initialData = etlEngine.executeFullLoad(); 
            for (InventoryInfo d : initialData) {
                processedIds.add(d.getUuidNo() + "_" + d.getReportTime());
            }
            initialDataProcessed = true;
        } catch (Exception e) {
            log.error("재고 초기 데이터 처리 오류: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void processIncrementalData() {
        try {
            List<InventoryInfo> processedData = etlEngine.executeETL(); 
            int newDataCount = 0;
            for (InventoryInfo d : processedData) {
                String id = d.getUuidNo() + "_" + d.getReportTime();
                if (!processedIds.contains(id)) {
                    processedIds.add(id);
                    newDataCount++;
                }
            }
            if (processedIds.size() > 10000) processedIds.clear();
            if (newDataCount > 0) {
                log.info("Processed {} new inventory data records", newDataCount);
            }
        } catch (Exception e) {
            log.error("재고 증분 데이터 처리 오류: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 스케줄러 캐시 초기화
     */
    @Override
    public void clearSchedulerCache() {
        processedIds.clear();
        lastProcessedTime.set(null);
        initialDataProcessed = false;
        etlEngine.resetCache();
        log.info("재고 정보 스케줄러 캐시 및 엔진 캐시 초기화 완료 (REDIS)");
    }
    

} 