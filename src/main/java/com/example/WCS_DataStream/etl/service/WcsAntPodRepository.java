package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.ant.AntPodInfoRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class WcsAntPodRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsAntPodRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<AntPodInfoRecord> fetchIncremental(Timestamp lastTs, String lastUuid, int limit) {
        String sql = """
            SELECT TOP (?)
              UUID, POD_ID, POD_FACE, LOCATION, REPORT_TIME, INS_DT, INS_USER_ID, UPD_DT, UPD_USER_ID
            FROM cdc_test.dbo.ANT_POD_INFO WITH (READPAST)
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

    private AntPodInfoRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        AntPodInfoRecord r = new AntPodInfoRecord();
        r.setUuid(rs.getString("UUID"));
        r.setPodId(rs.getString("POD_ID"));
        r.setPodFace(rs.getString("POD_FACE"));
        r.setLocation(rs.getString("LOCATION"));
        r.setReportTime(rs.getString("REPORT_TIME"));
        r.setInsDt(rs.getTimestamp("INS_DT"));
        r.setInsUserId(rs.getString("INS_USER_ID"));
        r.setUpdDt(rs.getTimestamp("UPD_DT"));
        r.setUpdUserId(rs.getString("UPD_USER_ID"));
        return r;
    }
} 