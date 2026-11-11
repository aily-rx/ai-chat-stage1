package com.ailyrx.aichat.extract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExtractRequest(
        @NotBlank(message = "text 不能为空")
        @Size(max = 10000, message = "text 不能超过 10000 字符")
        String text) {

    public String normalizedText() {
        return text == null ? "" : text.trim();
    }
}
