package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.ant.AntRobotInfoRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAgvRepository {

    private final JdbcTemplate postgresqlJdbcTemplate;

    public SystemAgvRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
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
    public int upsertAntRobotInfo(AntRobotInfoRecord r) {
        String sql = """
            INSERT INTO public.ant_robot_info (
              uuid, robot_no, robot_type, map_code, zone_code, status, manual, loaders,
              report_time, battery, node_id, pos_x, pos_y, speed, task_id, next_target,
              pod_id, ins_dt, ins_user_id, upd_dt, upd_user_id
            ) VALUES (
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            """;
        return postgresqlJdbcTemplate.update(sql,
            r.getUuid(), r.getRobotNo(), r.getRobotType(), r.getMapCode(), r.getZoneCode(), r.getStatus(), r.getManual(), r.getLoaders(),
            r.getReportTime(), r.getBattery(), r.getNodeId(), r.getPosX(), r.getPosY(), r.getSpeed(), r.getTaskId(), r.getNextTarget(),
            r.getPodId(), r.getInsDt(), r.getInsUserId(), r.getUpdDt(), r.getUpdUserId()
        );
    }
} 