package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.AttachmentMapper;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.Audit;
import cn.zhijie.util.ResponseMapper;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final Path root;
    private final Audit audit;

    public AttachmentService(
        AttachmentMapper attachmentMapper,
        Audit audit,
        @Value("${app.attachment-dir}") String root
    ) throws Exception {
        this.attachmentMapper = attachmentMapper;
        this.audit = audit;
        this.root = Path.of(root).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public AttachmentResponse upload(Actor a, MultipartFile f, String access) throws Exception {
        return uploadBytes(
            a,
            f.getBytes(),
            Objects.requireNonNullElse(f.getOriginalFilename(), "附件"),
            access
        );
    }

    public AttachmentResponse uploadBytes(Actor a, byte[] b, String name, String access)
        throws Exception {
        check(Set.of("PRIVATE", "CONTENT").contains(access), "附件访问级别无效");
        if (access.equals("CONTENT")) a.require("ADMIN", "EDITOR");
        check(b.length > 0 && b.length <= 20 * 1024 * 1024, "文件为空或超过 20MB");
        String type = detect(b);
        check(type != null, "文件格式不支持");
        if (access.equals("PRIVATE")) check(
            Set.of("application/pdf", "image/png", "image/jpeg", "image/webp").contains(type),
            "证明材料仅支持 PDF 与图片"
        );
        if (access.equals("CONTENT")) {
            var existing = attachmentMapper.contentAttachment(
                new ContentAttachmentCommand(a.id(), hash(b))
            );
            if (existing != null) return new AttachmentResponse(
                existing.getId(),
                name,
                type,
                b.length
            );
        }
        UUID id = UUID.randomUUID();
        String key = id.toString();
        Path path = root.resolve(key);
        Files.write(path, b, StandardOpenOption.CREATE_NEW);
        try {
            attachmentMapper.insertAttachment(
                new InsertAttachmentCommand(
                    id,
                    a.isAdmin() ? null : a.id(),
                    a.isAdmin() ? a.id() : null,
                    Path.of(name).getFileName().toString(),
                    key,
                    type,
                    (long) (b.length),
                    hash(b),
                    access
                )
            );
        } catch (Exception e) {
            Files.deleteIfExists(path);
            throw e;
        }
        if (
            org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()
        ) org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) try {
                        Files.deleteIfExists(path);
                    } catch (java.io.IOException ignored) {
                        /* 清理由运维孤立附件检查补偿。 */
                    }
                }
            }
        );
        return new AttachmentResponse(id, name, type, b.length);
    }

    private String detect(byte[] b) {
        if (b.length < 12) return null;
        if (
            b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-'
        ) return "application/pdf";
        if (
            (b[0] & 255) == 137 &&
            b[1] == 80 &&
            b[2] == 78 &&
            b[3] == 71 &&
            b[4] == 13 &&
            b[5] == 10
        ) return "image/png";
        if ((b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255) return "image/jpeg";
        String first = new String(b, 0, 4, java.nio.charset.StandardCharsets.US_ASCII), brand =
            new String(b, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if (first.equals("RIFF") && brand.equals("WEBP")) return "image/webp";
        if (first.equals("RIFF") && brand.equals("WAVE")) return "audio/wav";
        if (first.equals("OggS")) return "audio/ogg";
        if (b[0] == 'I' && b[1] == 'D' && b[2] == '3') return "audio/mpeg";
        if (
            new String(b, 4, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp") &&
            Set.of("isom", "iso2", "mp41", "mp42", "avc1", "M4V ").contains(brand)
        ) return "video/mp4";
        return null;
    }

    public AttachmentEntity ownedPrivate(UUID id, UUID user) {
        var f = found(attachmentMapper.attachment(id));
        check(
            Objects.equals(f.getOwnerId(), user) &&
            f.getAdminOwnerId() == null &&
            f.getAccessLevel().equals("PRIVATE"),
            "只能使用本人上传的私有证明材料"
        );
        return f;
    }

    public AttachmentEntity authorize(Actor a, UUID id) {
        var f = found(attachmentMapper.attachment(id));
        boolean own = a.isAdmin()
            ? Objects.equals(f.getAdminOwnerId(), a.id())
            : Objects.equals(f.getOwnerId(), a.id());
        boolean reviewer = a.isAdmin() && Set.of("ADMIN", "REVIEWER").contains(a.role());
        boolean content = f.getAccessLevel().equals("CONTENT");
        boolean editor = a.isAdmin() && Set.of("ADMIN", "EDITOR").contains(a.role());
        if (
            !(own ||
                (!content && reviewer) ||
                (content && (editor || attachmentMapper.attachmentPublished(id))))
        ) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无附件访问权限");
        audit.log(a, "ATTACHMENT_READ", id, "读取证明或内容附件");
        return f;
    }

    public Path path(AttachmentEntity f) {
        Path p = root.resolve(f.getStorageKey()).normalize();
        check(p.startsWith(root), "存储路径错误");
        return p;
    }
}
