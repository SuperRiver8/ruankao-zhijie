package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.AttachmentService;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService service;

    public AttachmentController(AttachmentService service) {
        this.service = service;
    }

    @PostMapping
    ApiResponse<AttachmentResponse> upload(
        @AuthenticationPrincipal Actor a,
        @RequestParam MultipartFile file,
        @RequestParam(defaultValue = "PRIVATE") String access
    ) throws Exception {
        return ApiResponse.ok(service.upload(a, file, access));
    }

    @GetMapping("/{id}")
    ResponseEntity<FileSystemResource> read(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id
    ) {
        var f = service.authorize(a, id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(f.getMediaType()))
            .header("Cache-Control", "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .header(
                "Content-Disposition",
                ContentDisposition.inline()
                    .filename(f.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .body(new FileSystemResource(service.path(f)));
    }
}
