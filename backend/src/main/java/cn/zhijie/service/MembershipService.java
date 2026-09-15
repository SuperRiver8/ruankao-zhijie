package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.*;
import cn.zhijie.integration.*;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.security.MemberCache;
import cn.zhijie.util.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

@Service
public class MembershipService {

    private final UserMapper users;
    private final CertificateMapper certificates;
    private final MembershipMapper memberships;
    private final CertificationMapper applications;
    private final Crypto crypto;
    private final AttachmentService files;
    private final CertificateExtractor extractor;
    private final MemberCache memberCache;
    private final Audit audit;
    private final QrInspector qr;

    public MembershipService(
        UserMapper users,
        CertificateMapper certificates,
        MembershipMapper memberships,
        CertificationMapper applications,
        Crypto crypto,
        AttachmentService files,
        CertificateExtractor extractor,
        MemberCache memberCache,
        Audit audit,
        QrInspector qr
    ) {
        this.users = users;
        this.certificates = certificates;
        this.memberships = memberships;
        this.applications = applications;
        this.crypto = crypto;
        this.files = files;
        this.extractor = extractor;
        this.memberCache = memberCache;
        this.audit = audit;
        this.qr = qr;
    }

    @Transactional
    public IdResponse submit(Actor actor, UUID existing, ApplicationRequest request) {
        actor.requireCustomer();
        UUID id = existing == null ? UUID.randomUUID() : existing;
        if (existing != null) {
            var old = found(applications.lockApplication(id));
            check(old.getUserId().equals(actor.id()), "只能修改本人申请");
            check("SUPPLEMENT".equals(old.getStatus()), "仅退回补充状态可以重新提交");
        }
        String name = required(request.holderName(), "持证人姓名"), number = required(
            request.number(),
            "编号"
        )
            .replaceAll("\\s+", "")
            .toUpperCase(Locale.ROOT);
        check(
            Set.of("CERTIFICATE", "MANAGEMENT", "QUERY").contains(request.numberType()),
            "编号类型错误"
        );
        check(name.length() <= 100 && number.length() <= 100, "姓名或编号过长");
        var certificate = found(certificates.certificate(request.certificateId()));
        check(Boolean.TRUE.equals(certificate.getEnabled()), "证书已停用");
        var file = files.ownedPrivate(request.attachmentId(), actor.id());
        if (request.auxiliaryId() != null) files.ownedPrivate(request.auxiliaryId(), actor.id());
        LocalDate obtained = Objects.requireNonNull(request.obtainedOn());
        check(!obtained.isAfter(LocalDate.now()) && obtained.getYear() >= 1980, "取得日期不合理");
        String fingerprint = crypto.fingerprint(number), text = "";
        try {
            text = extractor.extract(files.path(file), file.getMediaType());
        } catch (Exception ignored) {
            /* 提取失败仍允许人工审核。 */
        }
        boolean dateFound =
            text.contains(obtained.toString()) ||
            text.contains(
                obtained.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日"))
            ) ||
            text.contains(obtained.toString().replace('-', '/')) ||
            text.contains(obtained.toString().replace('-', '.'));
        boolean nameFound = text.contains(name), numberFound = text
            .replaceAll("\\s+", "")
            .toUpperCase(Locale.ROOT)
            .contains(number), certificateFound = text.contains(certificate.getName());
        ObjectNode details = JSON.createObjectNode()
            .put("textEncrypted", crypto.encrypt(text))
            .put(
                "duplicate",
                applications.duplicateCount(new DuplicateCountCommand(fingerprint, id)) > 0
            )
            .put("nameFound", nameFound)
            .put("numberFound", numberFound)
            .put("certificateFound", certificateFound)
            .put("dateFound", dateFound)
            .put("numberFormatPlausible", number.matches("[A-Z0-9-]{6,100}"))
            .put(
                "notice",
                "文本提取与二维码仅辅助检查，不能证明真实或归属；图片 OCR 可接入扩展服务"
            );
        details.set("qr", JSON.valueToTree(qr.inspect(files.path(file), file.getMediaType())));
        String autoResult = text.isBlank()
            ? "UNREADABLE"
            : nameFound && numberFound && certificateFound && dateFound
                ? "CONSISTENT"
                : "DIFFERENCES";
        String holder = crypto.encrypt(name), encryptedNumber = crypto.encrypt(number), note =
            Objects.requireNonNullElse(request.note(), "");
        if (existing == null) applications.insertApplication(
            new InsertApplicationCommand(
                id,
                actor.id(),
                request.certificateId(),
                holder,
                request.numberType(),
                encryptedNumber,
                fingerprint,
                obtained,
                request.attachmentId(),
                request.auxiliaryId(),
                note,
                autoResult,
                details
            )
        );
        else applications.resubmitApplication(
            new ResubmitApplicationCommand(
                id,
                actor.id(),
                request.certificateId(),
                holder,
                request.numberType(),
                encryptedNumber,
                fingerprint,
                obtained,
                request.attachmentId(),
                request.auxiliaryId(),
                note,
                autoResult,
                details
            )
        );
        applications.snapshotApplication(id);
        audit.log(
            actor,
            "CERTIFICATE_SUBMIT",
            id,
            "提交第 " + applications.application(id).getRevision() + " 版申请"
        );
        return new IdResponse(id);
    }

