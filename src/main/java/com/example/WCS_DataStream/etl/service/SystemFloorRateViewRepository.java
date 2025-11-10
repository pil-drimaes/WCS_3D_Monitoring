package com.example.WCS_DataStream.etl.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemFloorRateViewRepository {

    private final JdbcTemplate postgresqlJdbcTemplate;

    public SystemFloorRateViewRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }

    public boolean isConnected() { try { postgresqlJdbcTemplate.queryForObject("SELECT 1", Integer.class); return true; } catch (Exception e) { return false; } }

    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int upsert(String zoneCd, String floor, java.math.BigDecimal rate, java.math.BigDecimal qty) {
        String sql = """
            INSERT INTO public.floor_rate_view (zone_cd, floor, rate, qty)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (zone_cd, floor) DO UPDATE SET
              rate = EXCLUDED.rate,
              qty = EXCLUDED.qty,
              upd_dt = CURRENT_TIMESTAMP,
              upd_user_id = 'etl'
        """;
        return postgresqlJdbcTemplate.update(sql, zoneCd, floor, rate, qty);
    }
}


