package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.AuditMapper;
import cn.zhijie.dao.ContentMapper;
import cn.zhijie.dao.ExamMapper;
import cn.zhijie.dao.LearningMapper;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.ContentService;
import cn.zhijie.util.ResponseMapper;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamService {

    public PageResponse<ExamSummaryResponse> list(Actor a, PageQuery query) {
        return PageResponse.of(
            examMapper
                .attemptsPage(a.id(), query)
                .stream()
                .map(ResponseMapper::examSummary)
                .toList(),
            examMapper.attemptsCount(a.id(), query),
            query
        );
    }

    private final ContentMapper contentMapper;
    private final ExamMapper examMapper;
    private final LearningMapper learningMapper;
    private final AuditMapper auditMapper;

    public ExamService(
        ContentMapper contentMapper,
        ExamMapper examMapper,
        LearningMapper learningMapper,
        AuditMapper auditMapper
    ) {
        this.contentMapper = contentMapper;
        this.examMapper = examMapper;
        this.learningMapper = learningMapper;
        this.auditMapper = auditMapper;
    }

    @Transactional
    public ExamResponse start(Actor a, UUID paperId) {
        var paper = found(contentMapper.publishedContent(paperId));
        check(paper.getKind().equals("PAPER"), "请选择试卷");
        JsonNode rules = paper.getPayload();
        List<ContentProjection> questions = new ArrayList<>();
        if (rules.path("questionIds").isArray()) {
            for (var id : rules.path("questionIds")) {
                var q = found(contentMapper.publishedContent(uuid(id.asText())));
                check(q.getKind().equals("QUESTION"), "试卷引用了非题目内容");
                questions.add(q);
            }
        } else {
            questions.addAll(
                contentMapper
                    .contents("QUESTION", true)
                    .stream()
                    .filter(q -> {
                        JsonNode p = tree(q.getPayload());
                        return (
                            (!rules.has("certificateCode") ||
                                p.path("certificateCode").equals(rules.path("certificateCode"))) &&
                            (!rules.has("type") || p.path("type").equals(rules.path("type"))) &&
                            (!rules.has("difficulty") ||
                                p.path("difficulty").equals(rules.path("difficulty")))
                        );
                    })
                    .toList()
            );
            Collections.shuffle(questions);
            int count = rules.path("count").asInt();
            check(count > 0 && questions.size() >= count, "可用题量不足，无法组卷");
            questions = new ArrayList<>(questions.subList(0, count));
        }
        return create(a, paperId, paper, questions, rules);
    }

    @Transactional
    public ExamResponse practice(Actor a, PracticeRequest filters) {
        int count = filters.count() == null ? 10 : filters.count();
        check(count >= 1 && count <= 100, "练习题量为 1–100");
        Set<UUID> wrong = new HashSet<>();
        if (Boolean.TRUE.equals(filters.wrongOnly())) for (var record : learningMapper.learning(
            a.id()
        )) if (Boolean.TRUE.equals(record.getWrong())) wrong.add(record.getEntityId());
        List<ContentProjection> questions = new ArrayList<>(
            contentMapper
                .contents("QUESTION", true)
                .stream()
                .filter(q -> {
                    var payload = q.getPayload();
                    if (
                        Boolean.TRUE.equals(filters.wrongOnly()) && !wrong.contains(q.getId())
                    ) return false;
                    if (
                        !matches(payload, "certificateCode", filters.certificateCode()) ||
                        !matches(payload, "type", filters.type()) ||
                        !matches(payload, "difficulty", filters.difficulty()) ||
                        !matches(payload, "subject", filters.subject()) ||
                        !matches(payload, "chapter", filters.chapter())
                    ) return false;
                    if (filters.knowledgeCode() != null && !filters.knowledgeCode().isBlank()) {
                        boolean found = false;
                        for (var code : payload.path("knowledgeCodes")) if (
                            code.asText().equals(filters.knowledgeCode())
                        ) found = true;
                        if (!found) return false;
                    }
                    return true;
                })
                .toList()
        );
        check(questions.size() >= count, "匹配题量不足，当前可用 " + questions.size() + " 题");
        Collections.shuffle(questions);
        var paper = new ContentProjection();
        paper.setVersion(0);
        paper.setTitle(Boolean.TRUE.equals(filters.wrongOnly()) ? "错题重练" : "专项练习");
        return create(
            a,
            null,
            paper,
            new ArrayList<>(questions.subList(0, count)),
            JSON.createObjectNode().put("durationMinutes", 60).put("points", 1)
        );
    }

    private boolean matches(JsonNode payload, String key, String expected) {
        return (
            expected == null || expected.isBlank() || payload.path(key).asText().equals(expected)
        );
    }

    private ExamResponse create(
        Actor a,
        UUID paperId,
        ContentProjection paper,
        List<ContentProjection> questions,
        JsonNode rules
    ) {
        check(!questions.isEmpty() && questions.size() <= 200, "试卷题量需为 1–200");
        check(
            questions.stream().map(q -> q.getId()).distinct().count() == questions.size(),
            "试卷题目重复"
        );
        if (rules.path("shuffleQuestions").asBoolean()) Collections.shuffle(questions);
        ArrayNode frozen = JSON.createArrayNode();
        for (var q : questions) {
            var p = (ObjectNode) tree(q.getPayload());
            p.put("id", q.getId().toString());
            p.put("version", q.getVersion());
            p.put("points", rules.path("points").asDouble(1));
            if (rules.path("shuffleOptions").asBoolean() && p.path("options").isArray()) {
                List<JsonNode> options = new ArrayList<>();
                p.path("options").forEach(options::add);
                Collections.shuffle(options);
                p.set("options", JSON.valueToTree(options));
            }
            frozen.add(p);
        }
        UUID id = UUID.randomUUID();
        var snapshot = document(
            "paperVersion",
            paper.getVersion(),
            "title",
            paper.getTitle(),
            "questions",
            frozen,
            "scoring",
            "客观题全对得分；填空按顺序精确比较（忽略首尾空格）；主观题待人工评分"
        );
        examMapper.insertAttempt(
            new InsertAttemptCommand(
                id,
                a.id(),
                paperId,
                snapshot,
                Instant.now().plusSeconds(rules.path("durationMinutes").asInt() * 60L)
            )
        );
        return read(a, id);
    }

    private ExamEntity owned(Actor a, ExamEntity row) {
        found(row);
        check(row.getUserId().equals(a.id()), "只能访问本人的考试");
        return row;
    }

    public ExamResponse read(Actor a, UUID id) {
        var row = owned(a, examMapper.attempt(id));
        JsonNode snapshot = row.getSnapshot().deepCopy();
        if (row.getStatus().equals("IN_PROGRESS")) for (var q : snapshot.path(
            "questions"
        )) ContentService.stripAnswers(q);
        row.setSnapshot(snapshot);
        return ResponseMapper.exam(row);
    }

    @Transactional
    public SaveAnswersResponse save(Actor a, UUID id, AnswersRequest answers) {
        var row = owned(a, examMapper.lockAttempt(id));
        check(row.getStatus().equals("IN_PROGRESS"), "考试已交卷");
        check(Instant.now().isBefore(row.getDeadline()), "考试已到时，请交卷");
        validateAnswers(row.getSnapshot(), answers);
        examMapper.saveAnswers(new SaveAnswersCommand(id, JSON.valueToTree(answers.answers())));
        return new SaveAnswersResponse(true, Instant.now());
    }

    private void validateAnswers(JsonNode snap, AnswersRequest answers) {
        Set<String> ids = new HashSet<>();
        for (var q : snap.path("questions")) ids.add(q.path("id").asText());
        check(ids.containsAll(answers.answers().keySet()), "答案含试卷外题目");
        check(json(answers).length() <= 200000, "答案过长");
    }

    @Transactional
    public ExamResponse submit(Actor a, UUID id) {
        var row = owned(a, examMapper.lockAttempt(id));
        if (row.getStatus().equals("SUBMITTED")) return read(a, id);
        JsonNode snapshot = tree(row.getSnapshot()), answers = tree(row.getAnswers());
        ArrayNode details = JSON.createArrayNode();
        double score = 0, total = 0;
        boolean pending = false;
        for (var q : snapshot.path("questions")) {
            double points = q.path("points").asDouble(1);
            total += points;
            String type = q.path("type").asText();
            boolean subjective = Set.of("SHORT", "ESSAY", "CASE").contains(type);
            JsonNode answer = answers.path(q.path("id").asText());
            boolean correct = !subjective && correct(q, answer);
            if (correct) score += points;
            if (subjective) pending = true;
            details.add(
                JSON.valueToTree(
                    document(
                        "id",
                        q.path("id").asText(),
                        "version",
                        q.path("version").asInt(),
                        "correct",
                        subjective ? null : correct,
                        "score",
                        correct ? points : 0,
                        "pending",
                        subjective,
                        "knowledgeCodes",
                        q.path("knowledgeCodes")
                    )
                )
            );
            var prior = learningMapper
                .learning(a.id())
                .stream()
                .filter(l -> l.getEntityId().toString().equals(q.path("id").asText()))
                .findFirst()
                .orElseGet(LearningProjection::new);
            learningMapper.saveLearning(
                new SaveLearningCommand(
                    a.id(),
                    uuid(q.path("id").asText()),
                    q.path("version").asInt(),
                    Boolean.TRUE.equals(prior.getFavorite()),
                    subjective ? Boolean.TRUE.equals(prior.getWrong()) : !correct,
                    100
                )
            );
        }
        Instant now = Instant.now(), start = row.getStartedAt(), end = row.getDeadline();
        examMapper.submitAttempt(
            new SubmitAttemptCommand(
                id,
                JSON.valueToTree(
                    document(
                        "score",
                        score,
                        "total",
                        total,
                        "pendingManual",
                        pending,
                        "elapsedSeconds",
                        Duration.between(start, now.isBefore(end) ? now : end).toSeconds(),
                        "details",
                        details
                    )
                )
            )
        );
        return read(a, id);
    }

    private boolean correct(JsonNode q, JsonNode submitted) {
        if (!submitted.isArray()) return false;
        List<String> expected = new ArrayList<>(), actual = new ArrayList<>();
        q.path("answer").forEach(x -> expected.add(x.asText().trim()));
        submitted.forEach(x -> actual.add(x.asText().trim()));
        if (!q.path("type").asText().equals("FILL")) {
            Collections.sort(expected);
            Collections.sort(actual);
        }
        return expected.equals(actual);
    }

    @Transactional
    public ExamResponse selfGrade(Actor a, UUID id, SelfGradeRequest scores) {
        var row = owned(a, examMapper.lockAttempt(id));
        check(row.getStatus().equals("SUBMITTED"), "交卷后才能自评");
        var result = (ObjectNode) tree(row.getResult());
        check(result.path("pendingManual").asBoolean(), "评分已完成");
        var snapshot = tree(row.getSnapshot());
        double added = 0;
        for (var detail : result.path("details")) {
            if (!detail.path("pending").asBoolean()) continue;
            String qid = detail.path("id").asText();
            check(scores.scores().get(qid) != null, "请为每道主观题填写分数");
            double score = scores.scores().get(qid), max = 0;
            for (var q : snapshot.path("questions")) if (q.path("id").asText().equals(qid)) max = q
                .path("points")
                .asDouble();
            check(Double.isFinite(score) && score >= 0 && score <= max, "自评分数超出范围");
            ((ObjectNode) detail).put("score", score)
                .put("pending", false)
                .put("assessment", "SELF");
            added += score;
        }
        result
            .put("score", result.path("score").asDouble() + added)
            .put("pendingManual", false)
            .put("subjectiveAssessment", "SELF");
        examMapper.submitAttempt(new SubmitAttemptCommand(id, JSON.valueToTree(result)));
        auditMapper.audit(
            new AuditCommand(
                UUID.randomUUID(),
                a.id(),
                null,
                "EXAM_SELF_GRADE",
                id,
                "用户根据冻结评分要点完成主观题自评"
            )
        );
        return read(a, id);
    }
}
