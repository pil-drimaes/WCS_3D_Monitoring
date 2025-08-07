package com.example.cdcqueue.parser;

import com.example.cdcqueue.common.model.AgvData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL 결과를 AGV 데이터로 변환하는 파서
 * 
 * JDBC ResultSet을 AGV 데이터 객체로 변환합니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Component
public class SqlDataParser implements DataParser<ResultSet> {
    
    private static final Logger log = LoggerFactory.getLogger(SqlDataParser.class);
    
    /**
     * 파서 설정
     */
    private ParserConfig config;
    
    /**
     * 날짜 포맷터
     */
    private DateTimeFormatter dateFormatter;
    
    @Override
    public void initialize(ParserConfig config) {
        this.config = config;
        this.dateFormatter = DateTimeFormatter.ofPattern(config.getDateFormat());
        log.info("SqlDataParser initialized with config: {}", config.getDateFormat());
    }
    
    @Override
    public List<AgvData> parse(ResultSet resultSet) throws DataParseException {
        List<AgvData> agvDataList = new ArrayList<>();
        
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 컬럼명 매핑 생성
            Map<String, Integer> columnMapping = createColumnMapping(metaData, columnCount);
            
            while (resultSet.next()) {
                AgvData agvData = parseRow(resultSet, columnMapping);
                if (agvData != null) {
                    agvDataList.add(agvData);
                }
            }
            
            log.debug("Parsed {} AGV data records from ResultSet", agvDataList.size());
            
        } catch (SQLException e) {
            throw new DataParseException("Error parsing ResultSet", e);
        }
        
