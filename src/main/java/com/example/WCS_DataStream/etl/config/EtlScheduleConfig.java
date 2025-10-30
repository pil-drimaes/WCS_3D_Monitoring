package com.example.WCS_DataStream.etl.config;

import java.time.OffsetDateTime;

public class EtlScheduleConfig {

	private String domain;
	private boolean enabled;
	private long intervalMs;
	private long initialDelayMs;
	private String description;
	private String updId;
	private OffsetDateTime updDt;
	private String uuidId;

	public String getDomain() { return domain; }
	public void setDomain(String domain) { this.domain = domain; }

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }

	public long getIntervalMs() { return intervalMs; }
	public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }

	public long getInitialDelayMs() { return initialDelayMs; }
	public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public String getUpdId() { return updId; }
	public void setUpdId(String updId) { this.updId = updId; }

	public OffsetDateTime getUpdDt() { return updDt; }
	public void setUpdDt(OffsetDateTime updDt) { this.updDt = updDt; }

	public String getUuidId() { return uuidId; }
	public void setUuidId(String uuidId) { this.uuidId = uuidId; }
}

