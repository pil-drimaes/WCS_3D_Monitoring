package com.example.cdcqueue.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POD API 응답 데이터 모델
 * 
 * POD 정보 API 호출 시 MQ로 받는 response 데이터 구조
 * 
 * 필드 설명:
 * - sectionID: sectionID (Integer) - 예) 2
 * - zone: zone (String) - 예) 1F_PLT
 * - currentCellID: currentCellID (Integer) - 예) 186
 * - podID: podID (String) - 예) 186
 * - podDirection: podDirection (Integer) - 예) 0
 * - x: x (Integer) - 예) 0
 * - y: y (Integer) - 예) 0
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PodApiResponse {
    
    /**
     * 섹션 ID
     * 예: 2
     */
    private Integer sectionID;
    
    /**
     * 존
     * 예: 1F_PLT
     */
    private String zone;
    
    /**
     * 현재 셀 ID
     * 예: 186
     */
    private Integer currentCellID;
    
    /**
     * POD ID
     * 예: 186
     */
    private String podID;
    
    /**
     * POD 방향
     * 예: 0
     */
    private Integer podDirection;
    
    /**
     * X 좌표
     * 예: 0
     */
    private Integer x;
    
    /**
     * Y 좌표
     * 예: 0
     */
    private Integer y;
    
    /**
     * POD API 응답을 PodInfo로 변환
     * 
     * @return PodInfo 객체
     */
    public PodInfo toPodInfo() {
        PodInfo podInfo = new PodInfo();
        podInfo.setPodId(this.podID);
        podInfo.setPodFace(this.sectionID != null ? "S" + this.sectionID : "S0");
        podInfo.setLocation(this.currentCellID != null ? this.currentCellID.toString() : "0");
        podInfo.setReportTime(System.currentTimeMillis());
        
        return podInfo;
    }
} 