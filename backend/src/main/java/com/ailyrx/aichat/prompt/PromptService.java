package com.ailyrx.aichat.prompt;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    private static final String BASE_PROMPT = """
            你是一名严谨的中文公文写作助手，请根据用户的写作要求输出中文内容。
            要求：
            1. 内容准确、结构清晰，符合中文公文表达习惯；
            2. 使用 Markdown 分段，必要时使用标题、编号和列表；
            3. 不要编造不存在的单位、数据或文件依据，缺失信息使用“XX单位”等占位符；
            4. 直接输出正文，不要解释你的写作过程。
            """;

    private static final String FORMAL_STYLE = """
            风格要求：正式。使用规范书面语和完整公文结构，称谓、开头、正文、结尾和落款按需完整呈现，语气庄重克制。
            问答类回答通常控制在 200–300 字；撰写公文时按实际需要完整展开。
            """;

    private static final String CONCISE_STYLE = """
            风格要求：简洁。直入主题，保留必要信息，删除背景铺垫和重复表述，优先使用短句。
            问答类回答用一句话直接回答，不超过 50 字；撰写公文时只保留必要公文要素。
            """;

    public String buildChatPrompt(String style) {
        String stylePrompt = "concise".equals(style) ? CONCISE_STYLE : FORMAL_STYLE;
        return BASE_PROMPT + "\n" + stylePrompt;
    }
}
