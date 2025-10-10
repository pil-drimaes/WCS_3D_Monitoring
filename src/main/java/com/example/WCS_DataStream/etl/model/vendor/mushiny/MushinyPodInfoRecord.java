package com.example.WCS_DataStream.etl.model.vendor.mushiny;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class MushinyPodInfoRecord {
    private String uuid;
    private String podId;
    private Integer sectionId;
    private String zoneCode;
    private String location;
    private String podDirection;
    private BigDecimal posX;
    private BigDecimal posY;
    private Timestamp insDt;
    private String insUserId;
    private Timestamp updDt;
    private String updUserId;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getPodId() { return podId; }
    public void setPodId(String podId) { this.podId = podId; }
    public Integer getSectionId() { return sectionId; }
    public void setSectionId(Integer sectionId) { this.sectionId = sectionId; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPodDirection() { return podDirection; }
    public void setPodDirection(String podDirection) { this.podDirection = podDirection; }
    public BigDecimal getPosX() { return posX; }
    public void setPosX(BigDecimal posX) { this.posX = posX; }
    public BigDecimal getPosY() { return posY; }
    public void setPosY(BigDecimal posY) { this.posY = posY; }
    public Timestamp getInsDt() { return insDt; }
    public void setInsDt(Timestamp insDt) { this.insDt = insDt; }
    public String getInsUserId() { return insUserId; }
    public void setInsUserId(String insUserId) { this.insUserId = insUserId; }
    public Timestamp getUpdDt() { return updDt; }
    public void setUpdDt(Timestamp updDt) { this.updDt = updDt; }
    public String getUpdUserId() { return updUserId; }
    public void setUpdUserId(String updUserId) { this.updUserId = updUserId; }
} 