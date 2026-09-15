package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.AttachmentMapper;
import cn.zhijie.dao.CertificateMapper;
import cn.zhijie.dao.ContentMapper;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.Audit;
import cn.zhijie.util.ResponseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    private final CertificateMapper certificateMapper;
    private final AttachmentMapper attachmentMapper;
    private final ContentMapper contentMapper;
    private final Audit audit;
    private final com.networknt.schema.JsonSchema schema;

    public ContentService(
        CertificateMapper certificateMapper,
        AttachmentMapper attachmentMapper,
        ContentMapper contentMapper,
        Audit audit
    ) {
        this.certificateMapper = certificateMapper;
        this.attachmentMapper = attachmentMapper;
        this.contentMapper = contentMapper;
        this.audit = audit;
        try (var in = getClass().getResourceAsStream("/import-schema.json")) {
            schema = com.networknt.schema.JsonSchemaFactory.getInstance(
                com.networknt.schema.SpecVersion.VersionFlag.V202012
            ).getSchema(JSON.readTree(in));
        } catch (Exception e) {
            throw new IllegalStateException("内容 Schema 加载失败", e);
        }
    }

    public void validate(String kind, JsonNode p) {
        var errors = schema.validate(
            JSON.valueToTree(
                map(
                    "templateVersion",
                    "1.0",
                    "namespace",
                    "validation",
                    "entries",
                    List.of(map("kind", kind, "externalId", "validation", "payload", p))
                )
            )
        );
        check(
            errors.isEmpty(),
            "内容不符合 Schema：" + errors.stream().limit(3).map(Object::toString).toList()
        );
        check(
            Set.of("KNOWLEDGE", "SYLLABUS", "MAPPING", "QUESTION", "MATERIAL", "PAPER").contains(
                kind
            ),
            "内容类型无效"
        );
        check(p.isObject() && !p.path("title").asText().isBlank(), "内容标题必填");
        if (p.has("certificateCode")) check(
            certificateMapper
                .certificates()
                .stream()
                .anyMatch(c -> c.getCode().equals(p.path("certificateCode").asText())),
            "证书编码不存在"
        );
        for (String field : List.of("attachmentIds", "knowledgeCodes")) if (p.has(field)) check(
            p.path(field).isArray(),
            field + " 必须是数组"
        );
        for (JsonNode id : p.path("attachmentIds")) {
            var f = found(attachmentMapper.attachment(uuid(id.asText())));
            check(f.getAccessLevel().equals("CONTENT"), "内容不可引用证书私有材料");
        }
        if (kind.equals("QUESTION")) validateQuestion(p);
        if (kind.equals("PAPER")) {
            check(
                p.path("durationMinutes").asInt() >= 1 && p.path("durationMinutes").asInt() <= 600,
                "考试时长需为 1–600 分钟"
            );
            check(
                p.path("questionIds").isArray() || p.path("count").asInt() > 0,
                "请配置固定题目或动态题量"
            );
            check(p.path("points").asDouble(1) > 0, "分值必须大于零");
        }
        if (kind.equals("SYLLABUS")) {
            check(
                p.path("subjects").isArray() &&
                !p.path("subjects").isEmpty() &&
                !p.path("version").asText().isBlank(),
                "大纲需填写版本与科目章节"
            );
            Set<String> subjects = new HashSet<>();
            for (var subject : p.path("subjects")) {
                check(
                    !subject.path("code").asText().isBlank() &&
                    subjects.add(subject.path("code").asText()) &&
                    !subject.path("name").asText().isBlank(),
                    "科目需唯一编码及名称"
                );
                check(subject.path("chapters").isArray(), "科目需包含章节数组");
                Set<String> chapters = new HashSet<>();
                for (var chapter : subject.path("chapters")) check(
                    !chapter.path("code").asText().isBlank() &&
                    chapters.add(chapter.path("code").asText()) &&
                    !chapter.path("name").asText().isBlank(),
                    "章节需唯一编码及名称"
                );
            }
        }
        if (kind.equals("MAPPING")) check(
            p.path("knowledgeCodes").isArray() &&
            !p.path("syllabusCode").asText().isBlank() &&
            !p.path("subject").asText().isBlank() &&
            !p.path("chapter").asText().isBlank(),
            "大纲映射需提供大纲编码、科目、章节和知识点"
        );
    }

    private void validateQuestion(JsonNode p) {
        String type = p.path("type").asText();
        check(
            Set.of("SINGLE", "MULTIPLE", "BOOLEAN", "FILL", "SHORT", "CASE", "ESSAY").contains(
                type
            ),
            "题型无效"
        );
        check(!p.path("stem").asText().isBlank(), "题干不能为空");
        check(
            p.path("difficulty").asInt() >= 1 && p.path("difficulty").asInt() <= 5,
            "难度需为 1–5"
        );
        if (type.equals("CASE")) {
            check(
                p.path("children").isArray() && !p.path("children").isEmpty(),
                "案例题需包含子题"
            );
            for (var child : p.path("children")) {
                check(!child.path("type").asText().equals("CASE"), "案例题不能嵌套案例题");
                validateQuestion(child);
            }
            return;
        }
        if (Set.of("SHORT", "ESSAY").contains(type)) {
            check(p.hasNonNull("rubric"), "主观题必须提供评分要点");
            return;
        }
        check(p.path("answer").isArray() && !p.path("answer").isEmpty(), "答案必须是非空数组");
        Set<String> answer = new HashSet<>();
        for (var x : p.path("answer")) {
            check(x.isTextual() && !x.asText().isBlank(), "答案值必须为非空字符串");
            check(answer.add(x.asText()), "答案不能重复");
        }
        if (Set.of("SINGLE", "MULTIPLE").contains(type)) {
            check(
                p.path("options").isArray() && p.path("options").size() >= 2,
                "选择题至少两个选项"
            );
            Set<String> keys = new HashSet<>();
            for (var x : p.path("options")) {
                check(
                    !x.path("id").asText().isBlank() &&
                    !x.path("text").asText().isBlank() &&
                    keys.add(x.path("id").asText()),
                    "选项需唯一 ID 和文本"
                );
            }
            check(keys.containsAll(answer), "答案引用了不存在的选项");
            check(
                type.equals("MULTIPLE") ? answer.size() >= 2 : answer.size() == 1,
                "答案数量与题型不符"
            );
        }
        if (type.equals("BOOLEAN")) check(
            answer.size() == 1 && Set.of("true", "false").containsAll(answer),
            "判断题答案应为 true 或 false"
        );
    }

    public void references(JsonNode p, String namespace, Set<String> staged) {
        for (var code : p.path("knowledgeCodes")) {
            String c = code.asText();
            check(
                staged.contains("KNOWLEDGE:" + c) ||
                contentMapper.findContent(new FindContentCommand(namespace, "KNOWLEDGE", c)) !=
                null,
                "知识点引用不存在：" + c
            );
        }
        if (p.has("syllabusCode")) {
            String c = p.path("syllabusCode").asText();
            check(
                staged.contains("SYLLABUS:" + c) ||
                contentMapper.findContent(new FindContentCommand(namespace, "SYLLABUS", c)) != null,
                "大纲引用不存在：" + c
            );
        }
        for (var child : p.path("children")) references(child, namespace, staged);
    }

    public void chapterReference(JsonNode p, String namespace, Map<String, JsonNode> staged) {
        if (p.has("syllabusCode") && p.has("subject")) {
            String code = p.path("syllabusCode").asText();
            JsonNode syllabus = staged.get("SYLLABUS:" + code);
            if (syllabus == null) {
                var row = contentMapper.findContent(
                    new FindContentCommand(namespace, "SYLLABUS", code)
                );
                if (row != null) syllabus = tree(row.getPayload());
            }
            check(syllabus != null, "大纲引用不存在");
            JsonNode foundSubject = null;
            for (var subject : syllabus.path("subjects")) if (
                subject.path("code").asText().equals(p.path("subject").asText())
            ) foundSubject = subject;
            check(foundSubject != null, "科目编码不属于指定大纲");
            if (p.has("chapter")) {
                boolean matched = false;
                for (var chapter : foundSubject.path("chapters")) if (
                    chapter.path("code").asText().equals(p.path("chapter").asText())
                ) matched = true;
                check(matched, "章节编码不属于指定科目");
            }
        }
        for (var child : p.path("children")) chapterReference(child, namespace, staged);
    }

    @Transactional
    public SaveContentResponse save(Actor a, UUID id, ContentRequest p) {
        a.require("ADMIN", "EDITOR");
        String kind = required(p.kind(), "kind"), namespace = required(
            p.namespace(),
            "namespace"
        ), external = required(p.externalId(), "externalId");
        JsonNode payload = JSON.valueToTree(p.payload());
        validate(kind, payload);
        references(payload, namespace, Set.of());
        chapterReference(payload, namespace, Map.of());
        return saveInternal(a, id, kind, namespace, external, payload);
    }

    public SaveContentResponse saveInternal(
        Actor a,
        UUID id,
        String kind,
        String namespace,
        String external,
        JsonNode payload
    ) {
        int version = 1;
        String checksum = hash(canonical(payload));
        if (id == null) {
            var existing = contentMapper.findContent(
                new FindContentCommand(namespace, kind, external)
            );
            if (existing != null) {
                check(
                    existing.getChecksum().equals(checksum),
                    "编码已有不同内容，请通过编辑生成新版本"
                );
                return new SaveContentResponse(existing.getId(), null, true);
            }
            id = UUID.randomUUID();
            contentMapper.insertEntity(new InsertEntityCommand(id, kind, namespace, external));
        } else {
            var old = found(contentMapper.content(id));
            check(
                old.getKind().equals(kind) &&
                old.getNamespace().equals(namespace) &&
                old.getExternalId().equals(external),
                "稳定编码与类型不能修改"
            );
            if (old.getChecksum().equals(checksum)) return new SaveContentResponse(id, null, true);
            version = old.getCurrentVersion() + 1;
            contentMapper.nextVersion(new NextVersionCommand(id, version));
        }
        contentMapper.insertVersion(
            new InsertVersionCommand(
                id,
                version,
                payload.path("title").asText(),
                JSON.valueToTree(payload),
                checksum,
                a.id()
            )
        );
        audit.log(a, "CONTENT_VERSION_CREATED", id, "版本 " + version + "，待审核");
        return new SaveContentResponse(id, version, false);
    }

    public PageResponse<ContentResponse> list(
        Actor a,
        String kind,
        boolean admin,
        PageQuery query
    ) {
        if (admin) a.require("ADMIN", "EDITOR");
        return PageResponse.of(
            contentMapper
                .contentsPage(kind, !admin, query)
                .stream()
                .map(row -> {
                    var payload = row.getPayload().deepCopy();
                    if (!admin && Set.of("QUESTION", "PAPER").contains(row.getKind())) stripAnswers(
                        payload
                    );
                    row.setPayload(payload);
                    return ResponseMapper.content(row);
                })
                .toList(),
            contentMapper.contentsCount(kind, !admin, query),
            query
        );
    }

    public static void stripAnswers(JsonNode p) {
        if (p.isObject()) {
            var o = (com.fasterxml.jackson.databind.node.ObjectNode) p;
            o.remove(List.of("answer", "explanation", "rubric"));
        }
        for (var child : p.path("children")) stripAnswers(child);
    }

    @Transactional
    public void publish(Actor a, UUID id, ContentReviewRequest p) {
        a.require("ADMIN", "EDITOR");
        var c = found(contentMapper.content(id));
        check(c.getVersion() == p.version(), "内容版本已变化，请刷新审核");
        String status = required(p.status(), "status");
        check(Set.of("PUBLISHED", "REJECTED").contains(status), "发布状态无效");
        JsonNode payload = tree(c.getPayload());
        validate(c.getKind(), payload);
        references(payload, c.getNamespace(), Set.of());
        chapterReference(payload, c.getNamespace(), Map.of());
        check(
            contentMapper.publish(new PublishCommand(id, c.getVersion(), status)) == 1,
            "只能审核待审核版本"
        );
        audit.log(a, "CONTENT_" + status, id, required(p.reason(), "reason"));
    }
}
