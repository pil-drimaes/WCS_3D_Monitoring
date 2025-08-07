package com.example.cdcqueue.etl;

import java.time.LocalDateTime;

/**
 * ETL 통계 정보 클래스
 * 
 * ETL 프로세스의 실행 통계를 담습니다.
 * 
 * @author AGV Monitoring System
 * @version 2.0
 */
public class ETLStatistics {
    
    /**
     * 총 처리된 레코드 수
     */
    private long totalProcessedRecords;
    
    /**
     * 성공적으로 처리된 레코드 수
     */
    private long successfulRecords;
    
    /**
     * 실패한 레코드 수
     */
    private long failedRecords;
    
    /**
     * 건너뛴 레코드 수
     */
    private long skippedRecords;
    
    /**
     * 총 실행 시간 (밀리초)
     */
    private long totalExecutionTime;
    
    /**
     * 평균 처리 시간 (밀리초)
     */
    private double averageProcessingTime;
    
    /**
     * 마지막 실행 시간
     */
    private LocalDateTime lastExecutionTime;
    
    /**
     * 오류 발생 횟수
     */
    private long errorCount;
    
    /**
     * 재시도 횟수
     */
    private long retryCount;
    
    // 생성자
    public ETLStatistics() {}
    
    // Getter/Setter 메서드들
    public long getTotalProcessedRecords() { return totalProcessedRecords; }
    public void setTotalProcessedRecords(long totalProcessedRecords) { this.totalProcessedRecords = totalProcessedRecords; }
    
    public long getSuccessfulRecords() { return successfulRecords; }
    public void setSuccessfulRecords(long successfulRecords) { this.successfulRecords = successfulRecords; }
    
    public long getFailedRecords() { return failedRecords; }
    public void setFailedRecords(long failedRecords) { this.failedRecords = failedRecords; }
    
    public long getSkippedRecords() { return skippedRecords; }
    public void setSkippedRecords(long skippedRecords) { this.skippedRecords = skippedRecords; }
    
    public long getTotalExecutionTime() { return totalExecutionTime; }
    public void setTotalExecutionTime(long totalExecutionTime) { this.totalExecutionTime = totalExecutionTime; }
    
    public double getAverageProcessingTime() { return averageProcessingTime; }
    public void setAverageProcessingTime(double averageProcessingTime) { this.averageProcessingTime = averageProcessingTime; }
    
    public LocalDateTime getLastExecutionTime() { return lastExecutionTime; }
    public void setLastExecutionTime(LocalDateTime lastExecutionTime) { this.lastExecutionTime = lastExecutionTime; }
    
    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
    
    public long getRetryCount() { return retryCount; }
    public void setRetryCount(long retryCount) { this.retryCount = retryCount; }
    
    /**
     * 성공률 계산
     */
    public double getSuccessRate() {
        if (totalProcessedRecords == 0) {
            return 0.0;
        }
        return (double) successfulRecords / totalProcessedRecords * 100.0;
    }
    
    /**
     * 실패률 계산
     */
    public double getFailureRate() {
        if (totalProcessedRecords == 0) {
            return 0.0;
        }
        return (double) failedRecords / totalProcessedRecords * 100.0;
    }
    
    /**
     * 통계 정보를 문자열로 반환
     */
    @Override
    public String toString() {
        return String.format(
            "ETL Statistics: Total=%d, Success=%d, Failed=%d, Skipped=%d, " +
            "SuccessRate=%.2f%%, AvgTime=%.2fms, Errors=%d, Retries=%d",
            totalProcessedRecords, successfulRecords, failedRecords, skippedRecords,
            getSuccessRate(), averageProcessingTime, errorCount, retryCount
        );
    }
} 