    public PageResponse<ApplicationResponse> list(Actor actor, boolean admin, PageQuery query) {
        if (admin) actor.require("ADMIN", "REVIEWER");
        else actor.requireCustomer();
        return PageResponse.of(
            applications
                .applicationsPage(admin ? null : actor.id(), query)
                .stream()
                .map(row -> safe(row, false, null, null))
                .toList(),
            applications.applicationsCount(admin ? null : actor.id(), query),
            query
        );
    }

    public ApplicationResponse detail(Actor actor, UUID id) {
        var row = found(applications.application(id));
        if (!actor.ownsCustomer(row.getUserId())) actor.require("ADMIN", "REVIEWER");
        audit.log(actor, "CERTIFICATE_DETAIL_READ", id, "查看申请敏感字段及查验材料");
        List<VerificationResponse> verifications = null;
        if (Set.of("ADMIN", "REVIEWER").contains(actor.role())) verifications = applications
            .verifications(id)
            .stream()
            .map(v ->
                new VerificationResponse(
                    v.getId(),
                    v.getApplicationId(),
                    v.getReviewerId(),
                    v.getResult(),
                    v.getOfficialSource(),
                    crypto.decrypt(v.getEvidence()),
                    crypto.decrypt(v.getOwnershipEvidence()),
                    v.getCreatedAt()
                )
            )
            .toList();
        return safe(
            row,
            true,
            applications.reviews(id).stream().map(ResponseMapper::review).toList(),
            verifications
        );
    }

    private ApplicationResponse safe(
        ApplicationProjection row,
        boolean full,
        List<ReviewResponse> reviews,
        List<VerificationResponse> verifications
    ) {
        String name = crypto.decrypt(row.getHolderNameEncrypted()), number = crypto.decrypt(
            row.getNumberEncrypted()
        );
        ObjectNode details = row.getAutoDetails().deepCopy();
        details.remove("textEncrypted");
        if (full) details.put(
            "extractedText",
            crypto.decrypt(row.getAutoDetails().path("textEncrypted").asText())
        );
        return new ApplicationResponse(
            row.getId(),
            row.getUserId(),
            row.getCertificateId(),
            row.getCertificateName(),
            row.getUsername(),
            full ? name : name.substring(0, 1) + "**",
            full ? number : "****" + number.substring(Math.max(0, number.length() - 4)),
            row.getNumberType(),
            row.getObtainedOn(),
            row.getAttachmentId(),
            row.getAuxiliaryId(),
            row.getNote(),
            row.getRevision(),
            row.getAutoResult(),
            details,
            row.getOfficialResult(),
            row.getStatus(),
            row.getCreatedAt(),
            row.getUpdatedAt(),
            reviews,
            verifications
        );
    }

