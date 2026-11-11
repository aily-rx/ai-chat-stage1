package com.ailyrx.aichat.extract;

import com.ailyrx.aichat.common.ApiResponse;
import com.ailyrx.aichat.config.TraceIdFilter;
import com.ailyrx.aichat.llm.LlmException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExtractController {

    private static final Logger log = LoggerFactory.getLogger(ExtractController.class);

    private final ExtractionService extractionService;

    public ExtractController(ExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping("/extract")
    public ApiResponse<ExtractResult> extract(
            @Valid @RequestBody ExtractRequest request,
            @RequestAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE) String traceId) {
        try {
            return ApiResponse.ok(
                    extractionService.extract(request.normalizedText(), traceId),
                    traceId);
        } catch (ExtractionParseException exception) {
            return ApiResponse.error(5003, exception.getMessage(), traceId);
        } catch (LlmException exception) {
            log.error("extract model call failed traceId={}", traceId, exception);
            return ApiResponse.error(5001, "模型调用失败", traceId);
        }
    }
}
