package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.view.CapaDayViewRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class WcsCapaDayViewRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsCapaDayViewRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<CapaDayViewRow> fetchAll() {
        String sql = """
            SELECT TIME, QTY, [TYPE]
            FROM dbo.CAPA_DAY_VIEW WITH (READPAST)
        """;
        return wcsJdbcTemplate.query(sql, this::mapRow);
    }

    private CapaDayViewRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        String time = rs.getString("TIME");
        BigDecimal qty = rs.getBigDecimal("QTY");
        String type = rs.getString("TYPE");
        return new CapaDayViewRow(time, qty, type);
    }
}


