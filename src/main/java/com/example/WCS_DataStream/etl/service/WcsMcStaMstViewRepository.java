package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.view.McStaMstViewRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class WcsMcStaMstViewRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsMcStaMstViewRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<McStaMstViewRow> fetchAll() {
        String sql = """
            SELECT MC_TYP, MC_NO, MC_AREA_TYP, MC_NM, ERROR_CODE
            FROM dbo.MC_STA_MST_VIEW WITH (READPAST)
        """;
        return wcsJdbcTemplate.query(sql, this::mapRow);
    }

    private McStaMstViewRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        String mcTyp = rs.getString("MC_TYP");
        String mcNo = rs.getString("MC_NO");
        String mcAreaTyp = rs.getString("MC_AREA_TYP");
        String mcNm = rs.getString("MC_NM");
        String errorCode = rs.getString("ERROR_CODE");
        return new McStaMstViewRow(mcTyp, mcNo, mcAreaTyp, mcNm, errorCode);
    }
}


