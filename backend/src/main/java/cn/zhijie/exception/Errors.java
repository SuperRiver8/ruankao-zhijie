package cn.zhijie.exception;

import cn.zhijie.pojo.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class Errors {

    @ExceptionHandler(cn.zhijie.security.CaptchaException.class)
    ResponseEntity<ApiResponse<Void>> captcha(cn.zhijie.security.CaptchaException error) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(error.code(), error.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiResponse<Void>> request(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(
            ApiResponse.failure(
                "HTTP_" + error.getStatusCode().value(),
                error.getReason() == null ? "请求失败" : error.getReason()
            )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException error) {
        return ResponseEntity.status(409).body(
            ApiResponse.failure("CONFLICT", "数据已存在、已被认领或引用无效，请刷新检查")
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException error) {
        var field = error.getBindingResult().getFieldError();
        return ResponseEntity.badRequest()
            .body(
                ApiResponse.failure(
                    "VALIDATION_ERROR",
                    field == null
                        ? "参数校验失败"
                        : field.getField() + ": " + field.getDefaultMessage()
                )
            );
    }

    @ExceptionHandler(
        {
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
        }
    )
    ResponseEntity<ApiResponse<Void>> invalid(Exception error) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure("INVALID_REQUEST", "请求格式不正确"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException error) {
        return ResponseEntity.status(403).body(ApiResponse.failure("FORBIDDEN", "无操作权限"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception error) {
        LoggerFactory.getLogger(Errors.class).error("请求处理失败", error);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.failure("INTERNAL_ERROR", "服务暂时不可用，请凭 traceId 联系管理员"));
    }
}
