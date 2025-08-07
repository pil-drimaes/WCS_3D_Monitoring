package com.example.cdcqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**

 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties 
public class CdcQueueApplication {
    
    /**
     * 
     * @param args 명령행 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(CdcQueueApplication.class, args);
    }
}
