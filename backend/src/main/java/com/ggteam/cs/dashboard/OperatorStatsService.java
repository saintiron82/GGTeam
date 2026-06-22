package com.ggteam.cs.dashboard;

import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.dashboard.dto.OperatorStat;
import com.ggteam.cs.persistence.entity.ApprovalHistory;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.entity.Operator;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.persistence.repository.OperatorRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자별 처리 현황 집계 (US-27 확장). 담당: 백엔드 C.
 * 현재 담당 건수(assignedOperatorId) + 승인 완료(APPROVE 액션) + 총 처리 액션 수를 운영자별로 집계한다.
 */
@Service
public class OperatorStatsService {

    private final OperatorRepository operatorRepo;
    private final InquiryRepository inquiryRepo;
    private final ApprovalHistoryRepository historyRepo;

    public OperatorStatsService(OperatorRepository operatorRepo, InquiryRepository inquiryRepo,
                                ApprovalHistoryRepository historyRepo) {
        this.operatorRepo = operatorRepo;
        this.inquiryRepo = inquiryRepo;
        this.historyRepo = historyRepo;
    }

    @Transactional(readOnly = true)
    public List<OperatorStat> compute() {
        List<Operator> operators = operatorRepo.findAll();

        Map<UUID, Long> assignedByOp = inquiryRepo.findAll().stream()
                .filter(i -> i.getAssignedOperatorId() != null)
                .collect(Collectors.groupingBy(Inquiry::getAssignedOperatorId, Collectors.counting()));

        Map<UUID, Set<UUID>> approvedByOp = new HashMap<>();
        Map<UUID, Long> actionsByOp = new HashMap<>();
        for (ApprovalHistory h : historyRepo.findAll()) {
            UUID opId = h.getOperatorId();
            if (opId == null) {
                continue;
            }
            actionsByOp.merge(opId, 1L, Long::sum);
            if (h.getAction() == ApprovalAction.APPROVE) {
                approvedByOp.computeIfAbsent(opId, k -> new HashSet<>()).add(h.getInquiryId());
            }
        }

        return operators.stream()
                .map(op -> new OperatorStat(
                        op.getId(),
                        op.getUsername(),
                        op.getRole().name(),
                        assignedByOp.getOrDefault(op.getId(), 0L),
                        (long) approvedByOp.getOrDefault(op.getId(), Set.of()).size(),
                        actionsByOp.getOrDefault(op.getId(), 0L)))
                .sorted(Comparator.comparingLong((OperatorStat s) -> s.approved() + s.assigned()).reversed()
                        .thenComparing(OperatorStat::username))
                .toList();
    }
}
