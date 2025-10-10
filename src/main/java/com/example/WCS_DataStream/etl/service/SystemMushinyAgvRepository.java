package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.mushiny.MushinyAgvInfoRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemMushinyAgvRepository {

    private final JdbcTemplate postgresqlJdbcTemplate;

    public SystemMushinyAgvRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }

    public boolean isConnected() {
        try { postgresqlJdbcTemplate.queryForObject("SELECT 1", Integer.class); return true; } catch (Exception e) { return false; }
    }

    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int upsert(MushinyAgvInfoRecord r) {
        String sql = """
            INSERT INTO public.mushiny_agv_info (
              uuid, robot_no, zone_code, node_id, direction_front, pod_id, pod_direction,
              status, manual, battery, pos_x, pos_y, has_pod, ins_dt, ins_user_id, upd_dt, upd_user_id
            ) VALUES (
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            ON CONFLICT (uuid) DO UPDATE SET
              robot_no = EXCLUDED.robot_no,
              zone_code = EXCLUDED.zone_code,
              node_id = EXCLUDED.node_id,
              direction_front = EXCLUDED.direction_front,
              pod_id = EXCLUDED.pod_id,
              pod_direction = EXCLUDED.pod_direction,
              status = EXCLUDED.status,
              manual = EXCLUDED.manual,
              battery = EXCLUDED.battery,
              pos_x = EXCLUDED.pos_x,
              pos_y = EXCLUDED.pos_y,
              has_pod = EXCLUDED.has_pod,
              upd_dt = EXCLUDED.upd_dt,
              upd_user_id = EXCLUDED.upd_user_id
        """;
        return postgresqlJdbcTemplate.update(sql,
            r.getUuid(), r.getRobotNo(), r.getZoneCode(), r.getNodeId(), r.getDirectionFront(), r.getPodId(), r.getPodDirection(),
            r.getStatus(), r.getManual(), r.getBattery(), r.getPosX(), r.getPosY(), r.getHasPod(), r.getInsDt(), r.getInsUserId(), r.getUpdDt(), r.getUpdUserId()
        );
    }
} 