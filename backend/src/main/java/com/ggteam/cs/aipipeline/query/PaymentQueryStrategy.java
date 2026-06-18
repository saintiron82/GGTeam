package com.ggteam.cs.aipipeline.query;

import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.persistence.entity.ItemDelivery;
import com.ggteam.cs.persistence.entity.Payment;
import com.ggteam.cs.persistence.repository.ItemDeliveryRepository;
import com.ggteam.cs.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 결제 유형 시스템 데이터 조회 전략 (US-09, MVP). 백엔드 B.
 * 결제 이력 + 아이템 지급 이력을 조회하여 진단 근거로 제공한다.
 */
@Component
public class PaymentQueryStrategy implements QueryStrategy {

    private static final int LIMIT = 5;

    private final PaymentRepository paymentRepository;
    private final ItemDeliveryRepository itemDeliveryRepository;

    public PaymentQueryStrategy(PaymentRepository paymentRepository,
                                ItemDeliveryRepository itemDeliveryRepository) {
        this.paymentRepository = paymentRepository;
        this.itemDeliveryRepository = itemDeliveryRepository;
    }

    @Override
    public InquiryType supportedType() {
        return InquiryType.PAYMENT;
    }

    @Override
    public SystemQueryResult query(String userId) {
        List<Map<String, Object>> payments = new ArrayList<>();
        for (Payment p : paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().limit(LIMIT).toList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("paymentId", p.getId());
            m.put("amount", p.getAmount());
            m.put("status", p.getStatus());
            m.put("errorLog", p.getErrorLog());
            m.put("createdAt", p.getCreatedAt());
            payments.add(m);
        }

        List<Map<String, Object>> deliveries = new ArrayList<>();
        for (ItemDelivery d : itemDeliveryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().limit(LIMIT).toList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", d.getItemId());
            m.put("status", d.getStatus());
            m.put("paymentId", d.getPaymentId());
            m.put("createdAt", d.getCreatedAt());
            deliveries.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("payments", payments);
        data.put("deliveries", deliveries);
        return new SystemQueryResult(InquiryType.PAYMENT, data);
    }
}
