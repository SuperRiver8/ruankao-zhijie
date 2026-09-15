package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record GrantCertificateCommand(
    UUID id,
    UUID userId,
    UUID certificateId,
    UUID applicationId,
    String hash
) {}
