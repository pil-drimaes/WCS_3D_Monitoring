package com.example.WCS_DataStream.etl.model.vendor.mushiny;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class MushinyAgvInfoRecord {
    private String uuid;
    private String robotNo;
    private String zoneCode;
    private String nodeId;
    private String directionFront;
    private String podId;
    private String podDirection;
    private Integer status;
    private String manual;
    private BigDecimal battery;
    private BigDecimal posX;
    private BigDecimal posY;
    private String hasPod;
    private Timestamp insDt;
    private String insUserId;
    private Timestamp updDt;
    private String updUserId;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getRobotNo() { return robotNo; }
    public void setRobotNo(String robotNo) { this.robotNo = robotNo; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getDirectionFront() { return directionFront; }
    public void setDirectionFront(String directionFront) { this.directionFront = directionFront; }
    public String getPodId() { return podId; }
    public void setPodId(String podId) { this.podId = podId; }
    public String getPodDirection() { return podDirection; }
    public void setPodDirection(String podDirection) { this.podDirection = podDirection; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getManual() { return manual; }
    public void setManual(String manual) { this.manual = manual; }
    public BigDecimal getBattery() { return battery; }
    public void setBattery(BigDecimal battery) { this.battery = battery; }
    public BigDecimal getPosX() { return posX; }
    public void setPosX(BigDecimal posX) { this.posX = posX; }
    public BigDecimal getPosY() { return posY; }
    public void setPosY(BigDecimal posY) { this.posY = posY; }
    public String getHasPod() { return hasPod; }
    public void setHasPod(String hasPod) { this.hasPod = hasPod; }
    public Timestamp getInsDt() { return insDt; }
    public void setInsDt(Timestamp insDt) { this.insDt = insDt; }
    public String getInsUserId() { return insUserId; }
    public void setInsUserId(String insUserId) { this.insUserId = insUserId; }
    public Timestamp getUpdDt() { return updDt; }
    public void setUpdDt(Timestamp updDt) { this.updDt = updDt; }
    public String getUpdUserId() { return updUserId; }
    public void setUpdUserId(String updUserId) { this.updUserId = updUserId; }
} 