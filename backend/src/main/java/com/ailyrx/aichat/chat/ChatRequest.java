package com.ailyrx.aichat.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "message 不能为空")
        @Size(max = 2000, message = "message 不能超过 2000 字符")
        String message,

        @NotBlank(message = "style 不能为空")
        @Pattern(regexp = "formal|concise", message = "style 只支持 formal 或 concise")
        String style) {

    public String normalizedMessage() {
        return message == null ? "" : message.trim();
    }
}
