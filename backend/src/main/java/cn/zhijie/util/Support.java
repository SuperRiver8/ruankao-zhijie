package cn.zhijie.util;

import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Support {

    public static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private Support() {}

    public static com.fasterxml.jackson.databind.node.ObjectNode document(Object... pairs) {
        var result = JSON.createObjectNode();
        for (int i = 0; i < pairs.length; i += 2) result.set(
            pairs[i].toString(),
            JSON.valueToTree(pairs[i + 1])
        );
        return result;
    }

    public static Map<String, Object> map(Object... pairs) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) r.put(pairs[i].toString(), pairs[i + 1]);
        return r;
    }

    public static String json(Object o) {
        try {
            return JSON.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 无法序列化", e);
        }
    }

    public static JsonNode tree(Object o) {
        try {
            return JSON.readTree(o.toString());
        } catch (Exception e) {
            throw bad("JSON 格式不正确");
        }
    }

    public static UUID uuid(Object o) {
        try {
            return UUID.fromString(o.toString());
        } catch (Exception e) {
            throw bad("ID 格式不正确");
        }
    }

    public static String required(Map<String, Object> p, String key) {
        String v = Objects.toString(p.get(key), "").trim();
        if (v.isEmpty() || v.length() > 10000) throw bad(key + " 必填或长度超限");
        return v;
    }

    public static String required(String value, String key) {
        String v = Objects.toString(value, "").trim();
        check(!v.isEmpty() && v.length() <= 10000, key + " 必填或长度超限");
        return v;
    }

    public static ResponseStatusException bad(String s) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, s);
    }

    public static void check(boolean ok, String message) {
        if (!ok) throw bad(message);
    }

    public static <T> T found(T o) {
        if (o == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
        return o;
    }

    public static String hash(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String hash(String value) {
        return hash(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String canonical(JsonNode node) {
        if (node.isObject()) {
            var sorted = JSON.createObjectNode();
            var names = new TreeSet<String>();
            node.fieldNames().forEachRemaining(names::add);
            for (String name : names) sorted.set(name, tree(canonical(node.get(name))));
            return json(sorted);
        }
        if (node.isArray()) {
            var array = JSON.createArrayNode();
            for (var item : node) array.add(tree(canonical(item)));
            return json(array);
        }
        return json(node);
    }
}
