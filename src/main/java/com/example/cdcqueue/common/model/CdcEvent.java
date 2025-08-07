package com.example.cdcqueue.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CDC(Change Data Capture) 이벤트 모델
 * 
 * 데이터베이스 변화를 감지하여 생성되는 이벤트를 표현하는 클래스
 * AGV 데이터 변화를 WebSocket을 통해 클라이언트에 전송할 때 사용됩니다.
 * 
 * 이벤트 구조:
 * - id: 고유 식별자 (UUID)
 * - type: 이벤트 타입 (예: "AGV_UPDATE", "MANUAL")
 * - payload: 이벤트 데이터 (JSON 형태의 AGV 데이터)
 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
@Data
@AllArgsConstructor    // 전체 필드 생성자 자동 생성
@NoArgsConstructor     // 기본 생성자 자동 생성
public class CdcEvent {
    
    /**
     * 이벤트 고유 식별자 (UUID)
     */
    private String id;
    
    /**
     * 이벤트 타입
     * - "AGV_UPDATE": AGV 데이터 업데이트
     * - "MANUAL": 수동 생성 이벤트
     */
    private String type;
    
    /**
     * 이벤트 페이로드 (JSON 형태의 데이터)
     * AGV 데이터가 JSON 문자열로 직렬화되어 저장됨
     */
    private String payload;
}
