package com.ailyrx.aichat.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 从 classpath 加载 Prompt 模板，负责变量替换与版本标识。
 */
@Service
public class PromptService {

    private static final Logger log = LoggerFactory.getLogger(PromptService.class);
    private static final String CATALOG_PATH = "prompts/prompt-catalog.properties";
    private static final String CHAT_STYLE_PREFIX = "chat.styles.";
    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile(Pattern.quote("{{") + "([a-zA-Z0-9_]+)" + Pattern.quote("}}"));

    private final Map<String, PromptTemplate> chatTemplates;

    private final PromptTemplate extractTemplate;

    public PromptService() {
        Properties catalog = loadCatalog();
        String chatVersion = requireProperty(catalog, "chat.version");
        String chatBase = loadText(requireProperty(catalog, "chat.base"));

        Map<String, PromptTemplate> templates = new LinkedHashMap<>();
        for (String style : collectChatStyles(catalog)) {
            String stylePath = requireProperty(catalog, CHAT_STYLE_PREFIX + style);
            String styleInstruction = loadText(stylePath);
            String content = render(chatBase, Map.of("style_instruction", styleInstruction));
            templates.put(style, new PromptTemplate("chat-" + style, chatVersion, content));
        }

        if (templates.isEmpty()) {
            throw new IllegalStateException("Prompt 模板清单未配置任何对话风格");
        }

        this.chatTemplates = Collections.unmodifiableMap(templates);
        this.extractTemplate = new PromptTemplate(
                "extract-fields",
                requireProperty(catalog, "extract.version"),
                loadText(requireProperty(catalog, "extract.template")));

        log.info("prompt catalog loaded chatVersion={} styles={} extractVersion={}",
                chatVersion, this.chatTemplates.keySet(), this.extractTemplate.version());
    }

    public PromptTemplate chatPrompt(String style) {
        PromptTemplate template = chatTemplates.get(style);
        if (template == null) {
            throw new IllegalArgumentException("不支持的 Prompt 风格: " + style);
        }
        return template;
    }

    public PromptTemplate extractPrompt() {
        return extractTemplate;
    }

    private Properties loadCatalog() {
        Properties properties = new Properties();
        try (InputStream input = new ClassPathResource(CATALOG_PATH).getInputStream();
                Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("无法加载 Prompt 模板清单: " + CATALOG_PATH, exception);
        }
    }

    private TreeSet<String> collectChatStyles(Properties catalog) {
        TreeSet<String> styles = new TreeSet<>();
        for (String key : catalog.stringPropertyNames()) {
            if (key.startsWith(CHAT_STYLE_PREFIX) && key.length() > CHAT_STYLE_PREFIX.length()) {
                styles.add(key.substring(CHAT_STYLE_PREFIX.length()));
            }
        }
        return styles;
    }

    private String requireProperty(Properties catalog, String key) {
        String value = catalog.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少 Prompt 配置: " + key);
        }
        return value.strip();
    }

    private String loadText(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8).strip();
            if (content.isEmpty()) {
                throw new IllegalStateException("Prompt 模板内容为空: " + path);
            }
            return content;
        } catch (IOException exception) {
            throw new IllegalStateException("无法加载 Prompt 模板: " + path, exception);
        }
    }

    private String render(String template, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = variables.get(name);
            if (value == null) {
                throw new IllegalStateException("Prompt 模板缺少变量: " + name);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }
}
