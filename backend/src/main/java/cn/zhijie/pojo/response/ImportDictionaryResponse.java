package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImportDictionaryResponse(
    List<CertificateResponse> certificates,
    List<ContentResponse> knowledge,
    List<ContentResponse> syllabus
) {}