    @Transactional
    public void review(Actor actor, UUID id, CertificateReviewRequest request) {
        actor.require("ADMIN", "REVIEWER");
        var row = found(applications.lockApplication(id));
        check(!actor.ownsCustomer(row.getUserId()), "不能审核自己的申请");
        String action = required(request.action(), "审核操作"), reason = required(
            request.reason(),
            "审核理由"
        );
        check(
            Set.of("REVIEWING", "SUPPLEMENT", "APPROVED", "REJECTED", "REVOKED").contains(action),
            "审核操作无效"
        );
        check(
            action.equals("REVOKED")
                ? row.getStatus().equals("APPROVED")
                : Set.of("PENDING", "REVIEWING").contains(row.getStatus()),
            "申请状态已变化，不能执行此操作"
        );
        var user = found(users.lockUser(row.getUserId()));
        String official = Objects.requireNonNullElse(
            request.officialResult(),
            row.getOfficialResult()
        );
        check(
            Set.of("UNVERIFIED", "MATCH", "MISMATCH", "UNCERTAIN").contains(official),
            "核验结果无效"
        );
        if (!action.equals("REVOKED") && !action.equals("REVIEWING")) {
            String source = Objects.requireNonNullElse(request.officialSource(), ""), evidence =
                Objects.requireNonNullElse(request.evidence(), ""), ownership =
                Objects.requireNonNullElse(request.ownershipEvidence(), "");
            if (action.equals("APPROVED")) {
                check(official.equals("MATCH"), "通过审核必须有官方信息匹配结果");
                check(Boolean.TRUE.equals(user.getIdentityVerified()), "账号尚未核实身份");
                check(
                    crypto
                        .decrypt(user.getIdentityNameEncrypted())
                        .equals(crypto.decrypt(row.getHolderNameEncrypted())),
                    "已核实身份与持证姓名不一致，请补充证明"
                );
                check(
                    !source.isBlank() && !evidence.isBlank() && !ownership.isBlank(),
                    "需填写官方查验来源、依据及账号归属依据"
                );
                check(
                    Boolean.TRUE.equals(request.ownershipConfirmed()),
                    "必须确认持证人与账号归属一致"
                );
            }
            applications.insertVerification(
                new InsertVerificationCommand(
                    UUID.randomUUID(),
                    id,
                    actor.id(),
                    official,
                    source,
                    crypto.encrypt(evidence),
                    crypto.encrypt(ownership)
                )
            );
        }
        applications.setApplicationState(new SetApplicationStateCommand(id, action, official));
        applications.insertReview(
            new InsertReviewCommand(UUID.randomUUID(), id, actor.id(), action, reason)
        );
        if (action.equals("APPROVED")) memberships.grantCertificate(
            new GrantCertificateCommand(
                UUID.randomUUID(),
                row.getUserId(),
                row.getCertificateId(),
                id,
                row.getNumberHash()
            )
        );
        if (action.equals("REVOKED")) memberships.revokeCertificate(id);
        if (action.equals("APPROVED") || action.equals("REVOKED")) {
            int level = memberships.calculatedLevel(user.getId());
            memberships.setLevel(new SetLevelCommand(user.getId(), level));
            if (user.getMemberLevel() != level) memberships.levelHistory(
                new LevelHistoryCommand(
                    UUID.randomUUID(),
                    user.getId(),
                    user.getMemberLevel(),
                    level,
                    id
                )
            );
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    public void afterCommit() {
                        try {
                            memberCache.invalidate(user.getId());
                        } catch (Exception e) {
                            org.slf4j.LoggerFactory.getLogger(MembershipService.class).warn(
                                "会员缓存失效失败，资料接口仍直接读取数据库",
                                e
                            );
                        }
                    }
                }
            );
        }
        audit.log(actor, "CERTIFICATE_" + action, id, reason);
    }

    @Transactional
    public void identity(Actor actor, UUID id, IdentityRequest request) {
        actor.require("ADMIN", "REVIEWER");
        check(!actor.ownsCustomer(id), "不能核实自己的身份");
        var user = found(users.lockUser(id));
        check(!Boolean.TRUE.equals(user.getIdentityVerified()), "已核实身份不得直接覆盖");
        users.verifyIdentity(
            new VerifyIdentityCommand(
                id,
                crypto.encrypt(required(request.holderName(), "姓名")),
                crypto.encrypt(required(request.evidence(), "核实依据"))
            )
        );
        audit.log(actor, "IDENTITY_VERIFIED", id, "人工核实账号身份并保存依据");
    }
}
