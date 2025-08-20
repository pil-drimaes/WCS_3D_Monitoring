package com.example.WCS_DataStream.etl.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POD 정보 데이터 모델
 * 
 * POD 정보 테이블에 대한 엔티티 클래스
 * POD의 위치와 상태 정보를 관리합니다.
 * 
 * 테이블 구조:
 * - uuid_no: UUID 번호 (String(50))
 * - pod_id: POD ID (String(50))
 * - pod_face: POD 면 (String(50))
 * - location: 위치 (String(50))
 * - report_time: 보고시간 (Long)
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PodInfo {
    
    /**
     * UUID 번호 (고유 식별자)
     * 예: 550e8400-e29b-41d4-a716-446655440000
     */
    private String uuidNo;
    
    /**
     * POD ID
     * 예: POD001
     */
    private String podId;
    
    /**
     * POD 면
     * 예: A1
     */
    private String podFace;
    
    /**
     * 위치
     * 예: 123
     */
    private String location;
    
    /**
     * 보고시간 (Unix timestamp)
     * 예: 1754565354000
     */
    private Long reportTime;
    
    /**
     * POD 정보 생성자 (UUID 제외)
     * 
     * @param podId POD ID
     * @param podFace POD 면
     * @param location 위치
     * @param reportTime 보고시간
     */
    public PodInfo(String podId, String podFace, String location, Long reportTime) {
        this.podId = podId;
        this.podFace = podFace;
        this.location = location;
        this.reportTime = reportTime;
    }
} 