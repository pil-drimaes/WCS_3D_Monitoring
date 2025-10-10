package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.ant.AntPodInfoRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAntPodRepository {

    private final JdbcTemplate postgresqlJdbcTemplate;

    public SystemAntPodRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }

    public boolean isConnected() { try { postgresqlJdbcTemplate.queryForObject("SELECT 1", Integer.class); return true; } catch (Exception e) { return false; } }

    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int upsert(AntPodInfoRecord r) {
        String sql = """
            INSERT INTO public.ant_pod_info (
              uuid, pod_id, pod_face, location, report_time, ins_dt, ins_user_id, upd_dt, upd_user_id
            ) VALUES (
              ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            ON CONFLICT (uuid) DO UPDATE SET
              pod_id = EXCLUDED.pod_id,
              pod_face = EXCLUDED.pod_face,
              location = EXCLUDED.location,
              report_time = EXCLUDED.report_time,
              upd_dt = EXCLUDED.upd_dt,
              upd_user_id = EXCLUDED.upd_user_id
        """;
        return postgresqlJdbcTemplate.update(sql,
            r.getUuid(), r.getPodId(), r.getPodFace(), r.getLocation(), r.getReportTime(), r.getInsDt(), r.getInsUserId(), r.getUpdDt(), r.getUpdUserId()
        );
    }
} 