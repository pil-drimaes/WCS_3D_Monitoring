package com.example.cdcqueue.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정 클래스
 * 
 * STOMP(Simple Text Oriented Messaging Protocol) 기반의 WebSocket 설정을 제공합니다.
 * 클라이언트와 서버 간의 실시간 양방향 통신을 위한 설정을 담당합니다.
 * 
 * 주요 기능:
 * - WebSocket 엔드포인트 설정 (/ws)
 * - STOMP 메시지 브로커 설정 (/topic)
 * - CORS 설정 (Cross-Origin Resource Sharing)
 * - SockJS 지원 (WebSocket 폴백)
 * 
 * 설정 내용:
 * - 메시지 브로커: /topic (클라이언트 구독용)
 * - 애플리케이션 프리픽스: /app (클라이언트에서 서버로 메시지 전송용)
 * - WebSocket 엔드포인트: /ws (SockJS 지원)
 * - CORS: 모든 origin 허용 (개발용)
 * 
 * @author AGV Monitoring System
 * @version 1.0
 */
@Configuration
@EnableWebSocketMessageBroker  // STOMP 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    /**
     * 메시지 브로커 설정
     * 
     * 클라이언트가 구독할 수 있는 토픽과 애플리케이션 프리픽스를 설정합니다.
     * 
     * @param config 메시지 브로커 레지스트리
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 수 있는 토픽 설정
        // /topic/agv-updates 형태로 이벤트를 전송할 수 있음
        config.enableSimpleBroker("/topic");
        
        // 클라이언트에서 서버로 메시지를 보낼 때 사용할 프리픽스
        // /app/message 형태로 서버에 메시지를 전송할 수 있음
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * STOMP 엔드포인트 등록
     * 
     * WebSocket 연결을 위한 엔드포인트를 설정합니다.
     * SockJS를 지원하여 WebSocket을 지원하지 않는 환경에서도 동작합니다.
     * 
     * @param registry STOMP 엔드포인트 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")                    // WebSocket 연결 엔드포인트
                .setAllowedOriginPatterns("*")         // 모든 origin 허용 (개발용)
                .withSockJS();                         // SockJS 지원 (폴백)
    }
}