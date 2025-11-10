package com.example.WCS_DataStream.etl.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemBatchViewRepository {

    private final JdbcTemplate postgresqlJdbcTemplate;

    public SystemBatchViewRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }

    public boolean isConnected() {
        try { postgresqlJdbcTemplate.queryForObject("SELECT 1", Integer.class); return true; } catch (Exception e) { return false; }
    }

    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int upsert(String batchNo, String startTime, String endTime) {
        String sql = """
            INSERT INTO public.batch_view (batch_no, start_time, end_time)
            VALUES (?, ?, ?)
            ON CONFLICT (batch_no) DO UPDATE SET
              start_time = EXCLUDED.start_time,
              end_time = EXCLUDED.end_time,
              upd_dt = CURRENT_TIMESTAMP,
              upd_user_id = 'etl'
        """;
        return postgresqlJdbcTemplate.update(sql, batchNo, startTime, endTime);
    }
}