        return agvDataList;
    }
    
    @Override
    public boolean supports(ResultSet data) {
        return data != null;
    }
    
    /**
     * 컬럼 매핑 생성
     */
    private Map<String, Integer> createColumnMapping(ResultSetMetaData metaData, int columnCount) throws SQLException {
        Map<String, Integer> mapping = new java.util.HashMap<>();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            mapping.put(columnName.toUpperCase(), i);
        }
        
        return mapping;
    }
    
    /**
     * ResultSet의 한 행을 AGV 데이터로 파싱
     */
    private AgvData parseRow(ResultSet resultSet, Map<String, Integer> columnMapping) throws SQLException {
        AgvData agvData = new AgvData();
        
        try {
            // UUID 번호 파싱
            if (columnMapping.containsKey("uuid_no")) {
                String uuidNo = resultSet.getString(columnMapping.get("uuid_no"));
                if (isValidValue(uuidNo)) {
                    agvData.setUuidNo(uuidNo);
                }
            }
            
            // 로봇 번호 파싱
            if (columnMapping.containsKey("robot_no")) {
                String robotNo = resultSet.getString(columnMapping.get("robot_no"));
                if (isValidValue(robotNo)) {
                    agvData.setRobotNo(robotNo);
                }
            }
            
            // 맵 코드 파싱
            if (columnMapping.containsKey("map_code")) {
                String mapCode = resultSet.getString(columnMapping.get("map_code"));
                if (isValidValue(mapCode)) {
                    agvData.setMapCode(mapCode);
                }
            }
            
            // 존 코드 파싱
            if (columnMapping.containsKey("zone_code")) {
                String zoneCode = resultSet.getString(columnMapping.get("zone_code"));
                if (isValidValue(zoneCode)) {
                    agvData.setZoneCode(zoneCode);
                }
            }
            
            // 상태 파싱
            if (columnMapping.containsKey("status")) {
                int status = resultSet.getInt(columnMapping.get("status"));
                if (!resultSet.wasNull()) {
                    agvData.setStatus(status);
                }
            }
            
            // 수동 모드 파싱
            if (columnMapping.containsKey("manual")) {
                boolean manual = resultSet.getBoolean(columnMapping.get("manual"));
                if (!resultSet.wasNull()) {
                    agvData.setManual(manual);
                }
            }
            
            // 로더 정보 파싱
            if (columnMapping.containsKey("loaders")) {
                String loaders = resultSet.getString(columnMapping.get("loaders"));
                if (isValidValue(loaders)) {
                    agvData.setLoaders(loaders);
                }
            }
            
            // 리포트 시간 파싱
            if (columnMapping.containsKey("report_time")) {
                long reportTime = resultSet.getLong(columnMapping.get("report_time"));
                if (!resultSet.wasNull()) {
                    agvData.setReportTime(reportTime);
                }
            }
            
            // 배터리 파싱
            if (columnMapping.containsKey("battery")) {
                BigDecimal battery = resultSet.getBigDecimal(columnMapping.get("battery"));
                if (!resultSet.wasNull()) {
                    agvData.setBattery(battery);
                }
            }
            
            // 노드 ID 파싱
            if (columnMapping.containsKey("node_id")) {
                String nodeId = resultSet.getString(columnMapping.get("node_id"));
                if (isValidValue(nodeId)) {
                    agvData.setNodeId(nodeId);
                }
            }
            
            // X 좌표 파싱
            if (columnMapping.containsKey("pos_x")) {
                BigDecimal posX = resultSet.getBigDecimal(columnMapping.get("pos_x"));
                if (!resultSet.wasNull()) {
                    agvData.setPosX(posX);
                }
            }
            
            // Y 좌표 파싱
            if (columnMapping.containsKey("pos_y")) {
                BigDecimal posY = resultSet.getBigDecimal(columnMapping.get("pos_y"));
                if (!resultSet.wasNull()) {
                    agvData.setPosY(posY);
                }
            }
            
            // 속도 파싱
            if (columnMapping.containsKey("speed")) {
                BigDecimal speed = resultSet.getBigDecimal(columnMapping.get("speed"));
                if (!resultSet.wasNull()) {
                    agvData.setSpeed(speed);
                }
            }
            
            // 작업 ID 파싱
            if (columnMapping.containsKey("task_id")) {
                String taskId = resultSet.getString(columnMapping.get("task_id"));
                if (isValidValue(taskId)) {
                    agvData.setTaskId(taskId);
                }
            }
            
            // 다음 목표 파싱
            if (columnMapping.containsKey("next_target")) {
                String nextTarget = resultSet.getString(columnMapping.get("next_target"));
                if (isValidValue(nextTarget)) {
                    agvData.setNextTarget(nextTarget);
                }
            }
            
            // POD ID 파싱
            if (columnMapping.containsKey("pod_id")) {
                String podId = resultSet.getString(columnMapping.get("pod_id"));
                if (isValidValue(podId)) {
                    agvData.setPodId(podId);
                }
            }
            
            // 레거시 호환성을 위한 파싱 (점진적 제거 예정)
            // 실제 DB 컬럼으로 통일되었으므로 이 파싱들은 제거 예정
            
            // 유효성 검사
            if (config.isValidationEnabled() && !isValidAgvData(agvData)) {
                log.warn("Invalid AGV data detected: {}", agvData);
                return null;
            }
            
            return agvData;
            
        } catch (SQLException e) {
            log.error("Error parsing row: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 값이 유효한지 확인
     */
    private boolean isValidValue(String value) {
        if (value == null) {
            return config.isAllowNullValues();
        }
        return !value.trim().isEmpty();
    }
    
    /**
     * AGV 데이터 유효성 검사
     */
    private boolean isValidAgvData(AgvData agvData) {
        // 필수 필드 검사 (robot_no는 필수)
        if (agvData.getRobotNo() == null || agvData.getRobotNo().trim().isEmpty()) {
            return false;
        }
        
        // 좌표 유효성 검사 (pos_x, pos_y는 필수)
        if (agvData.getPosX() == null || agvData.getPosY() == null) {
            return false;
        }
        
        // 리포트 시간 검사 (report_time은 필수)
        if (agvData.getReportTime() == null) {
            return false;
        }
        
        // 레거시 호환성 검사 (점진적 제거 예정)
        // 실제 DB 컬럼으로 통일되었으므로 이 검사는 제거 예정
        
        return true;
    }
} 