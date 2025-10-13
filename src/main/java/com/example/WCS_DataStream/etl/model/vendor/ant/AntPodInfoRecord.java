package com.example.WCS_DataStream.etl.model.vendor.ant;

import java.sql.Timestamp;

public class AntPodInfoRecord {
    private String uuid;
    private String podId;
    private String podFace;
    private String location;
    private String reportTime;
    private Timestamp insDt;
    private String insUserId;
    private Timestamp updDt;
    private String updUserId;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getPodId() { return podId; }
    public void setPodId(String podId) { this.podId = podId; }
    public String getPodFace() { return podFace; }
    public void setPodFace(String podFace) { this.podFace = podFace; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getReportTime() { return reportTime; }
    public void setReportTime(String reportTime) { this.reportTime = reportTime; }
    public Timestamp getInsDt() { return insDt; }
    public void setInsDt(Timestamp insDt) { this.insDt = insDt; }
    public String getInsUserId() { return insUserId; }
    public void setInsUserId(String insUserId) { this.insUserId = insUserId; }
    public Timestamp getUpdDt() { return updDt; }
    public void setUpdDt(Timestamp updDt) { this.updDt = updDt; }
    public String getUpdUserId() { return updUserId; }
    public void setUpdUserId(String updUserId) { this.updUserId = updUserId; }
} 