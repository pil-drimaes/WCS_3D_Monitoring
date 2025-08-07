package com.example.cdcqueue.etl.service;

import com.example.cdcqueue.common.model.CdcEvent;
import com.example.cdcqueue.common.queue.EventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Queue;

/**
 * WebSocket Push Task
 * 
 * 이벤트 큐에 저장된 CDC 이벤트들을 WebSocket을 통해 클라이언트에 전송하는 스케줄링 태스크
 * 3초마다 실행되어 큐에 있는 모든 이벤트를 일괄적으로 클라이언트에 푸시합니다.
 * 
 * 주요 기능:
 * - 3초마다 이벤트 큐에서 모든 이벤트 추출
 * - WebSocket을 통해 클라이언트에 이벤트 전송
 * - 배치 처리를 통한 효율적인 네트워크 사용
 * 
 * 동작 방식:
 * 1. 이벤트 큐에서 모든 이벤트를 일괄 추출 (drainAll)
 * 2. 추출된 이벤트가 있으면 WebSocket으로 전송
 * 3. 클라이언트는 /topic/agv-updates 토픽을 구독하여 이벤트 수신
 * 
 * 성능 최적화:
 * - 배치 처리로 네트워크 오버헤드 최소화
 * - 3초 간격으로 실시간성과 효율성 균형
 * - 큐가 비어있으면 전송하지 않아 불필요한 네트워크 트래픽 방지
 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
@Component
public class WebSocketPushTask {
    
    /**
     * 로거
     */
    private static final Logger log = LoggerFactory.getLogger(WebSocketPushTask.class);

    /**
     * 이벤트 큐 (CDC 이벤트들이 저장됨)
     */
    private final EventQueue queue;
    
    /**
     * WebSocket 메시징 템플릿 (클라이언트에 메시지 전송)
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 생성자
     * 
     * @param queue 이벤트 큐
     * @param messagingTemplate WebSocket 메시징 템플릿
     */
    public WebSocketPushTask(EventQueue queue, SimpMessagingTemplate messagingTemplate) {
        this.queue = queue;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * WebSocket 클라이언트에 이벤트 푸시 (즉시 실행)
     * 
     * 이벤트 큐에 저장된 모든 CDC 이벤트를 WebSocket을 통해 클라이언트에 즉시 전송합니다.
     * 실시간성을 위해 배치 처리를 제거하고 즉시 전송합니다.
     */
    @Scheduled(fixedRate = 100) // 0.1초마다 실행 (실시간성 향상)
    public void pushToWebClients() {
        // 이벤트 큐에서 모든 이벤트를 일괄 추출 (큐는 비워짐)
        Queue<CdcEvent> drained = queue.drainAll();
        
        // 추출된 이벤트가 있으면 WebSocket으로 즉시 전송
        if (!drained.isEmpty()) {
            log.debug("Pushing {} AGV event(s) to /topic/agv-updates", drained.size());
            
            // WebSocket을 통해 /topic/agv-updates 토픽으로 이벤트 즉시 전송
            // 클라이언트는 이 토픽을 구독하여 실시간으로 이벤트를 수신
            messagingTemplate.convertAndSend("/topic/agv-updates", drained);
        }
    }
}
