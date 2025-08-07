package com.example.cdcqueue.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AGV(Automated Guided Vehicle) 데이터 모델
 * 
 * 실제 WCS DB 스키마에 맞춰 업데이트된 엔티티 클래스
 * AGV의 위치, 상태, 작업 정보를 포함합니다.
 * 
 * 테이블 구조:
 * - uuid_no: UUID 번호 (String(50))
 * - robot_no: 로봇 번호 (String(20))
 * - map_code: 맵 코드 (String(20))
 * - zone_code: 존 코드 (String(20))
 * - status: 상태 (Integer)
 * - manual: 수동 모드 여부 (Boolean)
 * - loaders: 로더 정보 (String(50))
 * - report_time: 리포트 시간 (Long)
 * - battery: 배터리 잔량 (Decimal(13,4))
 * - node_id: 노드 ID (String(50))
 * - pos_x: X 좌표 (Decimal(13,4))
 * - pos_y: Y 좌표 (Decimal(13,4))
 * - speed: 속도 (Decimal(13,4))
 * - task_id: 작업 ID (String(50))
 * - next_target: 다음 목표 (String(50))
 * - pod_id: POD ID (String(50))
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgvData {
    
    /**
     * UUID 번호 (고유 식별자)
     * 예: 550e8400-e29b-41d4-a716-4466554400000
     */
    private String uuidNo;
    
    /**
     * 로봇 번호
     * 예: ROBOT_001
     */
    private String robotNo;
    
    /**
     * 맵 코드
     * 예: MAP_001
     */
    private String mapCode;
    
    /**
     * 존 코드
     * 예: ZONE_2024Q3
     */
    private String zoneCode;
    
    /**
     * 상태 (1: Working, 2: Idle, 3: Error 등)
     */
    private Integer status;
    
    /**
     * 수동 모드 여부
     */
    private Boolean manual;
    
    /**
     * 로더 정보 (콤마로 구분)
     * 예: Loader1,Loader2,Loader3
     */
    private String loaders;
    
    /**
     * 리포트 시간 (Unix timestamp)
     * 예: 1717654321000
     */
    private Long reportTime;
    
    /**
     * 배터리 잔량
     * 예: 95.5000
     */
    private BigDecimal battery;
    
    /**
     * 노드 ID
     * 예: Node_123
     */
    private String nodeId;
    
    /**
     * X 좌표
     * 예: 123.4567
     */
    private BigDecimal posX;
    
    /**
     * Y 좌표
     * 예: 456.7890
     */
    private BigDecimal posY;
    
    /**
     * 속도
     * 예: 1.2345
     */
    private BigDecimal speed;
    
    /**
     * 작업 ID
     * 예: TASK_001
     */
    private String taskId;
    
    /**
     * 다음 목표
     * 예: Target_Node_A
     */
    private String nextTarget;
    
    /**
     * POD ID
     * 예: POD001
     */
    private String podId;
    
    /**
     * AGV 데이터 생성자 (UUID 제외)
     * 
     * @param robotNo 로봇 번호
     * @param mapCode 맵 코드
     * @param zoneCode 존 코드
     * @param status 상태
     * @param manual 수동 모드 여부
     * @param loaders 로더 정보
     * @param reportTime 리포트 시간
     * @param battery 배터리 잔량
     * @param nodeId 노드 ID
     * @param posX X 좌표
     * @param posY Y 좌표
     * @param speed 속도
     * @param taskId 작업 ID
     * @param nextTarget 다음 목표
     * @param podId POD ID
     */
    public AgvData(String robotNo, String mapCode, String zoneCode, Integer status, 
                   Boolean manual, String loaders, Long reportTime, BigDecimal battery,
                   String nodeId, BigDecimal posX, BigDecimal posY, BigDecimal speed,
                   String taskId, String nextTarget, String podId) {
        this.robotNo = robotNo;
        this.mapCode = mapCode;
        this.zoneCode = zoneCode;
        this.status = status;
        this.manual = manual;
        this.loaders = loaders;
        this.reportTime = reportTime;
        this.battery = battery;
        this.nodeId = nodeId;
        this.posX = posX;
        this.posY = posY;
        this.speed = speed;
        this.taskId = taskId;
        this.nextTarget = nextTarget;
        this.podId = podId;
    }
    
    // 호환성을 위한 레거시 메서드들
    @Deprecated
    public String getAgvId() { return robotNo; }
    @Deprecated
    public void setAgvId(String agvId) { this.robotNo = agvId; }
    
    @Deprecated
    public Double getX() { return posX != null ? posX.doubleValue() : null; }
    @Deprecated
    public void setX(Double x) { this.posX = x != null ? BigDecimal.valueOf(x) : null; }
    
    @Deprecated
    public Double getY() { return posY != null ? posY.doubleValue() : null; }
    @Deprecated
    public void setY(Double y) { this.posY = y != null ? BigDecimal.valueOf(y) : null; }
    
    @Deprecated
    public LocalDateTime getTimestamp() { 
        // reportTime을 직접 사용 (이미 올바른 밀리초 타임스탬프)
        return reportTime != null ? 
            LocalDateTime.ofEpochSecond(reportTime / 1000, 0, java.time.ZoneOffset.UTC) : null; 
    }
    @Deprecated
    public void setTimestamp(LocalDateTime timestamp) { 
        this.reportTime = timestamp != null ? timestamp.toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null; 
    }
    
    @Deprecated
    public String getDescription() { 
        return String.format("Robot: %s, Status: %d, Battery: %s%%, Task: %s", 
            robotNo, status, battery, taskId); 
    }
    @Deprecated
    public void setDescription(String description) { /* 레거시 호환성만 제공 */ }
    
    @Deprecated
    public Long getId() { return null; } // UUID 기반이므로 ID는 사용하지 않음
    @Deprecated
    public void setId(Long id) { /* 레거시 호환성만 제공 */ }
} 