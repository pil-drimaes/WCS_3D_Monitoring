package com.example.WCS_DataStream.etl.scheduler;

import com.example.WCS_DataStream.etl.config.ETLConfig;
import com.example.WCS_DataStream.etl.engine.ETLEngine;
import com.example.WCS_DataStream.etl.service.PostgreSQLDataService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 공통 ETL 스케줄러 베이스 클래스
 * 
 * 모든 ETL 스케줄러가 공통으로 사용하는 기능을 제공합니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public abstract class BaseETLScheduler<T> {
    
    protected static final Logger log = LoggerFactory.getLogger(BaseETLScheduler.class);
    
    /**
     * 초기화 완료 여부
     */
    protected boolean initialized = false;

    protected PostgreSQLDataService postgreSQLDataService;

    protected BaseETLScheduler(PostgreSQLDataService postgreSQLDataService) {
        this.postgreSQLDataService = postgreSQLDataService;
    }
    
    /**
     * 마지막 처리 시간 (중복 처리 방지용)
     */
    protected final AtomicReference<LocalDateTime> lastProcessedTime = new AtomicReference<>(null);
    
    /**
     * ETL 엔진
     */
    protected abstract ETLEngine<T> getETLEngine();
    
    /**
     * 스케줄러 이름
     */
    protected abstract String getSchedulerName();
    
    /**
     * 기본 ETL 설정 생성
     */
    protected ETLConfig createDefaultConfig() {
        return new ETLConfig();
    }
    
    /**
     * ETL 엔진 초기화
     */
    protected void initializeETL() {
        try {
            if (initialized) {
                return;
            }
            
            ETLConfig config = createDefaultConfig();
            getETLEngine().initialize(config, postgreSQLDataService);            
            initialized = true;
            log.info("{} ETL 엔진 초기화 완료", getSchedulerName());
            
        } catch (Exception e) {
            log.error("{} ETL 엔진 초기화 실패: {}", getSchedulerName(), e.getMessage(), e);
        }
    }
    
    /**
     * 초기 데이터 처리 (애플리케이션 시작 시 한 번만)
     */
    protected abstract void processInitialData();
    
    /**
     * 증분 데이터 처리 (변경된 데이터만)
     */
    protected abstract void processIncrementalData();
    
    /**
     * ETL 프로세스 실행 (공통 로직)
     */
    // @Scheduled(fixedRate = 100) // 0.1초마다 실행 (도메인별 주기 설정으로 이동)
    public void executeETLProcess() {
        try {
            if (!initialized) {
                initializeETL();
                processInitialData(); // 초기 데이터 처리
                return;
            }
            
            // 강제 재처리 모드인 경우 초기 데이터를 다시 처리
            if (forceReprocess) {
                log.info("{} 강제 재처리 모드: 초기 데이터를 다시 처리합니다", getSchedulerName());
                processInitialData(); // 초기 데이터 재처리
                disableForceReprocess(); // 강제 재처리 모드 비활성화
                log.info("{} 강제 재처리 완료, 일반 모드로 전환", getSchedulerName());
                return;
            }
            
            // 증분 데이터 처리
            processIncrementalData();
            
            // 마지막 처리 시간 업데이트
            lastProcessedTime.set(LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("{} ETL 프로세스 실행 중 오류: {}", getSchedulerName(), e.getMessage(), e);
        }
    }
    
    /**
     * ETL 엔진 상태 확인
     */
    public boolean isETLInitialized() {
        return initialized;
    }
    
    /**
     * 마지막 처리 시간 반환
     */
    public LocalDateTime getLastProcessedTime() {
        return lastProcessedTime.get();
    }
    
    /**
     * 스케줄러 캐시 초기화 (스케줄러 내부 상태 전용)
     */
    public abstract void clearSchedulerCache();
    
    /**
     * 강제 초기화 (재시작 시 사용)
     */
    public void forceReinitialize() {
        initialized = false;
        lastProcessedTime.set(null);
        clearSchedulerCache();
        log.info("{} 스케줄러 캐시 리셋 완료", getSchedulerName());
    }
    
    /**
     * 애플리케이션 시작 시 자동으로 호출되는 초기화
     */
    public void initializeOnStartup() {
        log.info("{} 애플리케이션 시작 시 초기화 시작", getSchedulerName());
        forceReinitialize();
        // 다음 executeETLProcess() 호출 시 전체 데이터를 다시 처리
    }
    
    /**
     * 강제 재처리 모드 (재시작 시 사용)
     */
    protected boolean forceReprocess = false;
    
    /**
     * 강제 재처리 모드 활성화
     */
    public void enableForceReprocess() {
        this.forceReprocess = true;
        log.info("{} 강제 재처리 모드 활성화", getSchedulerName());
    }
    
    /**
     * 강제 재처리 모드 비활성화
     */
    public void disableForceReprocess() {
        this.forceReprocess = false;
        log.info("{} 강제 재처리 모드 비활성화", getSchedulerName());
    }
} 