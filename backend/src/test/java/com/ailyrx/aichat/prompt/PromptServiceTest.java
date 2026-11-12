package com.ailyrx.aichat.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PromptServiceTest {

    private final PromptService promptService = new PromptService();

    @Test
    void formalAndConcisePromptsDiffer() {
        PromptTemplate formal = promptService.chatPrompt("formal");
        PromptTemplate concise = promptService.chatPrompt("concise");

        assertThat(formal.content()).contains("风格要求：正式");
        assertThat(concise.content()).contains("风格要求：简洁");
        assertThat(formal.content()).isNotEqualTo(concise.content());
    }

    @Test
    void templatesExposeStableIdsAndVersions() {
        PromptTemplate formal = promptService.chatPrompt("formal");
        PromptTemplate concise = promptService.chatPrompt("concise");
        PromptTemplate extract = promptService.extractPrompt();

        assertThat(formal.id()).isEqualTo("chat-formal");
        assertThat(concise.id()).isEqualTo("chat-concise");
        assertThat(extract.id()).isEqualTo("extract-fields");
        assertThat(formal.version()).isNotBlank().startsWith("v");
        assertThat(concise.version()).isNotBlank().startsWith("v");
        assertThat(extract.version()).isNotBlank().startsWith("v");
    }

    @Test
    void replacesStyleInstructionVariable() {
        String prompt = promptService.chatPrompt("formal").content();

        assertThat(prompt)
                .contains("风格要求：正式")
                .doesNotContain("{{style_instruction}}");
    }

    @Test
    void extractPromptKeepsFixedFieldContract() {
        String prompt = promptService.extractPrompt().content();

        assertThat(prompt)
                .contains("docType")
                .contains("title")
                .contains("recipient")
                .contains("signature")
                .contains("date")
                .doesNotContain("{{");
    }

    @Test
    void rejectsUnknownStyle() {
        assertThatThrownBy(() -> promptService.chatPrompt("poetic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("poetic");
    }
}
