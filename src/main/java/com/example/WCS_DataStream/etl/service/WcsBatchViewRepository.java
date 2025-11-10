package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.view.BatchViewRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class WcsBatchViewRepository {

    private final JdbcTemplate wcsJdbcTemplate;

    public WcsBatchViewRepository(JdbcTemplate wcsJdbcTemplate) {
        this.wcsJdbcTemplate = wcsJdbcTemplate;
    }

    public List<BatchViewRow> fetchAll() {
        String sql = """
            SELECT BATCH_NO, START_TIME, END_TIME
            FROM dbo.BATCH_VIEW WITH (READPAST)
        """;
        return wcsJdbcTemplate.query(sql, this::mapRow);
    }

    private BatchViewRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new BatchViewRow(
            rs.getString("BATCH_NO"),
            rs.getString("START_TIME"),
            rs.getString("END_TIME")
        );
    }
}


