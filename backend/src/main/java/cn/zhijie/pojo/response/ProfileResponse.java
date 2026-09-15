package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(
    UUID id,
    String username,
    String role,
    Boolean identityVerified,
    Integer memberLevel,
    List<BadgeResponse> badges,
    List<CertificateResponse> targets,
    List<MemberRuleResponse> rules,
    List<LevelHistoryResponse> history
) {}
