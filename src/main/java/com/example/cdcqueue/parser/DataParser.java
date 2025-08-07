package com.example.cdcqueue.parser;

import com.example.cdcqueue.common.model.AgvData;
import java.util.List;

/**
 * 데이터 파서 인터페이스
 * 
 * 다양한 형태의 원시 데이터를 AGV 데이터로 변환하는 파서의 공통 인터페이스
 * 
 * 구현 클래스:
 * - SqlDataParser: SQL 결과를 AGV 데이터로 변환
 * - JsonDataParser: JSON 데이터를 AGV 데이터로 변환
 * - CsvDataParser: CSV 데이터를 AGV 데이터로 변환
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public interface DataParser<T> {
    
    /**
     * 원시 데이터를 AGV 데이터로 파싱
     * 
     * @param rawData 파싱할 원시 데이터
     * @return 파싱된 AGV 데이터 리스트
     * @throws DataParseException 파싱 중 오류 발생 시
     */
    List<AgvData> parse(T rawData) throws DataParseException;
    
    /**
     * 파서가 지원하는 데이터 형식 확인
     * 
     * @param data 확인할 데이터
     * @return 지원 여부
     */
    boolean supports(T data);
    
    /**
     * 파서 초기화
     * 
     * @param config 파서 설정
     */
    void initialize(ParserConfig config);
} 