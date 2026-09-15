package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record InsertEntityCommand(UUID id, String kind, String namespace, String externalId) {}
