package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record VerifyIdentityCommand(UUID id, String name, String evidence) {}
