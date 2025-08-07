package com.example.cdcqueue.etl.controller;

import com.example.cdcqueue.common.model.AgvData;
import com.example.cdcqueue.common.model.CdcEvent;
import com.example.cdcqueue.common.queue.EventQueue;
import com.example.cdcqueue.etl.service.AgvDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**

 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
@RestController
public class TestController {
    
    /**
     * 이벤트 큐 (테스트 이벤트 생성용)
     */
    private final EventQueue queue;
    
    /**
     * AGV 데이터 서비스 (AGV 데이터 조회용)
     */
    private final AgvDataService wcsService;

    /**
     * 생성자
     * 
     * @param queue 이벤트 큐
     * @param wcsService AGV 데이터 서비스
     */
    @Autowired
    public TestController(EventQueue queue, AgvDataService wcsService) {
        this.queue = queue;
        this.wcsService = wcsService;
    }

    /**
     * 모든 AGV 데이터 조회
     * 
     * 데이터베이스의 모든 AGV 데이터를 최신순으로 조회합니다.
     * 
     * @return 모든 AGV 데이터 리스트 (JSON)
     */
    @GetMapping("/api/agv-data")
    public List<AgvData> getAllAgvData() {
        return wcsService.getAllAgvData();
    }

    /**
     * 최신 AGV 데이터 조회
     * 
     * 데이터베이스의 최신 AGV 데이터 10개를 조회합니다.
     * 웹 인터페이스에서 주로 사용됩니다.
     * 
     * @return 최신 AGV 데이터 10개 리스트 (JSON)
     */
    @GetMapping("/api/agv-data/latest")
    public List<AgvData> getLatestAgvData() {
        return wcsService.getLatestAgvData();
    }

    /**
     * 테이블 구조 확인
     * 
     * agv_data 테이블의 컬럼 구조를 확인합니다.
     * 개발 및 디버깅 목적으로 사용됩니다.
     * 
     * @return 테이블 구조 정보 (컬럼명, 데이터타입)
     */
    @GetMapping("/api/table-structure")
    public List<Map<String, Object>> getTableStructure() {
        return wcsService.getTableStructure();
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
}
