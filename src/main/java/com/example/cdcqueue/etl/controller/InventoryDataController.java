package com.example.cdcqueue.etl.controller;

import com.example.cdcqueue.common.model.InventoryInfo;
import com.example.cdcqueue.etl.InventoryDataETLEngine;
import com.example.cdcqueue.etl.service.InventoryDataService;
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
     * 최신 재고 데이터 조회
     * 
     * @return 최신 재고 데이터 리스트
     */
    @GetMapping("/latest")
    public ResponseEntity<List<InventoryInfo>> getLatestInventoryData() {
        try {
            List<InventoryInfo> inventoryDataList = inventoryDataService.getLatestInventoryData();
            log.info("최신 재고 데이터 조회 완료: {}개", inventoryDataList.size());
            return ResponseEntity.ok(inventoryDataList);
        } catch (Exception e) {
            log.error("최신 재고 데이터 조회 실패: {}", e.getMessage(), e);
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
    
    /**
     * 특정 재고 정보 조회
     * 
     * @param inventory 재고번호
     * @return 재고 정보
     */
    @GetMapping("/{inventory}")
    public ResponseEntity<InventoryInfo> getInventoryByInventory(@PathVariable String inventory) {
        try {
            // 실제 구현에서는 inventory 번호로 조회하는 메서드가 필요합니다
            List<InventoryInfo> allData = inventoryDataService.getAllInventoryData();
            InventoryInfo result = allData.stream()
                .filter(data -> inventory.equals(data.getInventory()))
                .findFirst()
                .orElse(null);
            
            if (result != null) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("재고 정보 조회 실패: inventory={}, error={}", inventory, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 