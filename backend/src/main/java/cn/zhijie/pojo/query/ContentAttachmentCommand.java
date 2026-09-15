package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record ContentAttachmentCommand(UUID ownerId, String checksum) {}
