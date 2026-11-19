package org.example.springboot.ai;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.service.AiChatSessionService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 非遗智能助手AI服务
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeritageAssistantService {

    private final DeepSeekService deepSeekService;

    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private AiChatSessionService sessionService;

    /**
     * 流式对话（带会话记忆）
     * 
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @return Flux流式响应
     */
    public Flux<String> chatStream(String userId,String sessionId, String userMessage) {
        log.info("开始AI对话，sessionId: {}, userMessage: {}", sessionId, userMessage);

        // 保存用户消息
        sessionService.saveMessage(sessionId, "user", userMessage);
        // 使用DeepSeek服务获取流式响应
        Flux<String> responseFlux = deepSeekService.sendMessageStream(
                userId,
                PromptManage.HERITAGE_ASSISTANT_PROMPT,
                userMessage
        );

        // 收集完整响应并保存
        StringBuilder fullResponse = new StringBuilder();
        return responseFlux
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    String assistantMessage = fullResponse.toString();
                    sessionService.saveMessage(sessionId, "assistant", assistantMessage);
                    log.info("AI对话完成，sessionId: {}, 响应长度: {}", sessionId, assistantMessage.length());
                })
                .doOnError(error -> {
                    log.error("AI对话失败，sessionId: {}", sessionId, error);
                });
    }

    /**
     * 非流式对话（一次性返回）
     * 
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @return AI响应内容
     */
    public String chat(String sessionId, String userMessage) {
        log.info("开始AI对话（非流式），sessionId: {}, userMessage: {}", sessionId, userMessage);
        
        // 保存用户消息
        sessionService.saveMessage(sessionId, "user", userMessage);
        
        try {
            // 使用DeepSeek服务获取响应
            String assistantMessage = deepSeekService.sendMessage(
                    "1",
                    PromptManage.HERITAGE_ASSISTANT_PROMPT,
                    userMessage
            );
            
            // 保存AI响应
            sessionService.saveMessage(sessionId, "assistant", assistantMessage);
            log.info("AI对话完成（非流式），sessionId: {}, 响应长度: {}", sessionId, assistantMessage.length());
            
            return assistantMessage;
            
        } catch (Exception e) {
            log.error("AI对话失败，sessionId: {}", sessionId, e);
            throw new RuntimeException("AI服务调用失败：" + e.getMessage(), e);
        }
    }
}

