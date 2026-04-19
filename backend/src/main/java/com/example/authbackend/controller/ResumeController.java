package com.example.authbackend.controller;

import com.example.authbackend.dto.Result;
import com.example.authbackend.service.MinioService;
import com.example.authbackend.service.ResumeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {
    
    @Autowired
    private ResumeService resumeService;

    @Autowired
    private MinioService minioService;
    
    @PostMapping("/upload")
    public Result<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = resumeService.uploadResume(file, token);
            if ((Boolean) result.get("success")) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/info")
    public Result<?> getInfo(@RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = resumeService.getResume(token);
            if ((Boolean) result.get("success")) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取简历信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public void download(@RequestHeader("Authorization") String token, HttpServletResponse response) {
        try {
            Map<String, Object> result = resumeService.getResume(token);
            if (!(Boolean) result.get("success")) {
                response.sendError(400, "获取简历失败");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resumeData = (Map<String, Object>) result.get("data");
            if (resumeData == null) {
                response.sendError(404, "简历不存在");
                return;
            }

            String fileUrl = (String) resumeData.get("fileUrl");
            String fileName = (String) resumeData.get("fileName");

            InputStream inputStream = minioService.getFile(fileUrl);
            String contentType = resumeData.get("fileType") != null
                ? (String) resumeData.get("fileType")
                : "application/octet-stream";

            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            OutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(500, "下载失败: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
}