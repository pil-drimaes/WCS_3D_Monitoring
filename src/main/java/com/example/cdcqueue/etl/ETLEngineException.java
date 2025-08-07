package com.example.cdcqueue.etl;

/**
 * ETL 엔진 처리 중 발생하는 예외
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public class ETLEngineException extends Exception {
    
    public ETLEngineException(String message) {
        super(message);
    }
    
    public ETLEngineException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ETLEngineException(Throwable cause) {
        super(cause);
    }
} 