package com.example.WCS_DataStream.etl.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemMcStaMstViewRepository {

    private final JdbcTemplate postgresqlJdbcTemplate;

    public SystemMcStaMstViewRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }

    public boolean isConnected() { try { postgresqlJdbcTemplate.queryForObject("SELECT 1", Integer.class); return true; } catch (Exception e) { return false; } }

    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int upsert(String mcTyp, String mcNo, String mcAreaTyp, String mcNm, String errorCode) {
        String sql = """
            INSERT INTO public.mc_sta_mst_view (mc_typ, mc_no, mc_area_typ, mc_nm, error_code)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (mc_typ, mc_no) DO UPDATE SET
              mc_area_typ = EXCLUDED.mc_area_typ,
              mc_nm = EXCLUDED.mc_nm,
              error_code = EXCLUDED.error_code,
              upd_dt = CURRENT_TIMESTAMP,
              upd_user_id = 'etl'
        """;
        return postgresqlJdbcTemplate.update(sql, mcTyp, mcNo, mcAreaTyp, mcNm, errorCode);
    }
}


