package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record SaveCertificateCommand(
    UUID id,
    String code,
    String name,
    String specialty,
    Integer level
) {}
