package com.hz.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * @author saber
 */
@RestController
@RequestMapping("/planningCheck")
public class FileUploadController {
    @Value("${myProject.upload.rootDir}")
    private String rootDir;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadSingleFile(@RequestParam("file") MultipartFile file) {
        // 检查是否上传了文件
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("请选择一个文件上传");
        }

//        // 校验文件扩展名
//        String originalFilename = file.getOriginalFilename();
//        if (originalFilename == null || !originalFilename.contains(".")) {
//            ResponseEntity.ok("文件格式不支持: " + originalFilename);
//        }

        try {
            // 确保存储目录存在
            Path uploadPath = Paths.get(rootDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成安全的文件名（防止路径遍历）
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            // 进一步清理文件名，如去除路径分隔符等
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            // 目标文件路径
            Path targetLocation = uploadPath.resolve(fileName);

            // 将上传的文件复制到目标位置（使用 StandardCopyOption.REPLACE_EXISTING 可覆盖）
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok("文件上传成功: " + fileName);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("文件上传失败: " + e.getMessage());
        }
    }
}