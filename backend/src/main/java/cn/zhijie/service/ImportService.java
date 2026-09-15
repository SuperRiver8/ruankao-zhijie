package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.AdminUserMapper;
import cn.zhijie.dao.CertificateMapper;
import cn.zhijie.dao.ContentMapper;
import cn.zhijie.dao.ImportMapper;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.AttachmentService;
import cn.zhijie.service.ContentService;
import cn.zhijie.util.ResponseMapper;
import com.fasterxml.jackson.databind.*;
import com.networknt.schema.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {
    @org.springframework.beans.factory.annotation.Value("${app.jobs-enabled:true}")
    private boolean jobsEnabled = true;

    private final CertificateMapper certificateMapper;
    private final AdminUserMapper userMapper;
    private final ContentMapper contentMapper;
    private final ImportMapper importMapper;
    private final ContentService content;
    private final JsonSchema schema;
    private final AttachmentService attachments;

    public ImportService(
        CertificateMapper certificateMapper,
        AdminUserMapper userMapper,
        ContentMapper contentMapper,
        ImportMapper importMapper,
        ContentService content,
        AttachmentService attachments
    ) throws Exception {
        this.certificateMapper = certificateMapper;
        this.userMapper = userMapper;
        this.contentMapper = contentMapper;
        this.importMapper = importMapper;
        this.content = content;
        this.attachments = attachments;
        try (var in = getClass().getResourceAsStream("/import-schema.json")) {
            schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(
                JSON.readTree(in)
            );
        }
    }

    public JsonNode schema(Actor a) throws Exception {
        a.require("ADMIN", "EDITOR");
        try (var in = getClass().getResourceAsStream("/import-schema.json")) {
            return JSON.readTree(in);
        }
    }

    public ImportDictionaryResponse dictionary(Actor a) {
        a.require("ADMIN", "EDITOR");
        return new ImportDictionaryResponse(
            certificateMapper.certificates().stream().map(ResponseMapper::certificate).toList(),
            contentMapper
                .contents("KNOWLEDGE", false)
                .stream()
                .map(ResponseMapper::content)
                .toList(),
            contentMapper.contents("SYLLABUS", false).stream().map(ResponseMapper::content).toList()
        );
    }

    public PageResponse<ImportBatchResponse> list(Actor a, PageQuery query) {
        a.require("ADMIN", "EDITOR");
        return PageResponse.of(
            importMapper
                .batchesPage(a.id(), query)
                .stream()
                .map(ResponseMapper::importBatch)
                .toList(),
            importMapper.batchesCount(a.id(), query),
            query
        );
    }

    public JsonNode parse(Actor actor, MultipartFile file) throws Exception {
        check(!file.isEmpty() && file.getSize() <= 20 * 1024 * 1024, "导入文件为空或超过 20MB");
        if (
            !Objects.toString(file.getOriginalFilename(), "")
                .toLowerCase(Locale.ROOT)
                .endsWith(".zip")
        ) return JSON.readTree(file.getInputStream());
        Map<String, byte[]> entries = new LinkedHashMap<>();
        int total = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                check(
                    !name.startsWith("/") &&
                    !name.contains("\\") &&
                    !Arrays.asList(name.split("/")).contains("..") &&
                    !name.contains(":"),
                    "ZIP 路径不安全"
                );
                if (entry.isDirectory()) continue;
                check(
                    entries.size() < 1000 && !entries.containsKey(name),
                    "ZIP 条目过多或路径重复"
                );
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = zip.read(buffer)) != -1) {
                    total += n;
                    check(total <= 30 * 1024 * 1024, "ZIP 解压后超过 30MB");
                    out.write(buffer, 0, n);
                }
                entries.put(name, out.toByteArray());
            }
        }
        check(entries.containsKey("manifest.json"), "ZIP 缺少 manifest.json");
        JsonNode manifest = JSON.readTree(entries.get("manifest.json"));
        var merged = JSON.createObjectNode();
        merged.set("templateVersion", manifest.path("templateVersion"));
        merged.set("namespace", manifest.path("namespace"));
        var all = merged.putArray("entries");
        for (var path : manifest.path("files")) {
            String name = path.asText();
            check(
                entries.containsKey(name) &&
                name.endsWith(".json") &&
                !name.equals("manifest.json"),
                "manifest 引用的 JSON 文件不存在"
            );
            var list = JSON.readTree(entries.get(name));
            check(list.isArray(), "分文件必须为内容条目数组");
            list.forEach(all::add);
        }
        check(schema.validate(merged).isEmpty(), "ZIP 内容不符合模板 Schema");
        for (var item : all) resolveAttachments(actor, item.path("payload"), entries);
        return merged;
    }

    private void resolveAttachments(Actor actor, JsonNode payload, Map<String, byte[]> entries)
        throws Exception {
        if (payload.has("attachmentPaths")) {
            check(payload.path("attachmentPaths").isArray(), "attachmentPaths 必须是数组");
            var ids = JSON.createArrayNode();
            for (var old : payload.path("attachmentIds")) ids.add(old);
            for (var path : payload.path("attachmentPaths")) {
                String name = path.asText();
                check(
                    name.startsWith("attachments/") && entries.containsKey(name),
                    "附件引用不存在：" + name
                );
                ids.add(
                    attachments
                        .uploadBytes(actor, entries.get(name), name, "CONTENT")
                        .id()
                        .toString()
                );
            }
            ((com.fasterxml.jackson.databind.node.ObjectNode) payload).remove("attachmentPaths");
            ((com.fasterxml.jackson.databind.node.ObjectNode) payload).set("attachmentIds", ids);
        }
        for (var child : payload.path("children")) resolveAttachments(actor, child, entries);
    }

    public List<ImportReportItem> report(JsonNode root) {
        var errors = schema.validate(root);
        check(
            errors.isEmpty(),
            "模板不符合 Schema：" + errors.stream().limit(5).map(Object::toString).toList()
        );
        Set<String> staged = new HashSet<>();
        for (var entry : root.path("entries")) {
            check(
                staged.add(entry.path("kind").asText() + ":" + entry.path("externalId").asText()),
                "导入包中存在重复编码"
            );
        }
        Map<String, JsonNode> stagedPayloads = new HashMap<>();
        for (var item : root.path("entries")) stagedPayloads.put(
            item.path("kind").asText() + ":" + item.path("externalId").asText(),
            item.path("payload")
        );
        List<ImportReportItem> report = new ArrayList<>();
        String namespace = root.path("namespace").asText();
        int index = 0;
        for (var entry : root.path("entries")) {
            String kind = entry.path("kind").asText(), external = entry
                .path("externalId")
                .asText(), status = "NEW", message = "校验通过";
            try {
                content.validate(kind, entry.path("payload"));
                content.references(entry.path("payload"), namespace, staged);
                content.chapterReference(entry.path("payload"), namespace, stagedPayloads);
                var old = contentMapper.findContent(
                    new FindContentCommand(namespace, kind, external)
                );
                if (old != null) {
                    status = old.getChecksum().equals(hash(canonical(entry.path("payload"))))
                        ? "SKIP"
                        : "CONFLICT";
                    message = status.equals("SKIP")
                        ? "内容相同，将跳过"
                        : "内容变化；提交时需明确允许生成新版本";
                }
            } catch (Exception e) {
                status = "ERROR";
                message = e instanceof org.springframework.web.server.ResponseStatusException r
                    ? r.getReason()
                    : "数据格式无效";
            }
            report.add(new ImportReportItem(index++, kind, external, status, message));
        }
        return report;
    }

    @Transactional
    public ImportBatchResponse preview(Actor a, String key, MultipartFile file) throws Exception {
        a.require("ADMIN", "EDITOR");
        check(key != null && !key.isBlank() && key.length() <= 100, "缺少有效 Idempotency-Key");
        String checksum = hash(file.getBytes());
        var old = importMapper.batchByKey(new BatchByKeyCommand(a.id(), key));
        if (old != null) {
            check(old.getChecksum().equals(checksum), "相同请求键不能上传不同内容");
            return ResponseMapper.importBatch(old);
        }
        JsonNode root = parse(a, file);
        var report = report(root);
        UUID id = UUID.randomUUID();
        importMapper.insertBatch(
            new InsertBatchCommand(
                id,
                a.id(),
                key,
                checksum,
                JSON.valueToTree(root),
                JSON.valueToTree(report)
            )
        );
        return ResponseMapper.importBatch(importMapper.batch(id));
    }

    @Transactional
    public ImportBatchResponse commit(Actor a, UUID id, boolean allowUpdates) {
        if (!jobsEnabled) throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
            "当前环境未启用导入任务，请配置 JOBS_ENABLED=true 并重启后提交"
        );
        a.require("ADMIN", "EDITOR");
        var batch = found(importMapper.lockBatch(id));
        check(batch.getOwnerId().equals(a.id()), "只能提交本人的导入批次");
        if (
            Set.of("COMMITTED", "QUEUED").contains(batch.getStatus())
        ) return ResponseMapper.importBatch(batch);
        var report = report(tree(batch.getPayload()));
        check(report.stream().noneMatch(r -> r.status().equals("ERROR")), "存在校验错误，不能提交");
        check(
            allowUpdates || report.stream().noneMatch(r -> r.status().equals("CONFLICT")),
            "存在冲突，请确认生成新版本"
        );
        importMapper.enqueueBatch(new EnqueueBatchCommand(id, allowUpdates));
        return ResponseMapper.importBatch(importMapper.batch(id));
    }

    @Transactional
    public void process(UUID id) {
        var batch = found(importMapper.lockBatch(id));
        if (!batch.getStatus().equals("QUEUED")) return;
        var user = found(userMapper.user(uuid(batch.getOwnerId())));
        check(Boolean.TRUE.equals(user.getEnabled()), "导入账号不可用");
        Actor a = new Actor(
            uuid(user.getId()),
            user.getRole(),
            "",
            cn.zhijie.pojo.IdentityType.ADMIN
        );
        a.require("ADMIN", "EDITOR");
        JsonNode root = tree(batch.getPayload());
        var report = report(root);
        check(report.stream().noneMatch(r -> r.status().equals("ERROR")), "引用已变化，请重新校验");
        check(
            Boolean.TRUE.equals(batch.getAllowUpdates()) ||
            report.stream().noneMatch(r -> r.status().equals("CONFLICT")),
            "提交时出现新冲突，请重新确认"
        );
        for (var entry : root.path("entries")) {
            String kind = entry.path("kind").asText(), namespace = root
                .path("namespace")
                .asText(), external = entry.path("externalId").asText();
            var old = contentMapper.findContent(new FindContentCommand(namespace, kind, external));
            content.saveInternal(
                a,
                old == null ? null : uuid(old.getId()),
                kind,
                namespace,
                external,
                entry.path("payload")
            );
        }
        importMapper.finishBatch(new FinishBatchCommand(id, "COMMITTED", JSON.valueToTree(report)));
    }
}
