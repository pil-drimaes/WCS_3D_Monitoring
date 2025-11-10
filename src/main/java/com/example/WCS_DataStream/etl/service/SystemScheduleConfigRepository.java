package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.config.EtlScheduleConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class SystemScheduleConfigRepository {

	private final JdbcTemplate postgresqlJdbcTemplate;

	public SystemScheduleConfigRepository(@Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate) {
		this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
	}

    @PostConstruct
    public void ensureTableExists() {
        try {
            String ddl = """
                CREATE TABLE IF NOT EXISTS public.etl_scheduler_config (
                    domain               VARCHAR(64) PRIMARY KEY,
                    enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
                    interval_ms          BIGINT       NOT NULL DEFAULT 1000,
                    initial_delay_ms     BIGINT       NOT NULL DEFAULT 0,
                    description          TEXT,
                    upd_id               VARCHAR(64),
                    upd_dt               TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
                    uuid_id              VARCHAR(64)
                )
                """;
            postgresqlJdbcTemplate.execute(ddl);
        } catch (Exception ignore) {
        }
    }

	public EtlScheduleConfig getByDomain(String domain) {
        String sql = "SELECT domain, enabled, interval_ms, initial_delay_ms, description, upd_id, upd_dt, uuid_id FROM public.etl_scheduler_config WHERE domain = ?";
		List<EtlScheduleConfig> list = postgresqlJdbcTemplate.query(sql, ps -> ps.setString(1, domain), this::mapRow);
		return list.isEmpty() ? null : list.get(0);
	}

    public void ensureDomainRow(String domain, long intervalMs, long initialDelayMs, String description) {
        try {
            String check = "SELECT COUNT(1) FROM public.etl_scheduler_config WHERE domain = ?";
            Integer cnt = postgresqlJdbcTemplate.queryForObject(check, Integer.class, domain);
            if (cnt != null && cnt > 0) return;
            String ins = """
                INSERT INTO public.etl_scheduler_config (domain, enabled, interval_ms, initial_delay_ms, description, upd_id)
                VALUES (?, TRUE, ?, ?, ?, 'etl')
            """;
            postgresqlJdbcTemplate.update(ins, domain, intervalMs, initialDelayMs, description);
        } catch (Exception ignore) {
        }
    }

	private EtlScheduleConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
		EtlScheduleConfig c = new EtlScheduleConfig();
		c.setDomain(rs.getString("domain"));
		c.setEnabled(rs.getBoolean("enabled"));
		c.setIntervalMs(rs.getLong("interval_ms"));
		c.setInitialDelayMs(rs.getLong("initial_delay_ms"));
		c.setDescription(rs.getString("description"));
		c.setUpdId(rs.getString("upd_id"));
		java.sql.Timestamp ts = rs.getTimestamp("upd_dt");
		c.setUpdDt(ts == null ? null : ts.toInstant().atOffset(java.time.ZoneOffset.UTC));
		c.setUuidId(rs.getString("uuid_id"));
		return c;
	}
}


