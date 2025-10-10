package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.ant.AntRobotInfoRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class WcsAntRobotRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsAntRobotRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<AntRobotInfoRecord> fetchIncremental(Timestamp lastTs, String lastUuid, int limit) {
        String sql = """
            SELECT TOP (?)
              UUID, ROBOT_NO, ROBOT_TYPE, MAP_CODE, ZONE_CODE, STATUS, MANUAL, LOADERS,
              REPORT_TIME, BATTERY, NODE_ID, POS_X, POS_Y, SPEED, TASK_ID, NEXT_TARGET,
              POD_ID, INS_DT, INS_USER_ID, UPD_DT, UPD_USER_ID
            FROM cdc_test.dbo.ANT_ROBOT_INFO WITH (READPAST)
            WHERE (UPD_DT > ?) OR (UPD_DT = ? AND UUID > ?)
            ORDER BY UPD_DT ASC, UUID ASC
        """;
        return wcsJdbcTemplate.query(sql, ps -> {
            ps.setInt(1, limit);
            ps.setTimestamp(2, lastTs);
            ps.setTimestamp(3, lastTs);
            ps.setString(4, lastUuid == null ? "" : lastUuid);
        }, this::mapRow);
    }

    private AntRobotInfoRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        AntRobotInfoRecord r = new AntRobotInfoRecord();
        r.setUuid(rs.getString("UUID"));
        r.setRobotNo(rs.getString("ROBOT_NO"));
        r.setRobotType(rs.getString("ROBOT_TYPE"));
        r.setMapCode(rs.getString("MAP_CODE"));
        r.setZoneCode(rs.getString("ZONE_CODE"));
        r.setStatus(rs.getObject("STATUS") == null ? null : rs.getInt("STATUS"));
        r.setManual(parseManual(rs.getString("MANUAL")));
        r.setLoaders(rs.getString("LOADERS"));
        r.setReportTime(rs.getString("REPORT_TIME"));
        r.setBattery(rs.getBigDecimal("BATTERY"));
        r.setNodeId(rs.getString("NODE_ID"));
        r.setPosX(rs.getBigDecimal("POS_X"));
        r.setPosY(rs.getBigDecimal("POS_Y"));
        r.setSpeed(rs.getBigDecimal("SPEED"));
        r.setTaskId(rs.getString("TASK_ID"));
        r.setNextTarget(rs.getString("NEXT_TARGET"));
        r.setPodId(rs.getString("POD_ID"));
        r.setInsDt(rs.getTimestamp("INS_DT"));
        r.setInsUserId(rs.getString("INS_USER_ID"));
        r.setUpdDt(rs.getTimestamp("UPD_DT"));
        r.setUpdUserId(rs.getString("UPD_USER_ID"));
        return r;
    }

    private Boolean parseManual(String v) {
        if (v == null) return null;
        String s = v.trim().toLowerCase();
        return s.equals("y") || s.equals("yes") || s.equals("1") || s.equals("true");
    }
} 