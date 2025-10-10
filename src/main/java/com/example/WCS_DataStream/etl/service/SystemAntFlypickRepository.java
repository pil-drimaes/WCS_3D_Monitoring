package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.ant.AntFlypickInfoRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAntFlypickRepository {

    private final JdbcTemplate postgresqlJdbcTemplate;

    public SystemAntFlypickRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
    }

    public boolean isConnected() {
        try { postgresqlJdbcTemplate.queryForObject("SELECT 1", Integer.class); return true; } catch (Exception e) { return false; }
    }

    @Transactional(transactionManager = "postgresqlTransactionManager")
    public int upsert(AntFlypickInfoRecord r) {
        String sql = """
            INSERT INTO public.ant_flypick_info (
              uuid, robot_no, robot_type, map_code, zone_code, status, manual,
              report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target,
              pod_id, ins_dt, ins_user_id, upd_dt, upd_user_id
            ) VALUES (
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            ON CONFLICT (uuid) DO UPDATE SET
              robot_no = EXCLUDED.robot_no,
              robot_type = EXCLUDED.robot_type,
              map_code = EXCLUDED.map_code,
              zone_code = EXCLUDED.zone_code,
              status = EXCLUDED.status,
              manual = EXCLUDED.manual,
              report_time = EXCLUDED.report_time,
              battery = EXCLUDED.battery,
              node_id = EXCLUDED.node_id,
              pos_x = EXCLUDED.pos_x,
              pos_y = EXCLUDED.pos_y,
              speed = EXCLUDED.speed,
              task_id = EXCLUDED.task_id,
              next_target = EXCLUDED.next_target,
              pod_id = EXCLUDED.pod_id,
              upd_dt = EXCLUDED.upd_dt,
              upd_user_id = EXCLUDED.upd_user_id
        """;
        return postgresqlJdbcTemplate.update(sql,
            r.getUuid(), r.getRobotNo(), r.getRobotType(), r.getMapCode(), r.getZoneCode(), r.getStatus(), r.getManual(),
            r.getReportTime(), r.getBattery(), r.getNodeId(), r.getPosX(), r.getPosY(), r.getSpeed(), r.getTaskId(), r.getNextTarget(),
            r.getPodId(), r.getInsDt(), r.getInsUserId(), r.getUpdDt(), r.getUpdUserId()
        );
    }
} 