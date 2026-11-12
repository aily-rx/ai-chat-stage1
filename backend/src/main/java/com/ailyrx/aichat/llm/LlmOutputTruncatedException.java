package com.ailyrx.aichat.llm;

/**
 * 模型输出达到长度上限，回答被截断。
 */
public class LlmOutputTruncatedException extends LlmException {

    public LlmOutputTruncatedException(String message) {
        super(message);
    }
}
