package com.example.cdcqueue.etl;

import com.example.cdcqueue.common.model.AgvData;
import java.util.List;

/**
 * 데이터 ETL 엔진 인터페이스
 * 
 * Extract(추출), Transform(변환), Load(적재) 과정을 관리하는 엔진
 * 
 * 주요 기능:
 * - 데이터 추출 (Extract): 다양한 소스에서 데이터 가져오기
 * - 데이터 변환 (Transform): 데이터 정제, 변환, 검증
 * - 데이터 적재 (Load): 처리된 데이터를 목적지에 저장
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public interface DataETLEngine<T> {
    
    /**
     * ETL 프로세스 실행
     * 
     * @return 처리된 데이터 리스트
     * @throws ETLEngineException ETL 처리 중 오류 발생 시
     */
    List<T> executeETL() throws ETLEngineException;
    
    /**
     * ETL 엔진 초기화
     * 
     * @param config ETL 엔진 설정
     */
    void initialize(ETLConfig config);
    
    /**
     * ETL 엔진 상태 확인
     * 
     * @return 엔진이 정상 동작 중인지 여부
     */
    boolean isHealthy();
    
    /**
     * 마지막 ETL 실행 시간 조회
     * 
     * @return 마지막 실행 시간
     */
    long getLastExecutionTime();
    
    /**
     * 처리된 데이터 통계 조회
     * 
     * @return ETL 통계 정보
     */
    ETLStatistics getStatistics();
} 