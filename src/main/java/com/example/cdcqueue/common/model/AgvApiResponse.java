package com.example.cdcqueue.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AGV API 응답 데이터 모델
 * 
 * AGV 정보 API 호출 시 MQ로 받는 response 데이터 구조
 * 
 * 필드 설명:
 * - r: robotId (String(50)) - 예) Robot_001
 * - zone: ZoneId (String(50)) - 예) 1F-PLT
 * - c: cellID (Integer) - 예) 1
 * - rD: Direction of the AGC front (Integer) - 0, 90, 180, 270; 0 at the top of the map - 예) 90
 * - p: podID (String(50)) - 예) POD001
 * - pD: 0, 90, 180, 270 the A-side direction of POD (Integer) - 0,90,180,270; at the top of the map - 예) 0
 * - s: status (Integer) - -1: offline 0: Standby 1: working 2: charging 3: Error 4: Connecting - 예) 0
 * - m: maintenance or not (Boolean) - true false - 예) false
 * - b: agv power (Integer) - 예) 100
 * - x: x_coordinate (Integer) - 예) 1
 * - y: y_coordinate (Integer) - 예) 0
 * - hp: whether a POD exists (Boolean) - True, False
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgvApiResponse {
    
    /**
     * 로봇 ID
     * 예: Robot_001
     */
    private String r;
    
    /**
     * 존 ID
     * 예: 1F-PLT
     */
    private String zone;
    
    /**
     * 셀 ID
     * 예: 1
     */
    private Integer c;
    
    /**
     * AGC 전면 방향 (0, 90, 180, 270; 맵 상단이 0)
     * 예: 90
     */
    private Integer rD;
    
    /**
     * POD ID
     * 예: POD001
     */
    private String p;
    
    /**
     * POD A면 방향 (0, 90, 180, 270; 맵 상단이 0)
     * 예: 0
     */
    private Integer pD;
    
    /**
     * 상태 (-1: offline, 0: Standby, 1: working, 2: charging, 3: Error, 4: Connecting)
     * 예: 0
     */
    private Integer s;
    
    /**
     * 유지보수 여부
     * 예: false
     */
    private Boolean m;
    
    /**
     * AGV 전력
     * 예: 100
     */
    private Integer b;
    
    /**
     * X 좌표
     * 예: 1
     */
    private Integer x;
    
    /**
     * Y 좌표
     * 예: 0
     */
    private Integer y;
    
    /**
     * POD 존재 여부
     * 예: true
     */
    private Boolean hp;
    
    /**
     * AGV API 응답을 AgvData로 변환
     * 
     * @return AgvData 객체
     */
    public AgvData toAgvData() {
        AgvData agvData = new AgvData();
        agvData.setRobotNo(this.r);
        agvData.setZoneCode(this.zone);
        agvData.setStatus(this.s);
        agvData.setPodId(this.p);
        agvData.setBattery(this.b != null ? java.math.BigDecimal.valueOf(this.b) : null);
        agvData.setPosX(this.x != null ? java.math.BigDecimal.valueOf(this.x) : null);
        agvData.setPosY(this.y != null ? java.math.BigDecimal.valueOf(this.y) : null);
        agvData.setReportTime(System.currentTimeMillis());
        
        // 기본값 설정
        agvData.setManual(false);
        agvData.setMapCode("MAP_DEFAULT");
        
        return agvData;
    }
} 