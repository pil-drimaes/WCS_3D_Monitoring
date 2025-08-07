package com.example.cdcqueue.cdc.controller;

import com.example.cdcqueue.etl.AgvDataETLEngine;
import com.example.cdcqueue.etl.ETLStatistics;
import com.example.cdcqueue.common.model.AgvData;
import com.example.cdcqueue.cdc.service.WcsDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ETL 모듈 전용 컨트롤러
 * 
 * ETL 관련 API 엔드포인트를 제공합니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
// @RestController  // IndependentETLController로 대체되어 비활성화
// @RequestMapping("/api/etl")
public class ETLController {
    
    /**
     * ETL 엔진
     */
    private final AgvDataETLEngine etlEngine;
    
    /**
     * WCS DB 서비스
     */
    private final WcsDatabaseService wcsDatabaseService;
    
    /**
     * 생성자
     * 
     * @param etlEngine ETL 엔진
     * @param wcsDatabaseService WCS DB 서비스
     */
    @Autowired
    public ETLController(AgvDataETLEngine etlEngine, WcsDatabaseService wcsDatabaseService) {
        this.etlEngine = etlEngine;
        this.wcsDatabaseService = wcsDatabaseService;
    }
    
    /**
     * ETL 엔진 상태 확인
     * 
     * @return ETL 엔진 상태 정보
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getETLStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("healthy", etlEngine.isHealthy());
        status.put("lastExecutionTime", etlEngine.getLastExecutionTime());
        status.put("wcsDbConnected", wcsDatabaseService.isConnected());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * ETL 통계 정보 조회
     * 
     * @return ETL 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<ETLStatistics> getETLStatistics() {
        ETLStatistics statistics = etlEngine.getStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * WCS DB에서 최신 AGV 데이터 조회
     * 
     * @return 최신 AGV 데이터 리스트
     */
    @GetMapping("/wcs/latest")
    public ResponseEntity<List<AgvData>> getLatestWcsData() {
        try {
            List<AgvData> data = wcsDatabaseService.getLatestAgvData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * WCS DB에서 모든 AGV 데이터 조회
     * 
     * @return 모든 AGV 데이터 리스트
     */
    @GetMapping("/wcs/all")
    public ResponseEntity<List<AgvData>> getAllWcsData() {
        try {
            List<AgvData> data = wcsDatabaseService.getAllAgvData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * WCS DB 테이블 구조 조회
     * 
     * @return 테이블 구조 정보
     */
    @GetMapping("/wcs/structure")
    public ResponseEntity<List<Map<String, Object>>> getWcsTableStructure() {
        try {
            List<Map<String, Object>> structure = wcsDatabaseService.getTableStructure();
            return ResponseEntity.ok(structure);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * WCS DB 연결 상태 확인
     * 
     * @return 연결 상태
     */
    @GetMapping("/wcs/connection")
    public ResponseEntity<Map<String, Object>> checkWcsConnection() {
        Map<String, Object> result = new HashMap<>();
        boolean connected = wcsDatabaseService.isConnected();
        
        result.put("connected", connected);
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 수동 ETL 실행
     * 
     * @return 실행 결과
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeETL() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<AgvData> processedData = etlEngine.executeETL();
            
            result.put("success", true);
            result.put("processedCount", processedData.size());
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(result);
        }
    }
} 