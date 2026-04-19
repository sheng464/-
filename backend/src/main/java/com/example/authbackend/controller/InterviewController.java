package com.example.authbackend.controller;

import com.example.authbackend.dto.Result;
import com.example.authbackend.service.InterviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    @PostMapping("/start")
    public Result<?> startInterview(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "resumeId", required = false) Long resumeId,
            @RequestParam("position") String position,
            @RequestParam(value = "sessionType", defaultValue = "TEXT") String sessionType,
            @RequestParam(value = "interviewType", defaultValue = "PROJECT") String interviewType,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = interviewService.startInterview(file, resumeId, position, token, sessionType, interviewType);
            if ((Boolean) result.get("success")) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("[ERROR] startInterview failed: " + errorMsg);
            return Result.error("面试启动失败: " + errorMsg);
        }
    }

    @PostMapping("/chat")
    public Result<?> chat(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            String message = (String) request.get("message");
            String sessionId = (String) request.get("sessionId");

            Map<String, Object> result = interviewService.chat(message, sessionId, token);
            if ((Boolean) result.get("success")) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("消息发送失败: " + e.getMessage());
        }
    }

    @GetMapping(value = "/chat/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @PathVariable String sessionId,
            @RequestParam String message,
            @RequestHeader("Authorization") String token) {
        SseEmitter emitter = new SseEmitter(300000L);

        interviewService.chatStream(message, sessionId, token, emitter);

        emitter.onCompletion(() -> System.out.println("[SSE] Completed for session: " + sessionId));
        emitter.onTimeout(() -> System.out.println("[SSE] Timeout for session: " + sessionId));
        emitter.onError(e -> System.out.println("[SSE] Error for session: " + sessionId + " - " + e.getMessage()));

        return emitter;
    }

    @GetMapping("/history/{sessionId}")
    public Result<?> getHistory(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            return Result.success(interviewService.getHistory(sessionId, token));
        } catch (Exception e) {
            return Result.error("获取历史记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/sessions")
    public Result<?> getSessions(@RequestHeader("Authorization") String token) {
        try {
            return Result.success(interviewService.getSessions(token));
        } catch (Exception e) {
            return Result.error("获取面试记录失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/session/{sessionId}")
    public Result<?> deleteSession(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            interviewService.deleteSession(sessionId, token);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/voice")
    public Result<?> voiceChat(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("sessionId") String sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = interviewService.voiceChat(audio, sessionId, token);
            if ((Boolean) result.get("success")) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("语音消息处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/voice/start")
    public Result<?> startVoiceInterview(
            @RequestParam("sessionId") String sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = interviewService.startVoiceInterview(sessionId, token);
            if ((Boolean) result.get("success")) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("语音面试启动失败: " + e.getMessage());
        }
    }

    @PostMapping("/evaluate/{sessionId}")
    public Result<?> evaluateAbility(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = interviewService.evaluateAbility(sessionId, token);
            if ((Boolean) result.get("success")) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("能力评估失败: " + e.getMessage());
        }
    }

    @GetMapping("/evaluation/history")
    public Result<?> getEvaluationHistory(@RequestHeader("Authorization") String token) {
        try {
            return Result.success(interviewService.getEvaluationHistory(token));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取评估历史失败: " + e.getMessage());
        }
    }

    @GetMapping("/evaluation/growth")
    public Result<?> getAbilityGrowthCurve(@RequestHeader("Authorization") String token) {
        try {
            return Result.success(interviewService.getAbilityGrowthCurve(token));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取能力曲线失败: " + e.getMessage());
        }
    }

    @GetMapping("/evaluation/{sessionId}")
    public Result<?> getEvaluationBySession(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = interviewService.getEvaluationBySession(sessionId, token);
            if ((Boolean) result.get("success")) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            return Result.error("获取评估详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/evaluation/debug")
    public Result<?> debugEvaluationData(@RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> debugInfo = interviewService.getDebugEvaluationData(token);
            return Result.success(debugInfo);
        } catch (Exception e) {
            return Result.error("诊断失败: " + e.getMessage());
        }
    }
}