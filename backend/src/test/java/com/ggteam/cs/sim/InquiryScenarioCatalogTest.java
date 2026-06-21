package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;

import com.ggteam.cs.common.enums.InquiryType;
import java.util.List;
import org.junit.jupiter.api.Test;

class InquiryScenarioCatalogTest {

    private final InquiryScenarioCatalog catalog = new InquiryScenarioCatalog(new ScenarioAssigner());

    @Test
    void count건의_문의를_생성한다() {
        List<PlannedInquiry> plan = catalog.build(100);

        assertThat(plan).hasSize(100);
        assertThat(plan.stream().map(PlannedInquiry::userId).distinct().count()).isEqualTo(100);
    }

    @Test
    void 모든_본문은_10자_이상이다() {
        for (PlannedInquiry p : catalog.build(100)) {
            assertThat(p.content().length()).isGreaterThanOrEqualTo(10);
        }
    }

    @Test
    void 시나리오에_맞는_유형으로_매핑된다() {
        List<PlannedInquiry> plan = catalog.build(100);

        assertThat(plan).filteredOn(p -> p.scenario() == SimScenario.ACCOUNT_ISSUE)
                .allMatch(p -> p.type() == InquiryType.ACCOUNT);
        assertThat(plan).filteredOn(p -> p.scenario() == SimScenario.ETC)
                .allMatch(p -> p.type() == InquiryType.ETC);
        assertThat(plan).filteredOn(p -> p.scenario() == SimScenario.PAID_NOT_DELIVERED)
                .allMatch(p -> p.type() == InquiryType.PAYMENT);
    }
}
