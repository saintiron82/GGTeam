package com.ggteam.cs.sim;

import com.ggteam.cs.common.enums.InquiryType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 시나리오 배정을 받아 정합되는 CS 문의 본문을 생성한다.
 * 본문은 시나리오별 템플릿을 index로 변주(결정적)하여 단조로움을 피한다.
 */
@Component
public class InquiryScenarioCatalog {

    private final ScenarioAssigner assigner;
    private final Map<SimScenario, String[]> templates = templates();

    public InquiryScenarioCatalog(ScenarioAssigner assigner) {
        this.assigner = assigner;
    }

    public List<PlannedInquiry> build(int count) {
        List<PlannedInquiry> out = new ArrayList<>(count);
        for (SimAssignment a : assigner.assign(count)) {
            String[] t = templates.get(a.scenario());
            out.add(new PlannedInquiry(a.userId(), typeOf(a.scenario()),
                    t[a.index() % t.length], a.scenario()));
        }
        return out;
    }

    private InquiryType typeOf(SimScenario s) {
        return switch (s) {
            case ACCOUNT_ISSUE -> InquiryType.ACCOUNT;
            case ETC -> InquiryType.ETC;
            default -> InquiryType.PAYMENT;
        };
    }

    private Map<SimScenario, String[]> templates() {
        Map<SimScenario, String[]> m = new EnumMap<>(SimScenario.class);
        m.put(SimScenario.PAID_NOT_DELIVERED, new String[]{
                "결제는 완료됐는데 아이템이 들어오지 않았어요. 확인 부탁드립니다.",
                "방금 결제했는데 아이템이 아직 지급되지 않았습니다. 빠른 처리 부탁해요.",
                "구매 완료 메시지는 떴는데 인벤토리에 아이템이 없어요.",
                "결제 성공했는데 한 시간째 아이템이 안 와요. 어떻게 된 건가요?"});
        m.put(SimScenario.DUPLICATE_CHARGE, new String[]{
                "같은 상품이 두 번 결제됐어요. 한 건 환불해주세요.",
                "결제가 중복으로 두 번 청구되었습니다. 확인 후 환불 바랍니다.",
                "동일 주문이 2회 결제된 것 같아요. 중복분 취소 부탁드립니다."});
        m.put(SimScenario.PAYMENT_FAILED, new String[]{
                "결제 실패라고 떴는데 카드에서는 돈이 빠져나갔어요.",
                "한도 초과로 실패했다는데 결제 문자가 왔습니다. 확인해주세요.",
                "결제가 안 됐다고 나오는데 출금은 됐어요. 어떻게 처리되나요?"});
        m.put(SimScenario.PARTIAL_DELIVERY, new String[]{
                "패키지를 샀는데 일부 아이템만 지급됐어요.",
                "묶음 상품 중 절반만 들어왔습니다. 나머지는 언제 오나요?",
                "구성품 일부가 지급되지 않았어요. 누락분 확인 부탁드립니다."});
        m.put(SimScenario.REFUND_PENDING, new String[]{
                "환불 신청한 지 며칠 됐는데 아직 처리가 안 됐어요.",
                "환불 요청했는데 진행 상황을 알 수 없습니다. 확인해주세요.",
                "환불이 접수만 되고 입금이 안 됐어요. 언제 처리되나요?"});
        m.put(SimScenario.POINT_NOT_CHARGED, new String[]{
                "포인트 충전 결제는 됐는데 포인트가 안 들어왔어요.",
                "캐시 충전했는데 잔액에 반영이 안 됩니다. 확인 부탁해요.",
                "포인트 결제 후에도 잔액이 그대로예요. 충전이 누락된 것 같아요."});
        m.put(SimScenario.WRONG_ITEM, new String[]{
                "주문한 아이템과 다른 아이템이 지급됐어요.",
                "구매한 것과 전혀 다른 상품이 들어왔습니다. 교환해주세요.",
                "엉뚱한 아이템이 지급됐어요. 올바른 아이템으로 변경 부탁드립니다."});
        m.put(SimScenario.ACCOUNT_ISSUE, new String[]{
                "갑자기 로그인이 안 됩니다. 계정에 문제가 있나요?",
                "계정이 정지된 것 같아요. 사유와 해제 방법을 알려주세요.",
                "휴면 계정이라고 떠서 접속이 안 됩니다. 복구 부탁드립니다."});
        m.put(SimScenario.ETC, new String[]{
                "게임 이용 관련해서 문의드릴 게 있습니다. 답변 부탁드려요.",
                "서비스 이용 중 궁금한 점이 있어 문의 남깁니다.",
                "일반 문의입니다. 확인 후 안내 부탁드립니다."});
        return m;
    }
}
