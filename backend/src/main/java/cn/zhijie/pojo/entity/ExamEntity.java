package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class ExamEntity {

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID value) {
        this.userId = value;
    }

    private UUID paperId;

    public UUID getPaperId() {
        return paperId;
    }

    public void setPaperId(UUID value) {
        this.paperId = value;
    }

    private JsonNode snapshot;

    public JsonNode getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(JsonNode value) {
        this.snapshot = value;
    }

    private JsonNode answers;

    public JsonNode getAnswers() {
        return answers;
    }

    public void setAnswers(JsonNode value) {
        this.answers = value;
    }

    private JsonNode result;

    public JsonNode getResult() {
        return result;
    }

    public void setResult(JsonNode value) {
        this.result = value;
    }

    private Instant startedAt;

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant value) {
        this.startedAt = value;
    }

    private Instant deadline;

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant value) {
        this.deadline = value;
    }

    private Instant submittedAt;

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant value) {
        this.submittedAt = value;
    }

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }
}
