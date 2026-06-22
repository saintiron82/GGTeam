package com.ggteam.cs.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.OperatorRole;
import com.ggteam.cs.dashboard.dto.OperatorStat;
import com.ggteam.cs.persistence.entity.ApprovalHistory;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.entity.Operator;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.persistence.repository.OperatorRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OperatorStatsServiceTest {

    @Mock OperatorRepository operatorRepo;
    @Mock InquiryRepository inquiryRepo;
    @Mock ApprovalHistoryRepository historyRepo;

    private Operator operator(UUID id, String username, OperatorRole role) {
        Operator op = Operator.of(username, "{noop}x", role);
        ReflectionTestUtils.setField(op, "id", id);
        return op;
    }

    private Inquiry inquiryAssignedTo(UUID operatorId) {
        Inquiry i = Inquiry.of(Map.of("userId", "u"), InquiryType.PAYMENT, "결제 문의입니다 도와주세요.");
        i.setAssignedOperatorId(operatorId);
        return i;
    }

    private ApprovalHistory history(UUID operatorId, ApprovalAction action, UUID inquiryId) {
        return ApprovalHistory.record(inquiryId, action, operatorId, null);
    }

    @Test
    void 운영자별_담당_승인_액션을_집계한다() {
        UUID adminId = UUID.randomUUID();
        UUID opId = UUID.randomUUID();
        UUID inq1 = UUID.randomUUID();
        UUID inq2 = UUID.randomUUID();

        when(operatorRepo.findAll()).thenReturn(List.of(
                operator(adminId, "admin", OperatorRole.ADMIN),
                operator(opId, "operator", OperatorRole.OPERATOR)));
        when(inquiryRepo.findAll()).thenReturn(List.of(
                inquiryAssignedTo(adminId), inquiryAssignedTo(adminId), inquiryAssignedTo(null)));
        when(historyRepo.findAll()).thenReturn(List.of(
                history(adminId, ApprovalAction.APPROVE, inq1),
                history(adminId, ApprovalAction.ASSIGN, inq1),
                history(opId, ApprovalAction.APPROVE, inq2)));

        List<OperatorStat> stats =
                new OperatorStatsService(operatorRepo, inquiryRepo, historyRepo).compute();

        assertThat(stats).hasSize(2);
        OperatorStat admin = stats.stream().filter(s -> s.operatorId().equals(adminId)).findFirst().orElseThrow();
        assertThat(admin.assigned()).isEqualTo(2);
        assertThat(admin.approved()).isEqualTo(1);
        assertThat(admin.actions()).isEqualTo(2);

        OperatorStat op = stats.stream().filter(s -> s.operatorId().equals(opId)).findFirst().orElseThrow();
        assertThat(op.assigned()).isEqualTo(0);
        assertThat(op.approved()).isEqualTo(1);
        assertThat(op.actions()).isEqualTo(1);
    }
}
