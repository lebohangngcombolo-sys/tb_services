package com.tradebridge.common.response;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.pagination.PageMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseFactoryTest {

    @AfterEach
    void cleanup() {
        CorrelationContext.clear();
    }

    @Test
    void ok_usesCurrentCorrelationId() {
        CorrelationContext.set("corr-123");
        ApiResponse<String> response = ResponseFactory.ok("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getCorrelationId()).isEqualTo("corr-123");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void ok_generatesCorrelationId_whenNoneSet() {
        ApiResponse<Void> response = ResponseFactory.ok("done");

        assertThat(response.getCorrelationId()).isNotBlank();
        assertThat(response.getCorrelationId()).hasSize(36); // UUID string
    }

    @Test
    void error_fromBusinessException_preservesCodeAndFieldErrors() {
        CorrelationContext.set("corr-abc");
        BusinessException ex = new BusinessException(
                ErrorCode.VALIDATION_ERROR,
                "bad",
                List.of(new FieldError("email", "Invalid email address")));

        ErrorResponse response = ResponseFactory.error(ex);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        assertThat(response.getMessage()).isEqualTo("bad");
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getCorrelationId()).isEqualTo("corr-abc");
    }

    @Test
    void page_buildsPageResponse() {
        PageMetadata meta = PageMetadata.of(0, 20, 2);
        PageResponse<String> page = ResponseFactory.page(List.of("a", "b"), meta);

        assertThat(page.getContent()).containsExactly("a", "b");
        assertThat(page.getPage().totalElements()).isEqualTo(2);
    }
}