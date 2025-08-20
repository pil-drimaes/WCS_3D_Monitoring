package com.example.WCS_DataStream.etl.controller;

import com.example.WCS_DataStream.etl.engine.PodDataETLEngine;
import com.example.WCS_DataStream.etl.model.PodInfo;
import com.example.WCS_DataStream.etl.service.PodDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POD 정보 컨트롤러
 * 
 * POD 정보 관련 API 엔드포인트를 제공하는 컨트롤러
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@RestController
@RequestMapping("/api/pod")
public class PodDataController {
    
    private static final Logger log = LoggerFactory.getLogger(PodDataController.class);
    
    private final PodDataService podDataService;
    private final PodDataETLEngine podDataETLEngine;
    
    public PodDataController(PodDataService podDataService,
                            PodDataETLEngine podDataETLEngine) {
        this.podDataService = podDataService;
        this.podDataETLEngine = podDataETLEngine;
    }
    
    /**
     * 모든 POD 데이터 조회
     * 
     * @return POD 데이터 리스트
     */
    @GetMapping("/all")
    public ResponseEntity<List<PodInfo>> getAllPodData() {
        try {
            List<PodInfo> podDataList = podDataService.getAllPodData();
            log.info("POD 데이터 조회 완료: {}개", podDataList.size());
            return ResponseEntity.ok(podDataList);
        } catch (Exception e) {
            log.error("POD 데이터 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 최신 POD 데이터 조회
     * 
     * @return 최신 POD 데이터 리스트
     */
    @GetMapping("/latest")
    public ResponseEntity<List<PodInfo>> getLatestPodData() {
        try {
            List<PodInfo> podDataList = podDataService.getLatestPodData();
            log.info("최신 POD 데이터 조회 완료: {}개", podDataList.size());
            return ResponseEntity.ok(podDataList);
        } catch (Exception e) {
            log.error("최신 POD 데이터 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 특정 시간 이후의 POD 데이터 조회
     * 
     * @param timestamp 기준 시간 (ISO 8601 형식)
     * @return 기준 시간 이후의 POD 데이터 리스트
     */
    @GetMapping("/after")
    public ResponseEntity<List<PodInfo>> getPodDataAfterTimestamp(
            @RequestParam("timestamp") String timestamp) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp);
            List<PodInfo> podDataList = podDataService.getPodDataAfterTimestamp(dateTime);
            log.info("POD 데이터 조회 완료 ({} 이후): {}개", timestamp, podDataList.size());
            return ResponseEntity.ok(podDataList);
        } catch (Exception e) {
            log.error("POD 데이터 조회 실패: {}", e.getMessage(), e);
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
                podDataETLEngine.getLatestTimestamp();
            
            int processedCount = podDataETLEngine.processETL(timestamp);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("processedCount", processedCount);
            result.put("lastProcessedTime", timestamp.toString());
            result.put("message", "POD 정보 ETL 프로세스가 성공적으로 실행되었습니다.");
            
            log.info("POD 정보 ETL 프로세스 실행 완료: {}개 처리", processedCount);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("POD 정보 ETL 프로세스 실행 실패: {}", e.getMessage(), e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "POD 정보 ETL 프로세스 실행 중 오류가 발생했습니다.");
            
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
            boolean isConnected = podDataETLEngine.isConnected();
            LocalDateTime latestTimestamp = podDataETLEngine.getLatestTimestamp();
            
            Map<String, Object> status = new HashMap<>();
            status.put("connected", isConnected);
            status.put("latestTimestamp", latestTimestamp.toString());
            status.put("service", "PodDataService");
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("POD 정보 서비스 상태 확인 실패: {}", e.getMessage(), e);
            
            Map<String, Object> status = new HashMap<>();
            status.put("connected", false);
            status.put("error", e.getMessage());
            status.put("service", "PodDataService");
            
            return ResponseEntity.ok(status);
        }
    }
    
    /**
     * 특정 POD 정보 조회
     * 
     * @param podId POD ID
     * @return POD 정보
     */
    @GetMapping("/{podId}")
    public ResponseEntity<PodInfo> getPodById(@PathVariable String podId) {
        try {
            // 실제 구현에서는 POD ID로 조회하는 메서드가 필요합니다
            List<PodInfo> allData = podDataService.getAllPodData();
            PodInfo result = allData.stream()
                .filter(data -> podId.equals(data.getPodId()))
                .findFirst()
                .orElse(null);
            
            if (result != null) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("POD 정보 조회 실패: podId={}, error={}", podId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 