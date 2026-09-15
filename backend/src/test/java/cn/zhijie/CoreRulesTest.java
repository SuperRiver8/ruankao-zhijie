package cn.zhijie;

import static cn.zhijie.util.Support.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.zhijie.dao.*;
import cn.zhijie.integration.*;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.request.CertificateReviewRequest;
import cn.zhijie.service.AttachmentService;
import cn.zhijie.service.Audit;
import cn.zhijie.service.ContentService;
import cn.zhijie.service.MembershipService;
import cn.zhijie.util.*;
import java.util.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class CoreRulesTest {

    @Test
    void mapperXmlLoads() throws Exception {
        var config = new Configuration();
        config.getTypeHandlerRegistry().register(cn.zhijie.config.UuidTypeHandler.class);
        config.getTypeHandlerRegistry().register(cn.zhijie.config.JsonNodeTypeHandler.class);
        Class<?>[] mappers = {
            UserMapper.class,
            AdminUserMapper.class,
            CertificateMapper.class,
            MembershipMapper.class,
            CertificationMapper.class,
            AttachmentMapper.class,
            ContentMapper.class,
            ImportMapper.class,
            ExamMapper.class,
            LearningMapper.class,
            AuditMapper.class,
        };
        for (var mapper : mappers) {
            String resource = "/mappers/" + mapper.getSimpleName() + ".xml";
            try (var in = getClass().getResourceAsStream(resource)) {
                assertNotNull(in, resource);
                new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
            }
            for (var method : mapper.getDeclaredMethods()) {
                assertTrue(config.hasStatement(mapper.getName() + "." + method.getName()));
            }
        }
        var attachmentSql = config
            .getMappedStatement("cn.zhijie.dao.AttachmentMapper.attachmentPublished")
            .getBoundSql(UUID.randomUUID())
            .getSql();
        assertTrue(attachmentSql.contains("@>"), "格式化不能拆开 PostgreSQL JSON 运算符");
    }

    @Test
    void encryptionAndCanonicalHash() {
        var crypto = new Crypto(Base64.getEncoder().encodeToString(new byte[32]));
        String encrypted = crypto.encrypt("测试持证人");
        assertEquals("测试持证人", crypto.decrypt(encrypted));
        assertNotEquals(encrypted, crypto.encrypt("测试持证人"));
        assertEquals(canonical(tree("{\"b\":2,\"a\":1}")), canonical(tree("{\"a\":1,\"b\":2}")));
    }

    @Test
    void cannotApproveWithoutOfficialMatch() {
        UserMapper userMapper = mock(UserMapper.class);
        CertificationMapper certificationMapper = mock(CertificationMapper.class);
        MembershipMapper membershipMapper = mock(MembershipMapper.class);
        UUID id = UUID.randomUUID(), owner = UUID.randomUUID(), reviewer = UUID.randomUUID();
        when(certificationMapper.lockApplication(id)).thenReturn(application(owner));
        when(userMapper.lockUser(owner)).thenReturn(identity(true));
        var service = membership(userMapper, certificationMapper, membershipMapper);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
            service.review(
                new Actor(reviewer, "REVIEWER", "s", cn.zhijie.pojo.IdentityType.ADMIN),
                id,
                new CertificateReviewRequest(
                    "APPROVED",
                    "测试",
                    "UNCERTAIN",
                    null,
                    null,
                    null,
                    false
                )
            )
        );
        verify(membershipMapper, never()).grantCertificate(any());
        verify(membershipMapper, never()).setLevel(any());
    }

    @Test
    void cannotApproveWithoutIdentity() {
        UserMapper userMapper = mock(UserMapper.class);
        CertificationMapper certificationMapper = mock(CertificationMapper.class);
        MembershipMapper membershipMapper = mock(MembershipMapper.class);
        UUID id = UUID.randomUUID(), owner = UUID.randomUUID();
        when(certificationMapper.lockApplication(id)).thenReturn(application(owner));
        when(userMapper.lockUser(owner)).thenReturn(identity(false));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
            membership(userMapper, certificationMapper, membershipMapper).review(
                new Actor(UUID.randomUUID(), "REVIEWER", "s", cn.zhijie.pojo.IdentityType.ADMIN),
                id,
                new CertificateReviewRequest("APPROVED", "测试", "MATCH", null, null, null, false)
            )
        );
        verify(membershipMapper, never()).grantCertificate(any());
    }

    @Test
    void cannotReviewOwnApplication() {
        UserMapper userMapper = mock(UserMapper.class);
        CertificationMapper certificationMapper = mock(CertificationMapper.class);
        MembershipMapper membershipMapper = mock(MembershipMapper.class);
        UUID id = UUID.randomUUID(), owner = UUID.randomUUID();
        when(certificationMapper.lockApplication(id)).thenReturn(application(owner));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
            membership(userMapper, certificationMapper, membershipMapper).review(
                new Actor(owner, "ADMIN", "s", cn.zhijie.pojo.IdentityType.CUSTOMER),
                id,
                new CertificateReviewRequest("APPROVED", "测试", null, null, null, null, false)
            )
        );
        verify(membershipMapper, never()).grantCertificate(any());
    }

    @Test
    void questionAnswerMustReferenceExistingOption() {
        var content = new ContentService(
            mock(CertificateMapper.class),
            mock(AttachmentMapper.class),
            mock(ContentMapper.class),
            mock(Audit.class)
        );
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
            content.validate(
                "QUESTION",
                tree(
                    "{\"title\":\"题目\",\"stem\":\"题干\",\"type\":\"SINGLE\",\"difficulty\":1,\"options\":[{\"id\":\"A\",\"text\":\"甲\"},{\"id\":\"B\",\"text\":\"乙\"}],\"answer\":[\"C\"]}"
                )
            )
        );
    }

    @Test
    void examplePackagePassesSchema() throws Exception {
        try (var in = getClass().getResourceAsStream("/import-schema.json")) {
            var schema = com.networknt.schema.JsonSchemaFactory.getInstance(
                com.networknt.schema.SpecVersion.VersionFlag.V202012
            ).getSchema(JSON.readTree(in));
            var example = JSON.readTree(
                java.nio.file.Path.of("../templates/example.json").toFile()
            );
            assertTrue(schema.validate(example).isEmpty(), () -> schema.validate(example).toString()
            );
        }
    }

    private ApplicationProjection application(UUID owner) {
        var row = new ApplicationProjection();
        row.setUserId(owner);
        row.setStatus("PENDING");
        row.setOfficialResult("UNVERIFIED");
        return row;
    }

    private UserEntity identity(boolean verified) {
        var row = new UserEntity();
        row.setIdentityVerified(verified);
        return row;
    }

    private MembershipService membership(
        UserMapper userMapper,
        CertificationMapper certificationMapper,
        MembershipMapper membershipMapper
    ) {
        return new MembershipService(
            userMapper,
            mock(CertificateMapper.class),
            membershipMapper,
            certificationMapper,
            new Crypto(Base64.getEncoder().encodeToString(new byte[32])),
            mock(AttachmentService.class),
            mock(CertificateExtractor.class),
            mock(cn.zhijie.security.MemberCache.class),
            mock(Audit.class),
            mock(QrInspector.class)
        );
    }
}
