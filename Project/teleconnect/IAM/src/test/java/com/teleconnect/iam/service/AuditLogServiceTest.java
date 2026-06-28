package com.teleconnect.iam.service;

import com.teleconnect.iam.dto.request.AuditLogFilterDTO;
import com.teleconnect.iam.dto.response.AuditLogResponseDTO;
import com.teleconnect.iam.entity.AuditLog;
import com.teleconnect.iam.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditLogService}.
 *
 * <p>Covers (1) that {@code log()} persists an {@link AuditLog} carrying exactly
 * the supplied fields, and (2) that the {@code from/to/action/module} filters in
 * {@code getAllLogs} / {@code getLogsByUser} narrow results correctly.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository repo;
    @InjectMocks private AuditLogService auditLogService;
    @Captor private ArgumentCaptor<AuditLog> logCaptor;

    private AuditLog entry(Long id, Long userId, String action, String module, LocalDateTime ts) {
        AuditLog l = new AuditLog();
        l.setAuditId(id);
        l.setUserId(userId);
        l.setAction(action);
        l.setModule(module);
        l.setIpAddress("127.0.0.1");
        l.setTimestamp(ts);
        return l;
    }

    // A small fixed dataset reused by the filter tests.
    private List<AuditLog> sampleLogs() {
        return List.of(
            entry(1L, 1L, "USER_LOGIN",   "IAM",     LocalDateTime.of(2024, 1, 10, 0, 0)),
            entry(2L, 1L, "USER_UPDATED", "IAM",     LocalDateTime.of(2024, 6, 15, 0, 0)),
            entry(3L, 2L, "USER_LOGIN",   "BILLING", LocalDateTime.of(2024, 12, 20, 0, 0))
        );
    }

    // ---- log() -------------------------------------------------------------
    @Nested
    @DisplayName("log()")
    class Log {
        @Test
        @DisplayName("persists an AuditLog carrying the supplied fields")
        void persistsEntry() {
            auditLogService.log(7L, "USER_LOGIN", "IAM", "10.0.0.1");

            verify(repo).save(logCaptor.capture());
            AuditLog saved = logCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo(7L);
            assertThat(saved.getAction()).isEqualTo("USER_LOGIN");
            assertThat(saved.getModule()).isEqualTo("IAM");
            assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
        }
    }

    // ---- getAllLogs() ------------------------------------------------------
    @Nested
    @DisplayName("getAllLogs()")
    class GetAllLogs {
        @Test
        @DisplayName("empty filter -> returns every log, mapped to DTO")
        void noFilter() {
            when(repo.findAll()).thenReturn(sampleLogs());

            List<AuditLogResponseDTO> result = auditLogService.getAllLogs(new AuditLogFilterDTO());

            assertThat(result).hasSize(3);
            AuditLogResponseDTO first = result.get(0);
            assertThat(first.getAuditId()).isEqualTo(1L);
            assertThat(first.getAction()).isEqualTo("USER_LOGIN");
            assertThat(first.getModule()).isEqualTo("IAM");
        }

        @Test
        @DisplayName("action filter is case-insensitive")
        void filterByAction() {
            when(repo.findAll()).thenReturn(sampleLogs());
            AuditLogFilterDTO f = new AuditLogFilterDTO();
            f.setAction("user_login");

            List<AuditLogResponseDTO> result = auditLogService.getAllLogs(f);

            assertThat(result).extracting(AuditLogResponseDTO::getAuditId)
                    .containsExactly(1L, 3L);
        }

        @Test
        @DisplayName("module filter is case-insensitive")
        void filterByModule() {
            when(repo.findAll()).thenReturn(sampleLogs());
            AuditLogFilterDTO f = new AuditLogFilterDTO();
            f.setModule("iam");

            List<AuditLogResponseDTO> result = auditLogService.getAllLogs(f);

            assertThat(result).extracting(AuditLogResponseDTO::getAuditId)
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("from/to window keeps only logs inside the range (bounds inclusive)")
        void filterByDateRange() {
            when(repo.findAll()).thenReturn(sampleLogs());
            AuditLogFilterDTO f = new AuditLogFilterDTO();
            f.setFrom(LocalDateTime.of(2024, 5, 1, 0, 0));
            f.setTo(LocalDateTime.of(2024, 7, 1, 0, 0));

            List<AuditLogResponseDTO> result = auditLogService.getAllLogs(f);

            assertThat(result).extracting(AuditLogResponseDTO::getAuditId)
                    .containsExactly(2L);
        }
    }

    // ---- getLogsByUser() ---------------------------------------------------
    @Nested
    @DisplayName("getLogsByUser()")
    class GetLogsByUser {
        @Test
        @DisplayName("returns only the given user's logs")
        void filtersByUser() {
            when(repo.findAll()).thenReturn(sampleLogs());

            List<AuditLogResponseDTO> result =
                    auditLogService.getLogsByUser(1L, new AuditLogFilterDTO());

            assertThat(result).extracting(AuditLogResponseDTO::getUserId)
                    .containsOnly(1L);
            assertThat(result).extracting(AuditLogResponseDTO::getAuditId)
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("combines the user filter with the field filter")
        void filtersByUserAndAction() {
            when(repo.findAll()).thenReturn(sampleLogs());
            AuditLogFilterDTO f = new AuditLogFilterDTO();
            f.setAction("USER_LOGIN");

            List<AuditLogResponseDTO> result = auditLogService.getLogsByUser(1L, f);

            assertThat(result).extracting(AuditLogResponseDTO::getAuditId)
                    .containsExactly(1L);
        }
    }
}
