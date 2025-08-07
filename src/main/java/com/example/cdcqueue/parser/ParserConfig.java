package com.example.cdcqueue.parser;

import java.util.Map;
import java.util.HashMap;

/**
 * 파서 설정 클래스
 * 
 * 데이터 파서의 동작을 제어하는 설정 정보를 담습니다.
 * 실제 WCS DB 스키마에 맞춰 필드 매핑을 설정합니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public class ParserConfig {
    
    /**
     * 필드 매핑 설정 (원시 데이터 필드명 -> AGV 데이터 필드명)
     * 실제 DB 컬럼명에 맞춰 설정
     */
    private Map<String, String> fieldMappings = new HashMap<>();
    
    /**
     * 날짜 형식
     */
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    
    /**
     * 시간대
     */
    private String timeZone = "Asia/Seoul";
    
    /**
     * null 값 허용 여부
     */
    private boolean allowNullValues = false;
    
    /**
     * 유효성 검사 활성화 여부
     */
    private boolean validationEnabled = true;
    
    /**
     * 기본 생성자
     */
    public ParserConfig() {
        // 실제 DB 컬럼명에 맞춘 필드 매핑 설정
        fieldMappings.put("uuid_no", "uuidNo");
        fieldMappings.put("robot_no", "robotNo");
        fieldMappings.put("map_code", "mapCode");
        fieldMappings.put("zone_code", "zoneCode");
        fieldMappings.put("status", "status");
        fieldMappings.put("manual", "manual");
        fieldMappings.put("loaders", "loaders");
        fieldMappings.put("report_time", "reportTime");
        fieldMappings.put("battery", "battery");
        fieldMappings.put("node_id", "nodeId");
        fieldMappings.put("pos_x", "posX");
        fieldMappings.put("pos_y", "posY");
        fieldMappings.put("speed", "speed");
        fieldMappings.put("task_id", "taskId");
        fieldMappings.put("next_target", "nextTarget");
        fieldMappings.put("pod_id", "podId");
    }
    
    // Getter/Setter 메서드들
    public Map<String, String> getFieldMappings() { return fieldMappings; }
    public void setFieldMappings(Map<String, String> fieldMappings) { this.fieldMappings = fieldMappings; }
    
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
    
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    
    public boolean isAllowNullValues() { return allowNullValues; }
    public void setAllowNullValues(boolean allowNullValues) { this.allowNullValues = allowNullValues; }
    
    public boolean isValidationEnabled() { return validationEnabled; }
    public void setValidationEnabled(boolean validationEnabled) { this.validationEnabled = validationEnabled; }
} 