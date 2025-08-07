package com.example.cdcqueue.etl.engine;

import com.example.cdcqueue.common.model.AgvData;
import java.util.List;

/**
 * 데이터 풀링 엔진 인터페이스
 * 
 * 다양한 데이터 소스에서 AGV 데이터를 가져오는 엔진의 공통 인터페이스
 * 전략 패턴을 사용하여 다양한 풀링 전략을 구현할 수 있습니다.
 * 
 * 구현 클래스:
 * - FullDataPullingEngine: 전체 데이터 비교 방식
 * - ConditionalPullingEngine: 조건부 쿼리 방식  
 * - HybridPullingEngine: 하이브리드 방식
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public interface DataPullingEngine {
    
    /**
     * 새로운 AGV 데이터를 풀링합니다.
     * 
     * @return 새로 감지된 AGV 데이터 리스트
     */
    List<AgvData> pullNewData();
    
    /**
     * 엔진 초기화
     * 
     * @param config 엔진 설정 정보
     */
    void initialize(PullingEngineConfig config);
    
    /**
     * 엔진 상태 확인
     * 
     * @return 엔진이 정상 동작 중인지 여부
     */
    boolean isHealthy();
    
    /**
     * 마지막 풀링 시간 조회
     * 
     * @return 마지막 풀링 시간
     */
    long getLastPullTime();
} 