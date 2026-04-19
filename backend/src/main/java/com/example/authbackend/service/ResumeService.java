package com.example.authbackend.service;

import com.example.authbackend.entity.Resume;
import com.example.authbackend.repository.ResumeRepository;
import com.example.authbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ResumeService {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private MinioService minioService;

    @Autowired
    private AIService aiService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RagService ragService;

    /**
     * 上传简历文件，解析并保存到数据库
     * @param file 简历文件（PDF或DOCX格式）
     * @param token 用户认证令牌
     * @return 包含上传结果和简历信息的Map
     */
    public Map<String, Object> uploadResume(MultipartFile file, String token) throws Exception {
        Map<String, Object> result = new HashMap<>();

        Long userId = getUserIdFromToken(token);

        Optional<Resume> existingResume = resumeRepository.findByUserId(userId);
        Resume resume;
        String oldFileUrl = null;

        if (existingResume.isPresent()) {
            resume = existingResume.get();
            oldFileUrl = resume.getFileUrl();
        } else {
            resume = new Resume();
            resume.setUserId(userId);
        }

        Map<String, Object> parsedInfo = aiService.parseResume(file);

        String fileName = minioService.uploadFile(file);
        resume.setFileName(file.getOriginalFilename());
        resume.setFileSize(file.getSize());
        resume.setFileType(file.getContentType());
        resume.setFileUrl(fileName);

        resume.setName((String) parsedInfo.get("name"));
        resume.setPhone((String) parsedInfo.get("phone"));
        resume.setEmail((String) parsedInfo.get("email"));
        resume.setGender((String) parsedInfo.get("gender"));

        Object ageObj = parsedInfo.get("age");
        if (ageObj != null) {
            resume.setAge((Integer) ageObj);
        }

        resume.setEducation((String) parsedInfo.get("education"));
        resume.setPosition((String) parsedInfo.get("position"));
        resume.setSkills((String) parsedInfo.get("skills"));
        resume.setExperience((String) parsedInfo.get("experience"));

        resumeRepository.save(resume);

        if (oldFileUrl != null && !oldFileUrl.equals(fileName)) {
            try {
                minioService.deleteFile(oldFileUrl);
            } catch (Exception e) {
                System.err.println("删除旧文件失败: " + oldFileUrl);
            }
        }

        try {
            String resumeText = aiService.extractText(file);
            if (oldFileUrl != null) {
                ragService.deleteResumeFromIndex(resume.getId());
            }
            ragService.indexResume(resume.getId(), userId, resumeText);
        } catch (Exception e) {
            System.err.println("简历向量索引失败: " + e.getMessage());
        }

        result.put("success", true);
        result.put("message", "简历上传成功");
        result.put("data", buildResumeResponse(resume));

        return result;
    }

    /**
     * 获取当前用户的简历信息
     * @param token 用户认证令牌
     * @return 包含简历信息的Map，如果没有简历则data为null
     */
    public Map<String, Object> getResume(String token) {
        Map<String, Object> result = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(token);

            Optional<Resume> resumeOpt = resumeRepository.findByUserId(userId);

            if (resumeOpt.isPresent()) {
                result.put("success", true);
                result.put("data", buildResumeResponse(resumeOpt.get()));
            } else {
                result.put("success", true);
                result.put("data", null);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 构建简历响应对象，将Resume实体转换为前端需要的格式
     * @param resume 简历实体对象
     * @return 包含分类信息的响应Map（basicInfo、contact、intention、skills等）
     */
    private Map<String, Object> buildResumeResponse(Resume resume) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", resume.getId());
        response.put("fileName", resume.getFileName());
        response.put("fileUrl", resume.getFileUrl());
        response.put("fileSize", resume.getFileSize());
        response.put("fileType", resume.getFileType());
        response.put("updateTime", resume.getUpdatedAt());

        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("name", resume.getName() != null ? resume.getName() : "");
        basicInfo.put("gender", resume.getGender() != null ? resume.getGender() : "");
        basicInfo.put("age", resume.getAge() != null ? resume.getAge() : "");
        basicInfo.put("education", resume.getEducation() != null ? resume.getEducation() : "");
        response.put("basicInfo", basicInfo);

        Map<String, Object> contact = new HashMap<>();
        contact.put("phone", resume.getPhone() != null ? resume.getPhone() : "");
        contact.put("email", resume.getEmail() != null ? resume.getEmail() : "");
        response.put("contact", contact);

        Map<String, Object> intention = new HashMap<>();
        intention.put("position", resume.getPosition() != null ? resume.getPosition() : "");
        intention.put("salary", resume.getSalary() != null ? resume.getSalary() : "");
        intention.put("location", resume.getLocation() != null ? resume.getLocation() : "");
        intention.put("joinTime", resume.getJoinTime() != null ? resume.getJoinTime() : "");
        response.put("intention", intention);

        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
            response.put("skills", resume.getSkills().split(","));
        } else {
            response.put("skills", new String[]{});
        }

        return response;
    }

    /**
     * 从JWT令牌中提取用户ID
     * @param token 带有"Bearer "前缀的令牌字符串
     * @return 用户ID
     */
    private Long getUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("无效的令牌");
        }
        token = token.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("无效的用户令牌");
        }
        return userId;
    }
}
