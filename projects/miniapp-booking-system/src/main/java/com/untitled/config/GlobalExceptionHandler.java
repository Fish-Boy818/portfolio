package com.untitled.config;

import com.untitled.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Void> handleIllegalState(IllegalStateException ex) {
        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ApiResponse.fail(message.isEmpty() ? "参数校验失败" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException ex) {
        return ApiResponse.fail(ex.getMessage());
    }

    @ExceptionHandler(RestClientException.class)
    public ApiResponse<Void> handleRestClient(RestClientException ex) {
        log.error("Remote call failed", ex);
        return ApiResponse.fail("微信登录失败，请检查网络或 AppID/Secret");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ApiResponse.fail("文件过大，请上传不超过 10MB 的文件");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.contains("LocalDate") || message.contains("expireDate")) {
            return ApiResponse.fail("有效期格式不正确，请使用 YYYY-MM-DD，或留空");
        }
        return ApiResponse.fail("参数格式不正确，请检查日期/数字字段");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResponse.fail("服务异常");
    }

    private String formatFieldError(FieldError error) {
        if (error == null) {
            return "";
        }
        return error.getField() + " " + error.getDefaultMessage();
    }
}
