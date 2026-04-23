package com.smartfarm.ai.infrastructure.llm;

import com.smartfarm.ai.application.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 通义千问实现 — 通过 OpenAI 兼容接口调用
 * 也可切换为 DeepSeek / GLM-4 等兼容 OpenAI 格式的模型
 */
@Slf4j
@Component
public class QwenClient implements LlmClient {

    @Value("${ai.llm.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.llm.api-key:}")
    private String apiKey;

    @Value("${ai.llm.model:qwen-plus}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public LlmResponse chat(String systemPrompt, List<Map<String, String>> messages, List<FunctionDef> functions) {
        long start = System.currentTimeMillis();

        List<Map<String, Object>> msgs = new ArrayList<>();
        msgs.add(Map.of("role", "system", "content", systemPrompt));
        for (Map<String, String> m : messages) {
            msgs.add(Map.of("role", m.get("role"), "content", m.get("content")));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", msgs);
        body.put("temperature", 0.7);
        body.put("max_tokens", 1024);

        if (functions != null && !functions.isEmpty()) {
            List<Map<String, Object>> tools = functions.stream().map(f -> Map.<String, Object>of(
                    "type", "function",
                    "function", Map.of("name", f.name(), "description", f.description(), "parameters", f.parameters())
            )).toList();
            body.put("tools", tools);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(apiUrl, new HttpEntity<>(body, headers), Map.class);
            long latency = System.currentTimeMillis() - start;

            if (resp == null) return new LlmResponse("AI 服务暂不可用", null, null, model, 0, latency);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices == null || choices.isEmpty()) return new LlmResponse("AI 无响应", null, null, model, 0, latency);

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.getOrDefault("content", "");

            // 解析 function call
            String funcName = null;
            Map<String, Object> funcArgs = null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fc = (Map<String, Object>) toolCalls.get(0).get("function");
                funcName = (String) fc.get("name");
                String argsStr = (String) fc.get("arguments");
                if (argsStr != null) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                        funcArgs = om.readValue(argsStr, Map.class);
                    } catch (Exception e) {
                        log.warn("Function args 解析失败: {}", argsStr);
                    }
                }
            }

            // token 统计
            int tokens = 0;
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) resp.get("usage");
            if (usage != null) {
                tokens = ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
            }

            return new LlmResponse(content, funcName, funcArgs, model, tokens, latency);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.error("LLM 调用失败: {}", e.getMessage());
            return new LlmResponse("AI 服务暂时不可用，请稍后再试", null, null, model, 0, latency);
        }
    }

    @Override
    public LlmResponse chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, List.of(Map.of("role", "user", "content", userMessage)), null);
    }
}
