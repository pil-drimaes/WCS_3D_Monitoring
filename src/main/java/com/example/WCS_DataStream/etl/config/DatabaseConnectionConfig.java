package com.example.WCS_DataStream.etl.config;

/**
 * 데이터베이스 연결 설정 정보 클래스
 * 
 * WCS DB 연결을 위한 설정 정보를 담는 POJO 클래스입니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public class DatabaseConnectionConfig {
    
    /**
     * 데이터베이스 URL
     */
    private String url;
    
    /**
     * 사용자명
     */
    private String username;
    
    /**
     * 비밀번호
     */
    private String password;
    
    /**
     * 드라이버 클래스명
     */
    private String driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    
    /**
     * 최대 연결 수
     */
    private int maxConnections = 10;
    
    /**
     * 최소 연결 수
     */
    private int minConnections = 2;
    
    /**
     * 연결 타임아웃 (초)
     */
    private int connectionTimeout = 30;
    
    /**
     * 쿼리 타임아웃 (초)
     */
    private int queryTimeout = 60;
    
    // 생성자
    public DatabaseConnectionConfig() {}
    
    public DatabaseConnectionConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
    
    // Getter/Setter 메서드들
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
    
    public int getMinConnections() { return minConnections; }
    public void setMinConnections(int minConnections) { this.minConnections = minConnections; }
    
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    
    public int getQueryTimeout() { return queryTimeout; }
    public void setQueryTimeout(int queryTimeout) { this.queryTimeout = queryTimeout; }
} 