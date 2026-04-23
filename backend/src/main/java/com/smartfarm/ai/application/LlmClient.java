package com.smartfarm.ai.application;

import java.util.List;
import java.util.Map;

/**
 * 大模型客户端抽象层 — 支持切换不同模型
 */
public interface LlmClient {

    /**
     * 发送对话请求（含 Function Calling）
     */
    LlmResponse chat(String systemPrompt, List<Map<String, String>> messages, List<FunctionDef> functions);

    /**
     * 简单文本对话
     */
    LlmResponse chat(String systemPrompt, String userMessage);

    record LlmResponse(String content, String functionName, Map<String, Object> functionArgs,
                        String model, int tokensUsed, long latencyMs) {}

    record FunctionDef(String name, String description, Map<String, Object> parameters) {}
}
