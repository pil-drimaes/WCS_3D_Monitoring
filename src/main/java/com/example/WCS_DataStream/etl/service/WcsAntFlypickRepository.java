package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.ant.AntFlypickInfoRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class WcsAntFlypickRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsAntFlypickRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<AntFlypickInfoRecord> fetchIncremental(Timestamp lastTs, String lastUuid, int limit) {
        String sql = """
            SELECT TOP (?)
              UUID, ROBOT_NO, ROBOT_TYPE, MAP_CODE, ZONE_CODE, STATUS, MANUAL, REPORT_TIME,
              BATTERY, NODE_ID, POS_X, POS_Y, SPEED, TASK_ID, NEXT_TARGET, POD_ID,
              INS_DT, INS_USER_ID, UPD_DT, UPD_USER_ID
            FROM cdc_test.dbo.ANT_FLYPICK_INFO WITH (READPAST)
            WHERE (COALESCE(UPD_DT, INS_DT) > ?)
               OR (COALESCE(UPD_DT, INS_DT) = ? AND UUID > ?)
            ORDER BY COALESCE(UPD_DT, INS_DT) ASC, UUID ASC
        """;
        return wcsJdbcTemplate.query(sql, ps -> {
            ps.setInt(1, limit);
            ps.setTimestamp(2, lastTs);
            ps.setTimestamp(3, lastTs);
            ps.setString(4, lastUuid == null ? "" : lastUuid);
        }, this::mapRow);
    }

    private AntFlypickInfoRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        AntFlypickInfoRecord r = new AntFlypickInfoRecord();
        r.setUuid(rs.getString("UUID"));
        r.setRobotNo(rs.getString("ROBOT_NO"));
        r.setRobotType(rs.getString("ROBOT_TYPE"));
        r.setMapCode(rs.getString("MAP_CODE"));
        r.setZoneCode(rs.getString("ZONE_CODE"));
        r.setStatus(rs.getObject("STATUS") == null ? null : rs.getInt("STATUS"));
        r.setManual(rs.getString("MANUAL"));
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
} 