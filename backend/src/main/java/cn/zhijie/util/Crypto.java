package cn.zhijie.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Crypto {

    private final byte[] key;

    public Crypto(@Value("${app.encryption-key}") String key) {
        this.key = Base64.getDecoder().decode(key);
        if (this.key.length != 32) throw new IllegalArgumentException(
            "ENCRYPTION_KEY 必须是 Base64 编码的 32 字节密钥"
        );
    }

    public String encrypt(String s) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, iv)
            );
            byte[] data = c.doFinal(s.getBytes(StandardCharsets.UTF_8));
            return (
                Base64.getEncoder().encodeToString(iv) +
                "." +
                Base64.getEncoder().encodeToString(data)
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String decrypt(Object o) {
        try {
            String[] p = o.toString().split("\\.");
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, Base64.getDecoder().decode(p[0]))
            );
            return new String(c.doFinal(Base64.getDecoder().decode(p[1])), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }

    public String fingerprint(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
