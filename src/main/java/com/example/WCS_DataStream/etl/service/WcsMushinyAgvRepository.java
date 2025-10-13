package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.mushiny.MushinyAgvInfoRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class WcsMushinyAgvRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsMushinyAgvRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<MushinyAgvInfoRecord> fetchIncremental(Timestamp lastTs, String lastUuid, int limit) {
        String sql = """
            SELECT TOP (?)
              UUID, ROBOT_NO, ZONE_CODE, NODE_ID, DIRECTION_FRONT, POD_ID, POD_DIRECTION,
              STATUS, MANUAL, BATTERY, POS_X, POS_Y, HAS_POD, INS_DT, INS_USER_ID, UPD_DT, UPD_USER_ID
            FROM cdc_test.dbo.MUSHINY_AGV_INFO WITH (READPAST)
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

    private MushinyAgvInfoRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        MushinyAgvInfoRecord r = new MushinyAgvInfoRecord();
        r.setUuid(rs.getString("UUID"));
        r.setRobotNo(rs.getString("ROBOT_NO"));
        r.setZoneCode(rs.getString("ZONE_CODE"));
        r.setNodeId(rs.getString("NODE_ID"));
        r.setDirectionFront(rs.getString("DIRECTION_FRONT"));
        r.setPodId(rs.getString("POD_ID"));
        r.setPodDirection(rs.getString("POD_DIRECTION"));
        r.setStatus(rs.getObject("STATUS") == null ? null : rs.getInt("STATUS"));
        r.setManual(rs.getString("MANUAL"));
        r.setBattery(rs.getBigDecimal("BATTERY"));
        r.setPosX(rs.getBigDecimal("POS_X"));
        r.setPosY(rs.getBigDecimal("POS_Y"));
        r.setHasPod(rs.getString("HAS_POD"));
        r.setInsDt(rs.getTimestamp("INS_DT"));
        r.setInsUserId(rs.getString("INS_USER_ID"));
        r.setUpdDt(rs.getTimestamp("UPD_DT"));
        r.setUpdUserId(rs.getString("UPD_USER_ID"));
        return r;
    }
} 