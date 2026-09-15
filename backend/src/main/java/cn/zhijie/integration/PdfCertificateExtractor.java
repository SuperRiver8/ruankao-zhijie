package cn.zhijie.integration;

import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfCertificateExtractor implements CertificateExtractor {

    public String extract(Path path, String mediaType) throws Exception {
        if (!mediaType.equals("application/pdf")) return "";
        try (var doc = Loader.loadPDF(path.toFile())) {
            if (doc.isEncrypted()) return "";
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(5);
            String text = stripper.getText(doc);
            return text.substring(0, Math.min(text.length(), 20000));
        }
    }
}
