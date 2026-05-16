package com.rain.ai.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BizExceptionTest {

    @Test
    void 业务异常携带错误码和消息() {
        BizException exception = new BizException(ErrorCode.参数错误, "知识库名称不能为空");

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.参数错误);
        assertThat(exception.getMessage()).isEqualTo("知识库名称不能为空");
    }
}
