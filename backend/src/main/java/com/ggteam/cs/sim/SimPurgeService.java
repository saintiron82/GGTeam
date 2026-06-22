package com.ggteam.cs.sim;

import com.ggteam.cs.persistence.repository.AIAnalysisRepository;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.DiagnosisRepository;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시뮬레이션 데이터 완전 초기화(데모용). 문의/분석/진단/초안/이력을 모두 삭제한다.
 * 계정/결제/아이템 시드와 운영자는 유지한다(드립이 참조하는 정적 데이터).
 */
@Service
@Profile("sim")
public class SimPurgeService {

    private static final Logger log = LoggerFactory.getLogger(SimPurgeService.class);

    private final InquiryRepository inquiryRepo;
    private final AIAnalysisRepository analysisRepo;
    private final DiagnosisRepository diagnosisRepo;
    private final DraftResponseRepository draftRepo;
    private final ApprovalHistoryRepository historyRepo;

    public SimPurgeService(InquiryRepository inquiryRepo, AIAnalysisRepository analysisRepo,
                           DiagnosisRepository diagnosisRepo, DraftResponseRepository draftRepo,
                           ApprovalHistoryRepository historyRepo) {
        this.inquiryRepo = inquiryRepo;
        this.analysisRepo = analysisRepo;
        this.diagnosisRepo = diagnosisRepo;
        this.draftRepo = draftRepo;
        this.historyRepo = historyRepo;
    }

    /** 문의 관련 데이터 전체 삭제 (자식 → 부모 순). 계정/결제/아이템/운영자는 보존. */
    @Transactional
    public void purge() {
        historyRepo.deleteAllInBatch();
        draftRepo.deleteAllInBatch();
        diagnosisRepo.deleteAllInBatch();
        analysisRepo.deleteAllInBatch();
        inquiryRepo.deleteAllInBatch();
        log.info("[SIM] 완전 초기화 — 문의/분석/진단/초안/이력 삭제됨 (계정/결제/아이템 시드 유지)");
    }
}
