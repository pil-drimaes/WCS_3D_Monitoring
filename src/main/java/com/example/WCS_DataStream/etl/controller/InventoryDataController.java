package com.example.WCS_DataStream.etl.controller;

import com.example.WCS_DataStream.etl.engine.InventoryDataETLEngine;
import com.example.WCS_DataStream.etl.model.InventoryInfo;
import com.example.WCS_DataStream.etl.service.InventoryDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 재고 정보 컨트롤러
 * 
 * 재고 정보 관련 API 엔드포인트를 제공하는 컨트롤러
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryDataController {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryDataController.class);
    
    private final InventoryDataService inventoryDataService;
    private final InventoryDataETLEngine inventoryDataETLEngine;
    
    public InventoryDataController(InventoryDataService inventoryDataService,
                                  InventoryDataETLEngine inventoryDataETLEngine) {
        this.inventoryDataService = inventoryDataService;
        this.inventoryDataETLEngine = inventoryDataETLEngine;
    }
    
    /**
     * 모든 재고 데이터 조회
     * 
     * @return 재고 데이터 리스트
     */
    @GetMapping("/all")
    public ResponseEntity<List<InventoryInfo>> getAllInventoryData() {
        try {
            List<InventoryInfo> inventoryDataList = inventoryDataService.getAllInventoryData();
            log.info("재고 데이터 조회 완료: {}개", inventoryDataList.size());
            return ResponseEntity.ok(inventoryDataList);
        } catch (Exception e) {
            log.error("재고 데이터 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    
    /**
     * 특정 시간 이후의 재고 데이터 조회
     * 
     * @param timestamp 기준 시간 (ISO 8601 형식)
     * @return 기준 시간 이후의 재고 데이터 리스트
     */
    @GetMapping("/after")
    public ResponseEntity<List<InventoryInfo>> getInventoryDataAfterTimestamp(
            @RequestParam("timestamp") String timestamp) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp);
            List<InventoryInfo> inventoryDataList = inventoryDataService.getInventoryDataAfterTimestamp(dateTime);
            log.info("재고 데이터 조회 완료 ({} 이후): {}개", timestamp, inventoryDataList.size());
            return ResponseEntity.ok(inventoryDataList);
        } catch (Exception e) {
            log.error("재고 데이터 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * ETL 프로세스 수동 실행
     * 
     * @param lastProcessedTime 마지막 처리 시간 (ISO 8601 형식, 선택사항)
     * @return ETL 처리 결과
     */
    @PostMapping("/etl/run")
    public ResponseEntity<Map<String, Object>> runETL(
            @RequestParam(value = "lastProcessedTime", required = false) String lastProcessedTime) {
        try {
            LocalDateTime timestamp = lastProcessedTime != null ? 
                LocalDateTime.parse(lastProcessedTime) : 
                inventoryDataETLEngine.getLatestTimestamp();
            
            int processedCount = inventoryDataETLEngine.processETL(timestamp);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("processedCount", processedCount);
            result.put("lastProcessedTime", timestamp.toString());
            result.put("message", "재고 정보 ETL 프로세스가 성공적으로 실행되었습니다.");
            
            log.info("재고 정보 ETL 프로세스 실행 완료: {}개 처리", processedCount);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("재고 정보 ETL 프로세스 실행 실패: {}", e.getMessage(), e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "재고 정보 ETL 프로세스 실행 중 오류가 발생했습니다.");
            
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 연결 상태 확인
     * 
     * @return 연결 상태 정보
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            boolean isConnected = inventoryDataETLEngine.isConnected();
            LocalDateTime latestTimestamp = inventoryDataETLEngine.getLatestTimestamp();
            
            Map<String, Object> status = new HashMap<>();
            status.put("connected", isConnected);
            status.put("latestTimestamp", latestTimestamp.toString());
            status.put("service", "InventoryDataService");
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("재고 정보 서비스 상태 확인 실패: {}", e.getMessage(), e);
            
            Map<String, Object> status = new HashMap<>();
            status.put("connected", false);
            status.put("error", e.getMessage());
            status.put("service", "InventoryDataService");
            
            return ResponseEntity.ok(status);
        }
    }
    


    @PostMapping("/reset-cache")
    public ResponseEntity<Map<String, Object>> resetCache() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Resetting Inventory ETL engine cache");
            inventoryDataETLEngine.clearCache();
            response.put("success", true);
            response.put("message", "Inventory ETL 엔진 캐시가 성공적으로 리셋되었습니다.");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resetting Inventory engine cache: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Inventory 캐시 리셋 실패: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 