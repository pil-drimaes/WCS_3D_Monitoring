package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.view.ZoneRateViewRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class WcsZoneRateViewRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsZoneRateViewRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<ZoneRateViewRow> fetchAll() {
        String sql = """
            SELECT ZONE_CD, RATE, QTY
            FROM dbo.ZONE_RATE_VIEW WITH (READPAST)
        """;
        return wcsJdbcTemplate.query(sql, this::mapRow);
    }

    private ZoneRateViewRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        String zoneCd = rs.getString("ZONE_CD");
        BigDecimal rate = rs.getBigDecimal("RATE");
        BigDecimal qty = rs.getBigDecimal("QTY");
        return new ZoneRateViewRow(zoneCd, rate, qty);
    }
}


