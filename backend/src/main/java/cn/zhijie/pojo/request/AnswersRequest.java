package cn.zhijie.pojo.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

// 按题目 ID 保存动态答案，兼容原接口的顶层 JSON 对象。
public record AnswersRequest(@JsonValue Map<String, JsonNode> answers) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AnswersRequest {}
}
