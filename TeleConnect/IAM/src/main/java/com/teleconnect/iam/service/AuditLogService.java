package com.teleconnect.iam.service;

import com.teleconnect.iam.dto.request.AuditLogFilterDTO;
import com.teleconnect.iam.dto.response.AuditLogResponseDTO;
import com.teleconnect.iam.entity.AuditLog;
import com.teleconnect.iam.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository repo;

    public AuditLogService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(Long userId, String action, String module, String ip) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setModule(module);
        log.setIpAddress(ip);
        repo.save(log);
    }

    // -- GET ALL LOGS (with filter) ---------------------------
    public List<AuditLogResponseDTO> getAllLogs(AuditLogFilterDTO filter) {
        return applyFilter(repo.findAll(), filter);
    }

    // -- GET LOGS BY USER (with filter) -----------------------
    public List<AuditLogResponseDTO> getLogsByUser(Long userId, AuditLogFilterDTO filter) {
        List<AuditLog> userLogs = repo.findAll().stream()
            .filter(l -> userId.equals(l.getUserId()))
            .collect(Collectors.toList());
        return applyFilter(userLogs, filter);
    }

    private List<AuditLogResponseDTO> applyFilter(List<AuditLog> logs, AuditLogFilterDTO f) {
        return logs.stream()
            .filter(l -> f.getFrom() == null || !l.getTimestamp().isBefore(f.getFrom()))
            .filter(l -> f.getTo() == null || !l.getTimestamp().isAfter(f.getTo()))
            .filter(l -> f.getAction() == null || l.getAction().equalsIgnoreCase(f.getAction()))
            .filter(l -> f.getModule() == null || l.getModule().equalsIgnoreCase(f.getModule()))
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    private AuditLogResponseDTO toDTO(AuditLog l) {
        AuditLogResponseDTO dto = new AuditLogResponseDTO();
        dto.setAuditId(l.getAuditId());
        dto.setUserId(l.getUserId());
        dto.setAction(l.getAction());
        dto.setModule(l.getModule());
        dto.setIpAddress(l.getIpAddress());
        dto.setTimestamp(l.getTimestamp());
        return dto;
    }
}
