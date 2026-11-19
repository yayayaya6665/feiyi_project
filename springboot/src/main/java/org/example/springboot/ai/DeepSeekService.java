//
//package org.example.springboot.ai;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.util.*;
//
///**
// * DeepSeek AI服务类（支持真正的流式输出）
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class DeepSeekService {
//
//    private final DeepSeekConfig deepSeekConfig;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    // 构建WebClient（非阻塞、支持流式）
//    private final WebClient webClient = WebClient.builder().build();
//
//    /**
//     * 普通非流式调用（保留原有功能）
//     */
//    public String sendMessage(String systemPrompt, String userMessage) {
//        try {
//            log.info("发送消息到DeepSeek API（非流式）");
//
//            // 构建请求体
//            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userMessage, false);
//
//            // 发送请求
//            String response = webClient.post()
//                    .uri(deepSeekConfig.getBaseUrl() + "/v1/chat/completions")
//                    .headers(this::setRequestHeaders)
//                    .body(Mono.just(requestBody), Map.class)
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .block(); // 阻塞获取结果（非流式场景）
//
//            // 解析响应
//            JsonNode rootNode = objectMapper.readTree(response);
//            String aiResponse = rootNode.path("choices").get(0).path("message").path("content").asText();
//
//            log.info("DeepSeek API返回成功，响应长度: {}", aiResponse.length());
//            return aiResponse;
//
//        } catch (Exception e) {
//            log.error("调用DeepSeek API失败: {}", e.getMessage(), e);
//            throw new RuntimeException("DeepSeek AI服务调用失败: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 真正的流式调用（核心改造方法）
//     */
//    public Flux<String> sendMessageStream(String systemPrompt, String userMessage) {
//        log.info("发送消息到DeepSeek API（流式模式）");
//
//        // 构建流式请求体（stream=true）
//        Map<String, Object> requestBody = buildRequestBody(systemPrompt, userMessage, true);
//
//        // 发送流式请求并处理响应
//        return webClient.post()
//                .uri(deepSeekConfig.getBaseUrl() + "/v1/chat/completions")
//                .headers(this::setRequestHeaders)
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(Mono.just(requestBody), Map.class)
//                .retrieve()
//                // 将响应体按行分割（SSE分块是每行一个data:xxx）
//                .bodyToFlux(String.class)
//                // 过滤空行和结束标记
//                .filter(line -> line != null && !line.isEmpty() && !line.equals("data: [DONE]"))
//                // 解析每行的SSE数据
//                .map(this::parseSseDataLine)
//                // 过滤空内容（避免返回空字符串）
//                .filter(content -> content != null && !content.isEmpty())
//                // 异常处理
//                .doOnError(e -> log.error("流式调用DeepSeek API失败: {}", e.getMessage(), e))
//                .onErrorResume(e -> Flux.just("流式响应出错: " + e.getMessage()));
//
//    }
//
//    /**
//     * 构建请求体（复用非流式/流式逻辑）
//     */
//    private Map<String, Object> buildRequestBody(String systemPrompt, String userMessage, boolean stream) {
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", deepSeekConfig.getModel());
//        requestBody.put("temperature", 0.7);
//        requestBody.put("max_tokens", 2000);
//        requestBody.put("stream", stream); // 关键：流式开关
//
//        // 构建消息列表
//        List<Map<String, String>> messages = new ArrayList<>();
//        // 添加系统消息
//        if (systemPrompt != null && !systemPrompt.isEmpty()) {
//            Map<String, String> systemMsg = new HashMap<>();
//            systemMsg.put("role", "system");
//            systemMsg.put("content", systemPrompt);
//            messages.add(systemMsg);
//        }
//        // 添加用户消息
//        Map<String, String> userMsg = new HashMap<>();
//        userMsg.put("role", "user");
//        userMsg.put("content", userMessage);
//        messages.add(userMsg);
//
//        requestBody.put("messages", messages);
//        return requestBody;
//    }
//
//    /**
//     * 设置请求头
//     */
//    private void setRequestHeaders(HttpHeaders headers) {
//        headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());
//        headers.setContentType(MediaType.APPLICATION_JSON);
//    }
//
//    /**
//     * 解析SSE单行数据（格式：data: {"id":"xxx","choices":[{"delta":{"content":"xxx"}}]}
//     */
//    private String parseSseDataLine(String line) {
//        try {
//            // 去掉前缀 "data: "
//            String jsonStr = line.startsWith("data: ") ? line.substring(6) : line;
//            if (jsonStr.isEmpty() || jsonStr.equals("[DONE]")) {
//                return "";
//            }
//            // 解析JSON
//            JsonNode rootNode = objectMapper.readTree(jsonStr);
//            // 提取流式返回的content（注意：流式返回的是delta而非完整message）
//            JsonNode contentNode = rootNode.path("choices")
//                    .get(0)
//                    .path("delta")
//                    .path("content");
//            return contentNode.isMissingNode() ? "" : contentNode.asText();
//        } catch (Exception e) {
//            log.warn("解析SSE数据失败: {}, 原始数据: {}", e.getMessage(), line);
//            return "";
//        }
//    }
//}

package org.example.springboot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DeepSeek AI服务类（支持多轮对话 + 流式输出）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder().build();

    // 会话上下文存储（key: 用户ID，value: 对话历史消息列表）
    // 使用ConcurrentHashMap保证线程安全
    private final Map<String, List<Map<String, String>>> conversationContext = new ConcurrentHashMap<>();

    /**
     * 普通非流式调用（支持多轮对话）
     * @param userId 用户唯一标识（用于区分不同会话）
     * @param systemPrompt 系统提示词（仅首次生效，后续复用上下文）
     * @param userMessage 本次用户消息
     * @return AI响应内容
     */
    public String sendMessage(String userId, String systemPrompt, String userMessage) {
        try {
            log.info("用户[{}]发送消息到DeepSeek API（非流式）", userId);

            // 构建包含历史上下文的请求体
            Map<String, Object> requestBody = buildRequestBodyWithContext(userId, systemPrompt, userMessage, false);

            // 发送请求
            String response = webClient.post()
                    .uri(deepSeekConfig.getBaseUrl() + "/v1/chat/completions")
                    .headers(this::setRequestHeaders)
                    .body(Mono.just(requestBody), Map.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析响应并更新上下文
            JsonNode rootNode = objectMapper.readTree(response);
            String aiResponse = rootNode.path("choices").get(0).path("message").path("content").asText();
            // 将AI响应添加到上下文
            addMessageToContext(userId, "assistant", aiResponse);

            log.info("用户[{}] DeepSeek API返回成功，响应长度: {}", userId, aiResponse.length());
            return aiResponse;

        } catch (Exception e) {
            log.error("用户[{}] 调用DeepSeek API失败: {}", userId, e.getMessage(), e);
            throw new RuntimeException("DeepSeek AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式调用（支持多轮对话）
     * @param userId 用户唯一标识
     * @param systemPrompt 系统提示词
     * @param userMessage 本次用户消息
     * @return 流式响应Flux
     */
    public Flux<String> sendMessageStream(String userId, String systemPrompt, String userMessage) {
        log.info("用户[{}]发送消息到DeepSeek API（流式模式）", userId);

        // 构建包含历史上下文的流式请求体
        Map<String, Object> requestBody = buildRequestBodyWithContext(userId, systemPrompt, userMessage, true);

        // 用于拼接流式响应的完整内容（最终要存入上下文）
        StringBuilder fullResponse = new StringBuilder();

        return webClient.post()
                .uri(deepSeekConfig.getBaseUrl() + "/v1/chat/completions")
                .headers(this::setRequestHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(requestBody), Map.class)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line != null && !line.isEmpty() && !line.equals("data: [DONE]"))
                .map(line -> {
                    String content = parseSseDataLine(line);
                    if (!content.isEmpty()) {
                        fullResponse.append(content);
                    }
                    return content;
                })
                .filter(content -> content != null && !content.isEmpty())
                // 流式响应结束后，将完整响应存入上下文
                .doOnComplete(() -> {
                    if (fullResponse.length() > 0) {
                        addMessageToContext(userId, "assistant", fullResponse.toString());
                        log.info("用户[{}] 流式响应完成，已更新上下文", userId);
                    }
                })
                .doOnError(e -> log.error("用户[{}] 流式调用DeepSeek API失败: {}", userId, e.getMessage(), e))
                .onErrorResume(e -> Flux.just("流式响应出错: " + e.getMessage()));
    }

    /**
     * 构建包含上下文的请求体
     */
    private Map<String, Object> buildRequestBodyWithContext(String userId, String systemPrompt, String userMessage, boolean stream) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekConfig.getModel());
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        requestBody.put("stream", stream);

        // 获取/初始化用户的对话上下文
        List<Map<String, String>> messages = getOrInitConversationContext(userId);

        // 首次对话时添加系统提示（避免重复添加）
        if (messages.isEmpty() && systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        // 添加本次用户消息到上下文（并同步到请求体）
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", new ArrayList<>(messages)); // 传入副本避免并发问题
        return requestBody;
    }

    /**
     * 获取或初始化用户的对话上下文
     */
    private List<Map<String, String>> getOrInitConversationContext(String userId) {
        return conversationContext.computeIfAbsent(userId, k -> new ArrayList<>());
    }

    /**
     * 向上下文添加消息（AI响应）
     */
    private void addMessageToContext(String userId, String role, String content) {
        List<Map<String, String>> messages = getOrInitConversationContext(userId);
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        messages.add(msg);

        // 可选：限制上下文长度，避免token超限（例如保留最近10轮）
        trimContextIfNecessary(messages, 10);
    }

    /**
     * 修剪上下文长度（避免消息过多导致token超限）
     * @param messages 消息列表
     * @param maxRound 保留的最大轮数（一轮=用户+助手）
     */
    private void trimContextIfNecessary(List<Map<String, String>> messages, int maxRound) {
        // 系统消息保留，只修剪用户/助手消息
        long systemMsgCount = messages.stream().filter(m -> "system".equals(m.get("role"))).count();
        long maxTotalMsg = systemMsgCount + maxRound * 2; // 每轮包含user+assistant

        if (messages.size() > maxTotalMsg) {
            // 保留系统消息 + 最后maxRound轮对话
            List<Map<String, String>> newMessages = new ArrayList<>();
            // 添加系统消息
            newMessages.addAll(messages.stream().filter(m -> "system".equals(m.get("role"))).toList());
            // 添加最后maxRound轮的用户/助手消息
            newMessages.addAll(messages.subList(messages.size() - maxRound * 2, messages.size()));
            // 替换原上下文
            messages.clear();
            messages.addAll(newMessages);
            log.warn("上下文消息数超限，已修剪为{}条", newMessages.size());
        }
    }

    /**
     * 清理指定用户的对话上下文
     */
    public void clearConversationContext(String userId) {
        conversationContext.remove(userId);
        log.info("用户[{}] 对话上下文已清理", userId);
    }

    /**
     * 清理所有用户的对话上下文（管理员用）
     */
    public void clearAllConversationContext() {
        conversationContext.clear();
        log.info("所有用户对话上下文已清理");
    }

    /**
     * 设置请求头
     */
    private void setRequestHeaders(HttpHeaders headers) {
        headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * 解析SSE单行数据
     */
    private String parseSseDataLine(String line) {
        try {
            String jsonStr = line.startsWith("data: ") ? line.substring(6) : line;
            if (jsonStr.isEmpty() || jsonStr.equals("[DONE]")) {
                return "";
            }
            JsonNode rootNode = objectMapper.readTree(jsonStr);
            JsonNode contentNode = rootNode.path("choices")
                    .get(0)
                    .path("delta")
                    .path("content");
            return contentNode.isMissingNode() ? "" : contentNode.asText();
        } catch (Exception e) {
            log.warn("解析SSE数据失败: {}, 原始数据: {}", e.getMessage(), line);
            return "";
        }
    }
}