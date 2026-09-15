package cn.zhijie.integration;

import java.nio.file.Path;

// 可替换为获得授权的 OCR 服务；提取结果始终只用于辅助检查。
public interface CertificateExtractor {
    String extract(Path file, String mediaType) throws Exception;
}
