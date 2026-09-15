package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record InsertVerificationCommand(
    UUID id,
    UUID applicationId,
    UUID reviewer,
    String result,
    String source,
    String evidence,
    String ownership
) {}
