package com.ailyrx.aichat.llm;

/**
 * 模型调用失败。对外只暴露通用错误信息，原始异常仅写入服务端日志。
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
