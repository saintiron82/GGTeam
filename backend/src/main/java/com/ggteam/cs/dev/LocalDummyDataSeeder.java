package com.ggteam.cs.dev;

import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.OperatorRole;
import com.ggteam.cs.common.enums.Urgency;
import com.ggteam.cs.persistence.entity.AIAnalysis;
import com.ggteam.cs.persistence.entity.ApprovalHistory;
import com.ggteam.cs.persistence.entity.Diagnosis;
import com.ggteam.cs.persistence.entity.DraftResponse;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.entity.Operator;
import com.ggteam.cs.persistence.repository.AIAnalysisRepository;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.DiagnosisRepository;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.persistence.repository.OperatorRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 단독 검수용 더미 데이터 시더. <b>local 프로파일 전용 (백엔드 C 자체 검증용).</b>
 *
 * <p>백엔드 A/B/DB 없이 워크플로우·대시보드 API를 눈으로 확인할 수 있도록, 다양한 상태의
 * 문의/분석/초안/이력과 운영자 계정을 시딩한다. 운영 환경에는 절대 활성화되지 않는다.
 */
@Component
@Profile("local")
@Order(1)
public class LocalDummyDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDummyDataSeeder.class);

    private final OperatorRepository operatorRepository;
    private final InquiryRepository inquiryRepository;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final DraftResponseRepository draftResponseRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;

    public LocalDummyDataSeeder(OperatorRepository operatorRepository,
                                InquiryRepository inquiryRepository,
                                AIAnalysisRepository aiAnalysisRepository,
                                DiagnosisRepository diagnosisRepository,
                                DraftResponseRepository draftResponseRepository,
                                ApprovalHistoryRepository approvalHistoryRepository) {
        this.operatorRepository = operatorRepository;
        this.inquiryRepository = inquiryRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.draftResponseRepository = draftResponseRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Operator op1 = operatorRepository.save(
                new Operator(UUID.randomUUID(), "operator1", "{noop}local", OperatorRole.OPERATOR));
        Operator op2 = operatorRepository.save(
                new Operator(UUID.randomUUID(), "operator2", "{noop}local", OperatorRole.ADMIN));

        // 1) 접수만 된 문의 (분석 전) — 카드에 AI필드 null
        seedInquiry(InquiryStatus.RECEIVED, "u1001", "결제했는데 아이템이 안 들어왔어요. 확인 부탁드립니다.",
                null, null, null, null, false, null);

        // 2) AI 분석중
        seedInquiry(InquiryStatus.AI_ANALYZING, "u1002", "환불 요청합니다. 중복 결제된 것 같아요.",
                null, null, null, null, false, null);

        // 3) 배정 대기 (긴급) — Pull 배정 테스트 대상
        seedInquiry(InquiryStatus.PENDING_ASSIGNMENT, "u1003", "결제 오류로 게임을 못 하고 있습니다. 급해요!",
                Urgency.HIGH, "결제실패", "결제 승인 직후 오류로 아이템 미지급 추정", "결제 로그 확인 후 수동 지급 안내",
                false, null);

        // 4) 배정 대기 (보통) — 우선순위(FIFO) 확인용
        seedInquiry(InquiryStatus.PENDING_ASSIGNMENT, "u1004", "결제 영수증을 받지 못했습니다.",
                Urgency.NORMAL, "영수증", "영수증 메일 발송 누락 추정", "영수증 재발송 안내", false, null);

        // 5) 운영자 검토중 (op1 배정, 초안 존재) — 승인/수정/반려/재분석 테스트 대상
        seedInquiry(InquiryStatus.OPERATOR_REVIEWING, "u1005", "결제가 두 번 청구되었습니다. 환불해주세요.",
                Urgency.HIGH, "중복결제", "동일 주문 2회 청구 확인됨", "1건 환불 처리 안내",
                true, op1.getId());

        // 6) 승인 완료 (발송 대기)
        seedInquiry(InquiryStatus.APPROVED, "u1006", "포인트가 차감되었는데 충전이 안 됐어요.",
                Urgency.NORMAL, "포인트", "충전 지연 추정", "충전 완료 확인 안내", true, op1.getId());

        // 7) 발송 완료 (종료)
        seedInquiry(InquiryStatus.SENT, "u1007", "문의 감사합니다. 해결됐어요.",
                Urgency.LOW, "일반", "정상 처리", "감사 응대", true, op2.getId());

        log.info("========== [LOCAL] 더미 데이터 시딩 완료 ==========");
        log.info("운영자: operator1={} (OPERATOR), operator2={} (ADMIN)", op1.getId(), op2.getId());
        log.info("API 호출 시 헤더에 운영자 식별자를 넣으세요:  X-Operator-Id: {}", op1.getId());
        log.info("예) 칸반:      GET  http://localhost:8080/api/v1/dashboard/board");
        log.info("예) 알림:      GET  http://localhost:8080/api/v1/dashboard/notifications");
        log.info("예) 목록검색:  GET  http://localhost:8080/api/v1/dashboard/inquiries?status=PENDING_ASSIGNMENT");
        log.info("예) Pull배정:  POST http://localhost:8080/api/v1/operator/inquiries/pull   (헤더 X-Operator-Id 필요)");
        log.info("==================================================");
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void seedInquiry(InquiryStatus status, String userId, String content,
                             Urgency urgency, String subCategory, String cause, String direction,
                             boolean withDraft, UUID assignedOperatorId) {
        UUID inquiryId = UUID.randomUUID();
        Inquiry inquiry = new Inquiry(inquiryId,
                Map.of("userId", userId, "nickname", userId + "님", "channel", "WEB"),
                InquiryType.PAYMENT, content);
        inquiry.setStatus(status);
        if (assignedOperatorId != null) {
            inquiry.setAssignedOperatorId(assignedOperatorId);
        }
        inquiryRepository.save(inquiry);

        // 분석/진단 (배정대기 이후 단계는 분석 완료 상태)
        if (urgency != null) {
            aiAnalysisRepository.save(AIAnalysis.success(inquiryId, InquiryType.PAYMENT, subCategory,
                    urgency, content.length() > 20 ? content.substring(0, 20) + "..." : content,
                    List.of("결제", subCategory), Map.of("paymentStatus", "FAILED", "amount", 9900)));
            diagnosisRepository.save(Diagnosis.of(inquiryId, cause, direction, BigDecimal.valueOf(0.82)));
        }

        // 초안 (검토중/승인/발송 단계)
        if (withDraft) {
            draftResponseRepository.save(new DraftResponse(UUID.randomUUID(), inquiryId,
                    "안녕하세요. 문의 주신 건 확인하여 처리해 드리겠습니다. (더미 초안)", 0));
        }

        // 배정 이력
        if (assignedOperatorId != null) {
            approvalHistoryRepository.save(
                    ApprovalHistory.record(inquiryId, ApprovalAction.ASSIGN, assignedOperatorId, null));
        }
    }
}
