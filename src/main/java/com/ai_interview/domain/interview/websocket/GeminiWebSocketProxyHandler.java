package com.ai_interview.domain.interview.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GeminiWebSocketProxyHandler extends TextWebSocketHandler {

    private final String geminiApiKey;
    private final Map<String, WebSocketSession> clientToGeminiSessions = new ConcurrentHashMap<>();

    public GeminiWebSocketProxyHandler(@Value("${spring.ai.google.genai.api-key}") String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession clientSession) throws Exception {
        log.info("Client connected to local WebSocket proxy: {}", clientSession.getId());

        // Construct official Gemini Live API WebSocket endpoint URI
        String uriStr = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidirectionalGenerateContent?key=" + geminiApiKey;
        URI geminiUri = URI.create(uriStr);

        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        
        // Connect to Gemini Live
        webSocketClient.execute(new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession geminiSession) throws Exception {
                log.info("Successfully linked local client {} to Gemini Live session {}", clientSession.getId(), geminiSession.getId());
                clientToGeminiSessions.put(clientSession.getId(), geminiSession);
            }

            @Override
            public void handleMessage(WebSocketSession geminiSession, WebSocketMessage<?> message) throws Exception {
                // Forward message from Gemini back to the frontend client
                if (clientSession.isOpen()) {
                    clientSession.sendMessage(message);
                }
            }

            @Override
            public void handleTransportError(WebSocketSession geminiSession, Throwable exception) throws Exception {
                log.error("Gemini session transport error", exception);
                closeSessions(clientSession, geminiSession);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession geminiSession, CloseStatus status) throws Exception {
                log.info("Gemini Live session closed for client {}: {}", clientSession.getId(), status);
                closeSessions(clientSession, geminiSession);
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, geminiUri.toString());
    }

    @Override
    protected void handleTextMessage(WebSocketSession clientSession, TextMessage message) throws Exception {
        // Forward message from client to Gemini Live
        WebSocketSession geminiSession = clientToGeminiSessions.get(clientSession.getId());
        if (geminiSession != null && geminiSession.isOpen()) {
            geminiSession.sendMessage(message);
        } else {
            log.warn("Cannot forward: Gemini session is not open for client {}", clientSession.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession clientSession, Throwable exception) throws Exception {
        log.error("Client session transport error", exception);
        WebSocketSession geminiSession = clientToGeminiSessions.remove(clientSession.getId());
        closeSessions(clientSession, geminiSession);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession clientSession, CloseStatus status) throws Exception {
        log.info("Client WebSocket session closed: {}", clientSession.getId());
        WebSocketSession geminiSession = clientToGeminiSessions.remove(clientSession.getId());
        closeSessions(clientSession, geminiSession);
    }

    private void closeSessions(WebSocketSession clientSession, WebSocketSession geminiSession) {
        try {
            if (clientSession != null && clientSession.isOpen()) {
                clientSession.close();
            }
        } catch (IOException e) {
            log.warn("Error closing client session", e);
        }
        try {
            if (geminiSession != null && geminiSession.isOpen()) {
                geminiSession.close();
            }
        } catch (IOException e) {
            log.warn("Error closing Gemini session", e);
        }
    }
}
