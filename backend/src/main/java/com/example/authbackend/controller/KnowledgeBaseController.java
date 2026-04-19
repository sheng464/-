package com.example.authbackend.controller;

import com.example.authbackend.dto.Result;
import com.example.authbackend.service.KnowledgeBaseService;
import com.example.authbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/add")
    public Result<?> addKnowledge(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @RequestHeader("Authorization") String token) {
        try {
            System.out.println("=== Knowledge Add Request ===");
            System.out.println("Title: " + title);
            System.out.println("Category: " + category);
            System.out.println("Content length: " + (content != null ? content.length() : "null"));
            System.out.println("Token: " + (token != null ? "present" : "null"));

            if (title == null || title.trim().isEmpty()) {
                return Result.error("标题不能为空");
            }
            if (content == null || content.trim().isEmpty()) {
                return Result.error("内容不能为空");
            }
            if (category == null || category.trim().isEmpty()) {
                return Result.error("分类不能为空");
            }

            Long userId = getUserIdFromToken(token);
            String role = getRoleFromToken(token);
            System.out.println("UserId: " + userId + ", Role: " + role);

            if (!"ADMIN".equals(role)) {
                return Result.error("无权限，只有管理员可以添加知识库");
            }
            Map<String, Object> result = knowledgeBaseService.addKnowledge(title.trim(), content.trim(), category.trim(), userId);
            System.out.println("Add result: " + result);
            return Result.success(result.get("data"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("=== Add Knowledge Error ===");
            System.out.println("Error: " + e.getClass().getName() + ": " + e.getMessage());
            return Result.error("添加失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    @PostMapping("/upload")
    public Result<?> uploadKnowledgeFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            String role = getRoleFromToken(token);
            if (!"ADMIN".equals(role)) {
                return Result.error("无权限，只有管理员可以上传知识库文件");
            }

            String content = extractTextFromFile(file);
            Map<String, Object> result = knowledgeBaseService.addKnowledge(title, content, category, userId);
            return Result.success(result.get("data"));
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result<?> updateKnowledge(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            String role = getRoleFromToken(token);
            if (!"ADMIN".equals(role)) {
                return Result.error("无权限，只有管理员可以修改知识库");
            }
            Map<String, Object> result = knowledgeBaseService.updateKnowledge(id, title, content, category, userId);
            return Result.success(result.get("data"));
        } catch (Exception e) {
            return Result.error("修改失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result<?> deleteKnowledge(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromToken(token);
            String role = getRoleFromToken(token);
            if (!"ADMIN".equals(role)) {
                return Result.error("无权限，只有管理员可以删除知识库");
            }
            knowledgeBaseService.deleteKnowledge(id, userId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<?> getKnowledgeList(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = knowledgeBaseService.getKnowledgeList(category, status, page, size);
            return Result.success(result.get("data"));
        } catch (Exception e) {
            return Result.error("获取列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<?> getKnowledgeById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = knowledgeBaseService.getKnowledgeById(id);
            return Result.success(result.get("data"));
        } catch (Exception e) {
            return Result.error("获取详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/categories")
    public Result<?> getCategories(@RequestHeader("Authorization") String token) {
        try {
            return Result.success(knowledgeBaseService.getCategories());
        } catch (Exception e) {
            return Result.error("获取分类失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Result<?> getStatistics(@RequestHeader("Authorization") String token) {
        try {
            String role = getRoleFromToken(token);
            if (!"ADMIN".equals(role)) {
                return Result.error("无权限，只有管理员可以查看统计数据");
            }
            return Result.success(knowledgeBaseService.getStatistics());
        } catch (Exception e) {
            return Result.error("获取统计失败: " + e.getMessage());
        }
    }

    @PostMapping("/reindex/{id}")
    public Result<?> reindexKnowledge(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            String role = getRoleFromToken(token);
            if (!"ADMIN".equals(role)) {
                return Result.error("无权限，只有管理员可以重建索引");
            }
            Map<String, Object> result = knowledgeBaseService.reindexKnowledge(id);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("重建索引失败: " + e.getMessage());
        }
    }

    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("无效的用户令牌");
        }
        return userId;
    }

    private String getRoleFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getRoleFromToken(token);
    }

    private String extractTextFromFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new Exception("文件名无效");
        }

        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".txt")) {
            return new String(file.getBytes(), "UTF-8");
        } else if (lowerName.endsWith(".md")) {
            return new String(file.getBytes(), "UTF-8");
        } else {
            throw new Exception("不支持的文件格式，请上传 .txt 或 .md 文件");
        }
    }
}