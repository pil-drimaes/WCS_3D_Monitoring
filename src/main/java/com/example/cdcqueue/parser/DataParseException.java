package com.example.cdcqueue.parser;

/**
 * 데이터 파싱 중 발생하는 예외
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public class DataParseException extends Exception {
    
    public DataParseException(String message) {
        super(message);
    }
    
    public DataParseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DataParseException(Throwable cause) {
        super(cause);
    }
} 