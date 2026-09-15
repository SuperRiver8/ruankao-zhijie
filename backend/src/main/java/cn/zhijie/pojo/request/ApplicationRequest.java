package cn.zhijie.pojo.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

// 接口请求实体，可选字段为空时不传入业务参数。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApplicationRequest(
    @NotNull UUID certificateId,
    @NotBlank @Size(max = 100) String holderName,
    @NotBlank @Size(max = 100) String number,
    @NotBlank String numberType,
    @NotNull @PastOrPresent LocalDate obtainedOn,
    @NotNull UUID attachmentId,
    UUID auxiliaryId,
    @Size(max = 4000) String note
) {}
