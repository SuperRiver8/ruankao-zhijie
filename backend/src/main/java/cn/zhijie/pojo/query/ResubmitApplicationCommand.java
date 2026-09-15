package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record ResubmitApplicationCommand(
    UUID id,
    UUID userId,
    UUID certificateId,
    String holder,
    String numberType,
    String number,
    String hash,
    LocalDate obtainedOn,
    UUID attachmentId,
    UUID auxiliaryId,
    String note,
    String autoResult,
    JsonNode autoDetails
) {}
