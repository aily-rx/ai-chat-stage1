package com.ailyrx.aichat.extract;

/**
 * 五个字段固定输出；无法识别的字段保留字段名并返回 null。
 */
public record ExtractResult(
        String docType,
        String title,
        String recipient,
        String signature,
        String date) {
}
