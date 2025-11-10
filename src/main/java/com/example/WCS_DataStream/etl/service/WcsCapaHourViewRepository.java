package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.view.CapaHourViewRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class WcsCapaHourViewRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsCapaHourViewRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<CapaHourViewRow> fetchAll() {
        String sql = """
            SELECT TIME, [TYPE], QTY
            FROM dbo.CAPA_HOUR_VIEW WITH (READPAST)
        """;
        return wcsJdbcTemplate.query(sql, this::mapRow);
    }

    private CapaHourViewRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        String time = rs.getString("TIME");
        String type = rs.getString("TYPE");
        BigDecimal qty = rs.getBigDecimal("QTY");
        return new CapaHourViewRow(time, type, qty);
    }
}


