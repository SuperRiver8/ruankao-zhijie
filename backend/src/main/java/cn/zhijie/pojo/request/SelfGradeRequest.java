package cn.zhijie.pojo.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;

// 按题目 ID 提交自评分数，不额外包装请求 JSON 层级。
public record SelfGradeRequest(@JsonValue Map<String, Double> scores) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public SelfGradeRequest {}
}
