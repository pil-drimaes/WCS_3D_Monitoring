package com.example.cdcqueue.etl.controller;

import com.example.cdcqueue.common.model.AgvData;
import com.example.cdcqueue.common.model.CdcEvent;
import com.example.cdcqueue.common.queue.EventQueue;
import com.example.cdcqueue.etl.AgvDataETLEngine;
import com.example.cdcqueue.etl.InventoryDataETLEngine;
import com.example.cdcqueue.etl.PodDataETLEngine;
import com.example.cdcqueue.etl.engine.AgvHybridPullingEngine;
import com.example.cdcqueue.etl.service.AgvDataService;
import com.example.cdcqueue.etl.service.PostgreSQLDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ETL 엔진 테스트 및 모니터링용 컨트롤러
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    
    private final EventQueue queue;
    private final AgvDataETLEngine agvDataETLEngine;
    private final InventoryDataETLEngine inventoryDataETLEngine;
    private final PodDataETLEngine podDataETLEngine;
    private final AgvHybridPullingEngine agvHybridPullingEngine;
    private final AgvDataService agvDataService;
    private final PostgreSQLDataService postgreSQLDataService;
    
    @Autowired
    public TestController(EventQueue queue,
                         AgvDataETLEngine agvDataETLEngine,
                         InventoryDataETLEngine inventoryDataETLEngine,
                         PodDataETLEngine podDataETLEngine,
                         AgvHybridPullingEngine agvHybridPullingEngine,
                         AgvDataService agvDataService,
                         PostgreSQLDataService postgreSQLDataService) {
        this.queue = queue;
        this.agvDataETLEngine = agvDataETLEngine;
        this.inventoryDataETLEngine = inventoryDataETLEngine;
        this.podDataETLEngine = podDataETLEngine;
        this.agvHybridPullingEngine = agvHybridPullingEngine;
        this.agvDataService = agvDataService;
        this.postgreSQLDataService = postgreSQLDataService;
    }
    
    /**
     * 모든 AGV 데이터 조회
     * 
     * 데이터베이스의 모든 AGV 데이터를 최신순으로 조회합니다.
     * 
     * @return 모든 AGV 데이터 리스트 (JSON)
     */
    @GetMapping("/agv-data")
    public List<AgvData> getAllAgvData() {
        return agvDataService.getAllAgvData();
    }

    /**
     * 최신 AGV 데이터 조회
     * 
     * 데이터베이스의 최신 AGV 데이터 10개를 조회합니다.
     * 웹 인터페이스에서 주로 사용됩니다.
     * 
     * @return 최신 AGV 데이터 10개 리스트 (JSON)
     */
    @GetMapping("/agv-data/latest")
    public List<AgvData> getLatestAgvData() {
        return agvDataService.getLatestAgvData();
    }

    /**
     * 테이블 구조 확인
     * 
     * robot_info 테이블의 컬럼 구조를 확인합니다.
     * 개발 및 디버깅 목적으로 사용됩니다.
     * 
     * @return 테이블 구조 정보 (컬럼명, 데이터타입)
     */
    @GetMapping("/table-structure")
    public List<Map<String, Object>> getTableStructure() {
        return agvDataService.getTableStructure();
    }

    /**
     * 테스트 이벤트 생성
     * 
     * 수동으로 테스트 이벤트를 생성하여 이벤트 큐에 추가합니다.
     * WebSocket 푸시 기능을 테스트할 때 사용됩니다.
     * 
     * @return 이벤트 생성 결과 메시지
     */
    @GetMapping("/test-event")
    public String addTestEvent() {
        // 테스트용 CDC 이벤트 생성
        CdcEvent event = new CdcEvent(
                UUID.randomUUID().toString(),  // 고유 ID
                "MANUAL",                      // 이벤트 타입
                "{\"manual\": \"triggered\"}"  // 테스트 페이로드
        );
        
        // 이벤트 큐에 추가 (3초 후 WebSocket으로 전송됨)
        queue.add(event);
        
        return "Event added!";
    }
    
    /**
     * ETL 엔진 상태 확인
     */
    @GetMapping("/etl/status")
    public ResponseEntity<Map<String, Object>> getETLStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("etlEngineHealthy", agvDataETLEngine.isHealthy());
            status.put("etlEngineStatus", "RUNNING"); // getStatus() 메서드가 없으므로 하드코딩
            status.put("lastExecutionTime", agvDataETLEngine.getLastExecutionTime());
            status.put("etlStatistics", agvDataETLEngine.getStatistics());
            
            status.put("pullingEngineHealthy", agvHybridPullingEngine.isHealthy());
            status.put("lastPullTime", agvHybridPullingEngine.getLastPullTime());
            
            status.put("mssqlConnected", agvDataService.isConnected());
            status.put("postgresqlConnected", postgreSQLDataService.isConnected());
            status.put("postgresqlTableExists", postgreSQLDataService.isTableExists());
            
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Error getting ETL status: {}", e.getMessage(), e);
            status.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(status);
        }
    }
    
    /**
     * ETL 엔진 수동 실행
     */
    @PostMapping("/etl/execute")
    public ResponseEntity<Map<String, Object>> executeETL() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Manual ETL execution requested");
            
            // ETL 실행
            var processedData = agvDataETLEngine.executeETL();
            
            result.put("success", true);
            result.put("processedCount", processedData.size());
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("Manual ETL execution completed: {} records processed", processedData.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in manual ETL execution: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 풀링 엔진 캐시 리셋
     */
    @PostMapping("/etl/reset-cache")
    public ResponseEntity<Map<String, Object>> resetCache() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Cache reset requested");
            
            // 캐시 리셋
            agvHybridPullingEngine.resetCache();
            
            result.put("success", true);
            result.put("message", "Cache reset successfully");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("Cache reset completed");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in cache reset: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 모든 ETL 엔진 캐시 리셋
     */
    @PostMapping("/etl/reset-all-cache")
    public ResponseEntity<Map<String, Object>> resetAllCache() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("All ETL engine cache reset requested");
            
            // 모든 ETL 엔진 캐시 리셋
            agvHybridPullingEngine.resetCache();
            inventoryDataETLEngine.resetCache();
            podDataETLEngine.resetCache();
            
            result.put("success", true);
            result.put("message", "All ETL engine caches reset successfully");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("All ETL engine caches reset completed");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in all cache reset: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * Inventory ETL 엔진 실행
     */
    @PostMapping("/etl/execute-inventory")
    public ResponseEntity<Map<String, Object>> executeInventoryETL() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Inventory ETL execution requested");
            
            // Inventory ETL 실행
            var processedData = inventoryDataETLEngine.executeETL();
            
            result.put("success", true);
            result.put("processedCount", processedData.size());
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("Inventory ETL execution completed: {} records processed", processedData.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in Inventory ETL execution: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * Pod ETL 엔진 실행
     */
    @PostMapping("/etl/execute-pod")
    public ResponseEntity<Map<String, Object>> executePodETL() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Pod ETL execution requested");
            
            // Pod ETL 실행 (processETL 메서드 사용)
            int processedCount = podDataETLEngine.processETL(LocalDateTime.now().minusYears(1));
            
            result.put("success", true);
            result.put("processedCount", processedCount);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("Pod ETL execution completed: {} records processed", processedCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in Pod ETL execution: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 모든 ETL 엔진 실행
     */
    @PostMapping("/etl/execute-all")
    public ResponseEntity<Map<String, Object>> executeAllETL() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("All ETL execution requested");
            
            // 모든 ETL 엔진 실행
            var agvData = agvDataETLEngine.executeETL();
            var inventoryData = inventoryDataETLEngine.executeETL();
            int podCount = podDataETLEngine.processETL(LocalDateTime.now().minusYears(1));
            
            int totalProcessed = agvData.size() + inventoryData.size() + podCount;
            
            result.put("success", true);
            result.put("agvProcessedCount", agvData.size());
            result.put("inventoryProcessedCount", inventoryData.size());
            result.put("podProcessedCount", podCount);
            result.put("totalProcessedCount", totalProcessed);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("All ETL execution completed: AGV={}, Inventory={}, Pod={}, Total={}", 
                agvData.size(), inventoryData.size(), podCount, totalProcessed);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in all ETL execution: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * MSSQL 데이터 확인
     */
    @GetMapping("/mssql/data")
    public ResponseEntity<Map<String, Object>> getMSSQLData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!agvDataService.isConnected()) {
                result.put("success", false);
                result.put("error", "MSSQL not connected");
                return ResponseEntity.ok(result);
            }
            
            // 최신 데이터 조회
            var latestData = agvDataService.getLatestAgvData();
            var allData = agvDataService.getAllAgvData();
            
            result.put("success", true);
            result.put("latestDataCount", latestData.size());
            result.put("totalDataCount", allData.size());
            result.put("latestData", latestData);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting MSSQL data: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * PostgreSQL 데이터 확인
     */
    @GetMapping("/postgresql/data")
    public ResponseEntity<Map<String, Object>> getPostgreSQLData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!postgreSQLDataService.isConnected()) {
                result.put("success", false);
                result.put("error", "PostgreSQL not connected");
                return ResponseEntity.ok(result);
            }
            
            if (!postgreSQLDataService.isTableExists()) {
                result.put("success", false);
                result.put("error", "robot_info table does not exist");
                return ResponseEntity.ok(result);
            }
            
            // PostgreSQL에서 데이터 개수 확인 (간단한 쿼리)
            int count = postgreSQLDataService.getRobotInfoCount();
            
            result.put("success", true);
            result.put("robotInfoCount", count);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting PostgreSQL data: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
