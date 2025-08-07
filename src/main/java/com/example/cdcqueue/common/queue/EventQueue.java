package com.example.cdcqueue.common.queue;

import com.example.cdcqueue.common.model.CdcEvent;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 이벤트 큐 관리 클래스
 * 
 * CDC 이벤트들을 임시로 저장하고 관리하는 스레드 안전한 큐
 * CdcPullingTask에서 생성된 이벤트를 저장하고,
 * WebSocketPushTask에서 일괄적으로 가져와서 클라이언트에 전송합니다.
 * 
 * 주요 기능:
 * - 이벤트 추가 (add)
 * - 모든 이벤트 일괄 추출 (drainAll)
 * - 큐 상태 확인 (isEmpty)
 * 
 * 스레드 안전성:
 * - ConcurrentLinkedQueue 사용으로 다중 스레드 환경에서 안전
 * - CdcPullingTask(100ms)와 WebSocketPushTask(3초) 간의 동기화
 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
@Component
public class EventQueue {
    
    /**
     * 이벤트 저장용 스레드 안전 큐
     * ConcurrentLinkedQueue는 다중 스레드 환경에서 안전하게 동작
     */
    private final Queue<CdcEvent> queue = new ConcurrentLinkedQueue<>();

    /**
     * 이벤트를 큐에 추가
     * 
     * @param event 추가할 CDC 이벤트
     */
    public void add(CdcEvent event) {
        queue.add(event);
    }

    /**
     * 큐에 있는 모든 이벤트를 일괄 추출하고 큐를 비움
     * 
     * @return 추출된 모든 이벤트들의 큐
     */
    public Queue<CdcEvent> drainAll() {
        Queue<CdcEvent> drained = new ConcurrentLinkedQueue<>();
        while (!queue.isEmpty()) {
            drained.add(queue.poll());
        }
        return drained;
    }

    /**
     * 큐가 비어있는지 확인
     * 
     * @return 큐가 비어있으면 true, 아니면 false
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
