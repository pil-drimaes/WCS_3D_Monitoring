package com.example.cdcqueue.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 재고 정보 데이터 모델
 * 
 * 재고 정보 테이블에 대한 엔티티 클래스
 * 
 * 테이블 구조:
 * - uuid_no: UUID 번호 (String(50))
 * - inventory: 재고번호 (String(50))
 * - batch_num: 배치번호 (String(50))
 * - unitload: 정면 트트번호/사이드 location 번호 (String(50))
 * - sku: SKU (String(50))
 * - pre_qty: 이전수량 (Integer)
 * - new_qty: 현재수량 (Integer)
 * - origin_order: 관련오더번호 (String(50))
 * - status: 상태 (Integer) - 0: normal, 1: locked
 * - report_time: 보고시간 (Long)
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryInfo {
    
    /**
     * UUID 번호 (고유 식별자)
     * 예: f48ac10b-58cc-4372-a567-0e02b2c3d479
     */
    private String uuidNo;
    
    /**
     * 재고번호
     * 예: WH-A01-12-3
     */
    private String inventory;
    
    /**
     * 배치번호
     * 예: 20250615-BATCH01
     */
    private String batchNum;
    
    /**
     * 정면 트트번호 피드백, 사이드는 location 번호 피드백
     * 예: UL001
     */
    private String unitload;
    
    /**
     * SKU
     * 예: SKU-8859476325
     */
    private String sku;
    
    /**
     * 이전수량
     * 예: 150
     */
    private Integer preQty;
    
    /**
     * 현재수량
     * 예: 135
     */
    private Integer newQty;
    
    /**
     * 관련오더번호
     * 예: SO-20250615-0012
     */
    private String originOrder;
    
    /**
     * 상태 (0: normal, 1: locked)
     * 예: 0
     */
    private Integer status;
    
    /**
     * 보고시간 (Unix timestamp)
     * 예: 1754565354000
     */
    private Long reportTime;
    
    /**
     * 재고 정보 생성자 (UUID 제외)
     * 
     * @param inventory 재고번호
     * @param batchNum 배치번호
     * @param unitload 정면 트트번호/사이드 location 번호
     * @param sku SKU
     * @param preQty 이전수량
     * @param newQty 현재수량
     * @param originOrder 관련오더번호
     * @param status 상태
     * @param reportTime 보고시간
     */
    public InventoryInfo(String inventory, String batchNum, String unitload, String sku,
                        Integer preQty, Integer newQty, String originOrder, Integer status, Long reportTime) {
        this.inventory = inventory;
        this.batchNum = batchNum;
        this.unitload = unitload;
        this.sku = sku;
        this.preQty = preQty;
        this.newQty = newQty;
        this.originOrder = originOrder;
        this.status = status;
        this.reportTime = reportTime;
    }
} 