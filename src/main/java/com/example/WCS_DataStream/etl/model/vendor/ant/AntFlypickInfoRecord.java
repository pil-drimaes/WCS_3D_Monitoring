package com.example.WCS_DataStream.etl.model.vendor.ant;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class AntFlypickInfoRecord {
    private String uuid;
    private String robotNo;
    private String robotType;
    private String mapCode;
    private String zoneCode;
    private Integer status;
    private String manual;
    private String reportTime;
    private BigDecimal battery;
    private String nodeId;
    private BigDecimal posX;
    private BigDecimal posY;
    private BigDecimal speed;
    private String taskId;
    private String nextTarget;
    private String podId;
    private Timestamp insDt;
    private String insUserId;
    private Timestamp updDt;
    private String updUserId;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getRobotNo() { return robotNo; }
    public void setRobotNo(String robotNo) { this.robotNo = robotNo; }
    public String getRobotType() { return robotType; }
    public void setRobotType(String robotType) { this.robotType = robotType; }
    public String getMapCode() { return mapCode; }
    public void setMapCode(String mapCode) { this.mapCode = mapCode; }
    public String getZoneCode() { return zoneCode; }
    public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getManual() { return manual; }
    public void setManual(String manual) { this.manual = manual; }
    public String getReportTime() { return reportTime; }
    public void setReportTime(String reportTime) { this.reportTime = reportTime; }
    public BigDecimal getBattery() { return battery; }
    public void setBattery(BigDecimal battery) { this.battery = battery; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public BigDecimal getPosX() { return posX; }
    public void setPosX(BigDecimal posX) { this.posX = posX; }
    public BigDecimal getPosY() { return posY; }
    public void setPosY(BigDecimal posY) { this.posY = posY; }
    public BigDecimal getSpeed() { return speed; }
    public void setSpeed(BigDecimal speed) { this.speed = speed; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getNextTarget() { return nextTarget; }
    public void setNextTarget(String nextTarget) { this.nextTarget = nextTarget; }
    public String getPodId() { return podId; }
    public void setPodId(String podId) { this.podId = podId; }
    public Timestamp getInsDt() { return insDt; }
    public void setInsDt(Timestamp insDt) { this.insDt = insDt; }
    public String getInsUserId() { return insUserId; }
    public void setInsUserId(String insUserId) { this.insUserId = insUserId; }
    public Timestamp getUpdDt() { return updDt; }
    public void setUpdDt(Timestamp updDt) { this.updDt = updDt; }
    public String getUpdUserId() { return updUserId; }
    public void setUpdUserId(String updUserId) { this.updUserId = updUserId; }
} 