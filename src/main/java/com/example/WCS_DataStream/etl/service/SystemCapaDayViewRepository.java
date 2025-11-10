package com.example.WCS_DataStream.etl.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SystemCapaDayViewRepository {

    private final JdbcTemplate postgresqlJdbcTemplate;

    public SystemCapaDayViewRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }

    public boolean isConnected() {
        try {
            postgresqlJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int upsert(LocalDate workDate, String time, java.math.BigDecimal qty, String type) {
        String sql = """
            INSERT INTO public.capa_day_view (work_date, "TIME", day_qty, "TYPE")
            VALUES (?, ?, ?, ?)
            ON CONFLICT (work_date, "TIME") DO UPDATE SET
              day_qty = EXCLUDED.day_qty,
              "TYPE" = EXCLUDED."TYPE",
              upd_dt = CURRENT_TIMESTAMP,
              upd_user_id = 'etl'
        """;
        return postgresqlJdbcTemplate.update(sql, workDate, time, qty, type);
    }
}


