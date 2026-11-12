package com.ailyrx.aichat.prompt;

/**
 * 已渲染的 Prompt 模板，携带稳定标识与版本，便于日志追踪和问题复现。
 */
public record PromptTemplate(String id, String version, String content) {
}
