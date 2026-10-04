package com.hz.web.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author saber
 */
@RestController
@RequestMapping("/planningCheck")
public class FileDownloadController {
    @Value("${myProject.download.fileDir}")
    private String fileDir;

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(fileDir).resolve(fileName).normalize();
            Resource resource = new FileSystemResource(filePath);

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException e) {
                // 忽略探测失败
            }

            // 关键修改：使用 ContentDisposition 构建带中文文件名的响应头
            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(Objects.requireNonNull(resource.getFilename()), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}