package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.mushiny.MushinyPodInfoRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemMushinyPodRepository {

    private final JdbcTemplate postgresqlJdbcTemplate;

    public SystemMushinyPodRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }

    public boolean isConnected() { try { postgresqlJdbcTemplate.queryForObject("SELECT 1", Integer.class); return true; } catch (Exception e) { return false; } }

    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int upsert(MushinyPodInfoRecord r) {
        String sql = """
            INSERT INTO public.mushiny_pod_info (
              uuid, pod_id, section_id, zone_code, location, pod_direction,
              pos_x, pos_y, ins_dt, ins_user_id, upd_dt, upd_user_id
            ) VALUES (
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
        """;
        return postgresqlJdbcTemplate.update(sql,
            r.getUuid(), r.getPodId(), r.getSectionId(), r.getZoneCode(), r.getLocation(), r.getPodDirection(),
            r.getPosX(), r.getPosY(), r.getInsDt(), r.getInsUserId(), r.getUpdDt(), r.getUpdUserId()
        );
    }
} 