package com.ggteam.cs.sim;

import static org.mockito.Mockito.verify;

import com.ggteam.cs.persistence.repository.AIAnalysisRepository;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.DiagnosisRepository;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimPurgeServiceTest {

    @Mock InquiryRepository inquiryRepo;
    @Mock AIAnalysisRepository analysisRepo;
    @Mock DiagnosisRepository diagnosisRepo;
    @Mock DraftResponseRepository draftRepo;
    @Mock ApprovalHistoryRepository historyRepo;

    @Test
    void purge_모든_문의_관련_데이터를_삭제한다() {
        SimPurgeService service =
                new SimPurgeService(inquiryRepo, analysisRepo, diagnosisRepo, draftRepo, historyRepo);

        service.purge();

        verify(inquiryRepo).deleteAllInBatch();
        verify(analysisRepo).deleteAllInBatch();
        verify(diagnosisRepo).deleteAllInBatch();
        verify(draftRepo).deleteAllInBatch();
        verify(historyRepo).deleteAllInBatch();
    }
}
