package com.example.WCS_DataStream.etl.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플리케이션 시작 시 모든 ETL 스케줄러를 초기화하는 컴포넌트
 * 
 * @author WCS Monitoring System
 * @version 2.0
 */
@Component
public class SchedulerInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(SchedulerInitializer.class);
    
    private final List<BaseETLScheduler<?>> schedulers;
    
    @Autowired
    public SchedulerInitializer(List<BaseETLScheduler<?>> schedulers) {
        this.schedulers = schedulers;
    }
    
    /**
     * 애플리케이션이 완전히 시작된 후 모든 스케줄러 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAllSchedulers() {
        log.info("=== 애플리케이션 시작 시 ETL 스케줄러 초기화 시작 ===");
        try {
            for (BaseETLScheduler<?> scheduler : schedulers) {
                try {
                    scheduler.initializeOnStartup();
                } catch (Exception e) {
                    log.error("스케줄러 초기화 중 오류 발생 ({}): {}", scheduler.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
            log.info("=== ETL 스케줄러 초기화 완료 (총 {}개) ===", schedulers.size());
        } catch (Exception e) {
            log.error("스케줄러 초기화 루프 오류: {}", e.getMessage(), e);
        }
    }
} 