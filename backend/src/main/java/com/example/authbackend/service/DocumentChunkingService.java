package com.example.authbackend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkingService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        text = text.replaceAll("\\s+", " ").trim();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());

            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf("。", end);
                int lastNewline = text.lastIndexOf("\n", end);
                int lastComma = text.lastIndexOf("，", end);

                int splitPoint = Math.max(lastPeriod, Math.max(lastNewline, lastComma));
                if (splitPoint > start + CHUNK_SIZE / 2) {
                    end = splitPoint + 1;
                }
            }

            String chunk = text.substring(start, end);
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end - CHUNK_OVERLAP;
            if (end == text.length()) {
                break;
            }
            if (start >= text.length()) {
                break;
            }
        }

        return chunks;
    }

    public List<String> chunkResumeBySections(String resumeText) {
        List<String> chunks = new ArrayList<>();

        String[] sections = {
            "基本信息", "个人信息", "姓名", "性别", "年龄", "学历", "教育背景",
            "工作经历", "工作经验", "工作经历", "项目经验", "项目经历",
            "技能", "技能证书", "专业技能", "技术栈", "自我评价", "个人简介"
        };

        String lowerText = resumeText.toLowerCase();
        int lastIndex = 0;

        for (String section : sections) {
            int index = lowerText.indexOf(section.toLowerCase());
            if (index > lastIndex) {
                String chunk = resumeText.substring(lastIndex, index).trim();
                if (!chunk.isEmpty() && chunk.length() > 50) {
                    chunks.add(chunk);
                }
                lastIndex = index;
            }
        }

        if (lastIndex < resumeText.length()) {
            String remaining = resumeText.substring(lastIndex).trim();
            if (!remaining.isEmpty() && remaining.length() > 50) {
                List<String> remainingChunks = chunkText(remaining);
                chunks.addAll(remainingChunks);
            }
        }

        if (chunks.isEmpty()) {
            chunks = chunkText(resumeText);
        }

        return chunks;
    }
}
