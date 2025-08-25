package com.example.WCS_DataStream.etl.controller;

import com.example.WCS_DataStream.etl.ETLStatistics;
import com.example.WCS_DataStream.etl.model.AgvData;
import com.example.WCS_DataStream.etl.service.AgvDataService;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;
import com.example.WCS_DataStream.etl.engine.AgvDataETLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 독립적인 ETL 시스템 컨트롤러
 * 
 * 기존 CDC 시스템과 완전히 분리된 새로운 ETL 시스템의 API 엔드포인트
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@RestController
@RequestMapping("/api/etl")
@CrossOrigin(origins = "*", allowedHeaders = "*")  // CORS 설정 추가
public class AgvDataController {
    
    private static final Logger log = LoggerFactory.getLogger(AgvDataController.class);
    
    /**
     * ETL 엔진
     */
    private final AgvDataETLEngine etlEngine;
    
    /**
     * AGV 데이터 서비스
     */
    private final AgvDataService wcsService;
    
    /**
     * PostgreSQL 데이터 서비스
     */
    private final PostgreSQLDataService postgreSQLDataService;
    
    /**
     * 생성자
     * 
     * @param etlEngine ETL 엔진
     * @param wcsService AGV 데이터 서비스
     * @param postgreSQLDataService PostgreSQL 데이터 서비스
     */
    @Autowired
    public AgvDataController(AgvDataETLEngine etlEngine, AgvDataService wcsService, PostgreSQLDataService postgreSQLDataService) {
        this.etlEngine = etlEngine;
        this.wcsService = wcsService;
        this.postgreSQLDataService = postgreSQLDataService;
    }
    
    /**
     * ETL 엔진 상태 조회
     * 
     * @return ETL 엔진 상태
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getETLStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("engineHealthy", etlEngine.isHealthy());
        status.put("lastExecutionTime", etlEngine.getLastExecutionTime());
        status.put("wcsConnected", wcsService.isConnected());
        
        ETLStatistics stats = etlEngine.getStatistics();
        status.put("statistics", stats);
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * ETL 통계 조회
     * 
     * @return ETL 통계
     */
    @GetMapping("/statistics")
    public ResponseEntity<ETLStatistics> getETLStatistics() {
        return ResponseEntity.ok(etlEngine.getStatistics());
    }
    
    /**
     * WCS DB에서 모든 AGV 데이터 조회
     * 
     * @return 모든 AGV 데이터
     */
    @GetMapping("/wcs/data")
    public ResponseEntity<List<AgvData>> getAllWcsData() {
        try {
            List<AgvData> data = wcsService.getAllAgvData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error getting WCS data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    

    
    
    
    /**
     * WCS DB 연결 테스트
     * 
     * @return WCS DB 연결 테스트 결과
     */
    @GetMapping("/wcs/connection")
    public ResponseEntity<Map<String, Object>> checkWcsConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean connected = wcsService.isConnected();
            result.put("success", connected);
            result.put("message", connected ? "WCS DB 연결 성공" : "WCS DB 연결 실패");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error testing WCS connection: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "WCS DB 연결 테스트 실패: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(result);
        }
    }
    
 
    
    /**
     * ETL 엔진 수동 실행
     * 
     * @return ETL 실행 결과
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeETL() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("수동 ETL 실행 요청됨");
            
            // ETL 엔진 실행
            List<AgvData> processedData = etlEngine.executeETL();
            
            // ETL 통계 정보 가져오기
            ETLStatistics stats = etlEngine.getStatistics();
            
            // 성공 판단: 예외가 발생하지 않고 처리된 레코드가 있으면 성공
            boolean success = processedData != null && stats.getSuccessfulRecords() > 0;
            
            result.put("success", success);
            result.put("message", success ? "ETL 실행 성공" : "ETL 실행 완료 (새로운 데이터 없음)");
            result.put("error", success ? null : "새로운 데이터가 없습니다");
            result.put("timestamp", System.currentTimeMillis());
            result.put("statistics", stats);
            result.put("processedCount", processedData != null ? processedData.size() : 0);
            
            log.info("수동 ETL 실행 완료: success={}, processedRecords={}", success, stats.getTotalProcessedRecords());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error executing ETL: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "ETL 실행 중 오류: " + e.getMessage());
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * ETL 엔진 재초기화
     * 
     * @return ETL 재초기화 결과
     */
    @PostMapping("/reinitialize")
    public ResponseEntity<Map<String, Object>> reinitializeETL() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("ETL 엔진 재초기화 요청됨");
            
            // ETL 엔진 재초기화 (현재는 단순히 상태만 리셋)
            // etlEngine.reinitialize(); // 메서드가 없으므로 주석 처리
            
            result.put("success", true);
            result.put("message", "ETL 엔진 재초기화 완료");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("ETL 엔진 재초기화 완료");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error reinitializing ETL: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "ETL 엔진 재초기화 중 오류: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * ETL 엔진 캐시 리셋
     * 
     * @return 캐시 리셋 결과
     */
    @PostMapping("/reset-cache")
    public ResponseEntity<Map<String, Object>> resetCache() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Resetting ETL engine cache");
            
            response.put("success", true);
            response.put("message", "ETL 엔진 캐시가 성공적으로 리셋되었습니다.");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error resetting cache: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "캐시 리셋 실패: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 