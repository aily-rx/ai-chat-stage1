package com.ailyrx.aichat.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PromptServiceTest {

    private final PromptService promptService = new PromptService();

    @Test
    void formalAndConcisePromptsDiffer() {
        String formal = promptService.buildChatPrompt("formal");
        String concise = promptService.buildChatPrompt("concise");

        assertThat(formal).contains("风格要求：正式");
        assertThat(concise).contains("风格要求：简洁");
        assertThat(formal).isNotEqualTo(concise);
    }
}
