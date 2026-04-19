package com.example.authbackend.controller;

import com.example.authbackend.dto.Result;
import com.example.authbackend.service.ExcellentAnswerService;
import com.example.authbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/excellent-answer")
public class ExcellentAnswerController {

    @Autowired
    private ExcellentAnswerService excellentAnswerService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/add")
    public Result<Map<String, Object>> addExcellentAnswer(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam String category,
            @RequestHeader("Authorization") String token) {
        try {
            System.out.println("=== Excellent Answer Add Request ===");
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

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String role = jwtUtil.getRoleFromToken(token);
            System.out.println("UserId: " + jwtUtil.getUserIdFromToken(token) + ", Role: " + role);

            if (!"ADMIN".equals(role)) {
                return Result.error("无权限，只有管理员可以添加优秀回答");
            }

            Long userId = jwtUtil.getUserIdFromToken(token);

            var excellentAnswer = excellentAnswerService.addExcellentAnswer(
                title.trim(), content.trim(), category.trim(), userId
            );

            System.out.println("Add result: id=" + excellentAnswer.getId() + ", status=" + excellentAnswer.getStatus());

            return Result.success(Map.of(
                "id", excellentAnswer.getId(),
                "title", excellentAnswer.getTitle(),
                "content", excellentAnswer.getContent(),
                "category", excellentAnswer.getCategory(),
                "status", excellentAnswer.getStatus()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("=== Add Excellent Answer Error ===");
            System.out.println("Error: " + e.getClass().getName() + ": " + e.getMessage());
            return Result.error("添加失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        List<Map<String, Object>> items = excellentAnswerService.getExcellentAnswers(page, size);
        Map<String, Object> stats = excellentAnswerService.getStatistics();

        return Result.success(Map.of(
            "items", items,
            "total", stats.get("totalCount"),
            "page", page,
            "size", size
        ));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(excellentAnswerService.getStatistics());
    }

    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(excellentAnswerService.getCategories());
    }

    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchByCategory(@RequestParam String category) {
        return Result.success(excellentAnswerService.searchByCategory(category));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteExcellentAnswer(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String role = jwtUtil.getRoleFromToken(token);
        if (!"ADMIN".equals(role)) {
            return Result.error("无权限，只有管理员可以删除优秀回答");
        }

        excellentAnswerService.deleteExcellentAnswer(id);
        return Result.success(null);
    }
}