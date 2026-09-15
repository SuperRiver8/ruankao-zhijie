package cn.zhijie;

import static cn.zhijie.util.Support.*;
import static org.junit.jupiter.api.Assertions.*;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.util.ResponseMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PojoContractTest {

    @Test
    void dynamicAnswersKeepOriginalDocumentShape() throws Exception {
        var answers = JSON.readValue("{\"q1\":[\"A\"],\"q2\":\"文字作答\"}", AnswersRequest.class);
        assertEquals("A", answers.answers().get("q1").get(0).asText());
        assertEquals("文字作答", answers.answers().get("q2").asText());
        assertEquals(
            2.5,
            JSON.readValue("{\"q1\":2.5}", SelfGradeRequest.class).scores().get("q1")
        );
        var application = JSON.readValue(
            "{\"obtainedOn\":\"2024-06-01\"}",
            ApplicationRequest.class
        );
        assertEquals(LocalDate.of(2024, 6, 1), application.obtainedOn());
    }

    @Test
    void explicitUserResponseExcludesSecretsAndUsesCamelCase() {
        var entity = new UserEntity();
        entity.setPasswordHash("secret");
        entity.setIdentityNameEncrypted("secret");
        entity.setIdentityEvidence("secret");
        entity.setMemberLevel(2);
        var response = JSON.valueToTree(ResponseMapper.user(entity));
        assertEquals(2, response.path("memberLevel").asInt());
        assertFalse(response.has("passwordHash"));
        assertFalse(response.has("identityNameEncrypted"));
        assertFalse(response.has("identityEvidence"));
    }

    @Test
    void dynamicContentRemainsJsonDocument() {
        var entity = new ContentProjection();
        entity.setPayload(tree("{\"body\":\"正文\"}"));
        assertEquals("正文", ResponseMapper.content(entity).payload().path("body").asText());
    }
}
