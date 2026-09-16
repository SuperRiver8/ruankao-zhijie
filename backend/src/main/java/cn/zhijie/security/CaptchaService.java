package cn.zhijie.security;

import static cn.zhijie.util.Support.*;

import cn.zhijie.config.CaptchaProperties;
import cn.zhijie.pojo.IdentityType;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.request.CaptchaChallengeRequest.Scene;
import cn.zhijie.pojo.response.CaptchaChallengeResponse;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CaptchaService {

    private static final int WIDTH = 320, HEIGHT = 160, SIZE = 48;
    private final CaptchaStore store;
    private final RateLimiter limiter;
    private final CaptchaProperties config;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService(CaptchaStore store, RateLimiter limiter, CaptchaProperties config) {
        this.store = store;
        this.limiter = limiter;
        this.config = config;
    }

    private String binding(Scene scene, String username, String ip) {
        // 哈希后的绑定信息不在缓存键中暴露账号或来源地址。
        return hash(
            scene.name() + "\n" + cn.zhijie.util.Support.required(username, "username") + "\n" + ip
        );
    }

    public Scene loginScene(IdentityType type) {
        return type == IdentityType.ADMIN ? Scene.ADMIN_LOGIN : Scene.CUSTOMER_LOGIN;
    }

    public boolean required(IdentityType type, String username, String ip) {
        return (
            store.failures(
                binding(loginScene(type), username, ip),
                Duration.ofSeconds(config.windowSeconds()),
                false
            ) >=
            config.failureThreshold()
        );
    }

    public boolean failed(IdentityType type, String username, String ip) {
        return (
            store.failures(
                binding(loginScene(type), username, ip),
                Duration.ofSeconds(config.windowSeconds()),
                true
            ) >=
            config.failureThreshold()
        );
    }

    public void succeeded(IdentityType type, String username, String ip) {
        store.clearFailures(binding(loginScene(type), username, ip));
    }

    public void verify(Scene scene, String username, String ip, CaptchaAnswer answer) {
        if (answer == null) throw CaptchaException.required();
        if (
            answer.challengeId() == null || !answer.challengeId().matches("[A-Za-z0-9_-]{43}")
        ) throw new CaptchaException("CAPTCHA_INVALID", "拼图验证失败，请重新拖动");
        String value = store
            .consumeChallenge(answer.challengeId())
            .orElseThrow(() ->
                new CaptchaException("CAPTCHA_EXPIRED", "拼图已过期或已使用，请刷新后重试")
            );
        String[] fields = value.split(":", 2);
        if (
            !fields[0].equals(binding(scene, username, ip)) ||
            answer.offsetX() == null ||
            !Double.isFinite(answer.offsetX()) ||
            answer.offsetX() < 0 ||
            answer.offsetX() > WIDTH - SIZE ||
            Math.abs(Integer.parseInt(fields[1]) - answer.offsetX()) > config.tolerance()
        ) throw new CaptchaException("CAPTCHA_INVALID", "拼图验证失败，请重新拖动");
    }

    public CaptchaChallengeResponse challenge(CaptchaChallengeRequest request, String ip) {
        if (
            limiter.increment(
                "captcha:issue:" + ip,
                Duration.ofSeconds(config.issueWindowSeconds())
            ) >
            config.issueLimit()
        ) throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS,
            "获取拼图过于频繁，请稍后重试"
        );
        String bound = binding(request.scene(), request.username(), ip);
        int x = 64 + random.nextInt(WIDTH - SIZE - 64 - 8), y =
            12 + random.nextInt(HEIGHT - SIZE - 24);
        var background = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = background.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, color(), WIDTH, HEIGHT, color()));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        for (int i = 0; i < 70; i++) {
            g.setColor(
                new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 100)
            );
            g.fillOval(
                random.nextInt(WIDTH),
                random.nextInt(HEIGHT),
                12 + random.nextInt(65),
                12 + random.nextInt(65)
            );
            g.drawLine(
                random.nextInt(WIDTH),
                random.nextInt(HEIGHT),
                random.nextInt(WIDTH),
                random.nextInt(HEIGHT)
            );
        }
        // 圆形凸起与凹口构成真正的拼图轮廓；只向前端提供纵坐标。
        Area shape = new Area(new Rectangle2D.Double(4, 10, 34, 34));
        shape.add(new Area(new Ellipse2D.Double(15, 1, 12, 18)));
        shape.add(new Area(new Ellipse2D.Double(30, 20, 17, 12)));
        shape.subtract(new Area(new Ellipse2D.Double(-4, 20, 17, 12)));
        var piece = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = piece.createGraphics();
        pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pg.setClip(shape);
        pg.drawImage(background, -x, -y, null);
        pg.setClip(null);
        pg.setColor(Color.WHITE);
        pg.setStroke(new BasicStroke(1.5f));
        pg.draw(shape);
        pg.dispose();
        Shape hole = AffineTransform.getTranslateInstance(x, y).createTransformedShape(shape);
        g.setColor(new Color(0, 0, 0, 155));
        g.fill(hole);
        g.setColor(new Color(255, 255, 255, 180));
        g.draw(hole);
        g.dispose();
        String backgroundPng = png(background), piecePng = png(piece);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        store.saveChallenge(id, bound + ":" + x, Duration.ofSeconds(config.challengeTtlSeconds()));
        return new CaptchaChallengeResponse(
            id,
            backgroundPng,
            piecePng,
            WIDTH,
            HEIGHT,
            SIZE,
            SIZE,
            y,
            config.challengeTtlSeconds()
        );
    }

    private Color color() {
        return Color.getHSBColor(random.nextFloat(), 0.45f, 0.8f);
    }

    private String png(BufferedImage image) {
        try {
            var out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (java.io.IOException error) {
            throw new IllegalStateException("无法生成拼图", error);
        }
    }
}
