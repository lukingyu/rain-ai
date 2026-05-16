package com.rain.ai.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void 成功响应包含数据和成功标记() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("成功");
        assertThat(response.message()).isEqualTo("请求成功");
        assertThat(response.data()).isEqualTo("ok");
    }

    @Test
    void 失败响应包含错误码和错误信息() {
        ApiResponse<Void> response = ApiResponse.failure("参数错误", "知识库名称不能为空");

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("参数错误");
        assertThat(response.message()).isEqualTo("知识库名称不能为空");
        assertThat(response.data()).isNull();
    }
}
