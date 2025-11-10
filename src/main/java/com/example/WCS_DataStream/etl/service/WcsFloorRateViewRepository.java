package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.view.FloorRateViewRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class WcsFloorRateViewRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsFloorRateViewRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<FloorRateViewRow> fetchAll() {
        String sql = """
            SELECT ZONE_CD, FLOOR, RATE, QTY
            FROM dbo.FLOOR_RATE_VIEW WITH (READPAST)
        """;
        return wcsJdbcTemplate.query(sql, this::mapRow);
    }

    private FloorRateViewRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        String zoneCd = rs.getString("ZONE_CD");
        String floor = rs.getString("FLOOR");
        BigDecimal rate = rs.getBigDecimal("RATE");
        BigDecimal qty = rs.getBigDecimal("QTY");
        return new FloorRateViewRow(zoneCd, floor, rate, qty);
    }
}


