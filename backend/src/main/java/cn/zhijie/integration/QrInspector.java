package cn.zhijie.integration;

import static cn.zhijie.util.Support.*;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QrInspector {

    private final Set<String> domains;

    public QrInspector(@Value("${app.official-domains}") String domains) {
        this.domains = new HashSet<>(Arrays.asList(domains.toLowerCase(Locale.ROOT).split(",")));
    }

    public Map<String, Object> inspect(Path file, String media) {
        try {
            BufferedImage image;
            if (media.equals("application/pdf")) {
                try (var doc = Loader.loadPDF(file.toFile())) {
                    if (doc.getNumberOfPages() == 0) return map("status", "UNREADABLE");
                    var box = doc.getPage(0).getMediaBox();
                    if (box.getWidth() * box.getHeight() * 2 > 20000000) return map(
                        "status",
                        "UNREADABLE"
                    );
                    image = new PDFRenderer(doc).renderImageWithDPI(0, 100);
                }
            } else {
                try (var stream = ImageIO.createImageInputStream(file.toFile())) {
                    var readers = ImageIO.getImageReaders(stream);
                    if (!readers.hasNext()) return map("status", "UNREADABLE");
                    var reader = readers.next();
                    try {
                        reader.setInput(stream);
                        if ((long) reader.getWidth(0) * reader.getHeight(0) > 20000000) return map(
                            "status",
                            "UNREADABLE"
                        );
                        image = reader.read(0);
                    } finally {
                        reader.dispose();
                    }
                }
            }
            String text = new MultiFormatReader()
                .decode(
                    new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))
                )
                .getText();
            URI uri = URI.create(text);
            String host = Objects.toString(uri.getHost(), "").toLowerCase(Locale.ROOT);
            boolean official =
                "https".equals(uri.getScheme()) &&
                domains.stream().anyMatch(d -> host.equals(d) || host.endsWith("." + d));
            return map(
                "status",
                "DETECTED",
                "host",
                host,
                "officialDomain",
                official,
                "notice",
                "仅判断域名，不访问二维码地址，不代表官方查验通过"
            );
        } catch (Exception e) {
            return map("status", "UNREADABLE", "notice", "二维码未识别，继续人工查验");
        }
    }
}
