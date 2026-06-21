package com.ggteam.cs.sim;

import com.ggteam.cs.common.enums.DemoEnums.AccountStatus;
import com.ggteam.cs.common.enums.DemoEnums.DeliveryStatus;
import com.ggteam.cs.common.enums.DemoEnums.PaymentStatus;
import com.ggteam.cs.persistence.entity.Account;
import com.ggteam.cs.persistence.entity.ItemDelivery;
import com.ggteam.cs.persistence.entity.Payment;
import com.ggteam.cs.persistence.repository.AccountRepository;
import com.ggteam.cs.persistence.repository.ItemDeliveryRepository;
import com.ggteam.cs.persistence.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * sim 프로파일 데이터 시더. 시나리오 배정에 따라 계정/결제/아이템지급을 정합되게 시드한다.
 * 총 지급건수는 itemTarget까지 정상지급(DELIVERED) 이력으로 패딩한다.
 */
@Component
@Profile("sim")
@Order(1)
public class SimDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SimDataSeeder.class);

    private final ScenarioAssigner assigner;
    private final SimProperties props;
    private final AccountRepository accountRepo;
    private final PaymentRepository paymentRepo;
    private final ItemDeliveryRepository itemRepo;

    public SimDataSeeder(ScenarioAssigner assigner, SimProperties props,
                         AccountRepository accountRepo, PaymentRepository paymentRepo,
                         ItemDeliveryRepository itemRepo) {
        this.assigner = assigner;
        this.props = props;
        this.accountRepo = accountRepo;
        this.paymentRepo = paymentRepo;
        this.itemRepo = itemRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        SeedSummary s = seed(assigner.assign(props.getCount()), props.getItemTarget());
        log.info("========== [SIM] 시드 완료 ==========");
        log.info("[SIM] accounts={} payments={} items={}", s.accounts(), s.payments(), s.items());
        log.info("[SIM] 제어판: http://localhost:5173/dev/sim  (드립 시작은 제어판에서)");
        log.info("=====================================");
    }

    /**
     * 시드 핵심 로직 (테스트 대상).
     *
     * <p>총 지급건수를 {@code itemTarget}까지 정상지급(DELIVERED) 이력으로 채운다.
     * {@code itemTarget}가 시나리오로 이미 시드된 지급 건수보다 작으면 패딩하지 않으며,
     * 그 경우 {@link SeedSummary#items()}는 시드된 실제 건수(&gt;{@code itemTarget})가 된다
     * (절단하지 않음).
     *
     * @param assignments 시나리오 배정 목록 ({@link ScenarioAssigner#assign(int)}의 반환값)
     * @param itemTarget  목표 지급건수. 이미 시드된 건수보다 크면 DELIVERED 패딩으로 채운다.
     * @return 계정·결제·지급건수 요약 ({@link SeedSummary})
     */
    public SeedSummary seed(List<SimAssignment> assignments, int itemTarget) {
        int[] c = {0, 0};
        for (SimAssignment a : assignments) {
            accountRepo.save(account(a));
            seedScenario(a, c);
        }
        // 총 지급건수를 itemTarget까지 정상지급 이력으로 패딩 (참조 결제 없음)
        int i = 0;
        while (c[1] < itemTarget && !assignments.isEmpty()) {
            SimAssignment a = assignments.get(i % assignments.size());
            saveDelivery(a.userId(), null, DeliveryStatus.DELIVERED, "item-hist-" + c[1], c);
            i++;
        }
        return new SeedSummary(assignments.size(), c[0], c[1]);
    }

    private void seedScenario(SimAssignment a, int[] c) {
        String u = a.userId();
        switch (a.scenario()) {
            case PAID_NOT_DELIVERED -> {
                UUID pid = savePayment(u, "9900", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, pid, DeliveryStatus.NOT_DELIVERED, "item-sword-100", c);
            }
            case DUPLICATE_CHARGE -> {
                UUID p1 = savePayment(u, "9900", PaymentStatus.SUCCESS, null, c);
                savePayment(u, "9900", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, p1, DeliveryStatus.DELIVERED, "item-gold-500", c);
            }
            case PAYMENT_FAILED ->
                    savePayment(u, "4900", PaymentStatus.FAILED, "CARD_LIMIT_EXCEEDED: 카드 한도 초과", c);
            case PARTIAL_DELIVERY -> {
                UUID pid = savePayment(u, "19900", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, pid, DeliveryStatus.PARTIAL, "item-bundle-10", c);
            }
            case REFUND_PENDING -> savePayment(u, "9900", PaymentStatus.REFUNDED, null, c);
            case POINT_NOT_CHARGED -> {
                UUID pid = savePayment(u, "5000", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, pid, DeliveryStatus.NOT_DELIVERED, "item-point-5000", c);
            }
            case WRONG_ITEM -> {
                UUID pid = savePayment(u, "9900", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, pid, DeliveryStatus.DELIVERED, "item-wrong-999", c);
            }
            case ACCOUNT_ISSUE, ETC -> {
                // 결제 이력 없음 (계정 상태/일반 문의로 표현)
            }
        }
    }

    private Account account(SimAssignment a) {
        Account acc = new Account();
        acc.setUserId(a.userId());
        acc.setStatus(a.scenario() == SimScenario.ACCOUNT_ISSUE
                ? switch (a.index() % 3) {
                    case 0 -> AccountStatus.SUSPENDED;
                    case 1 -> AccountStatus.DORMANT;
                    default -> AccountStatus.BANNED;
                }
                : AccountStatus.ACTIVE);
        acc.setLastLogin(ZonedDateTime.now());
        return acc;
    }

    private UUID savePayment(String userId, String amount, PaymentStatus status, String errorLog, int[] c) {
        Payment p = new Payment();
        p.setUserId(userId);
        p.setAmount(new BigDecimal(amount));
        p.setStatus(status);
        p.setErrorLog(errorLog);
        Payment saved = paymentRepo.save(p);
        c[0]++;
        return saved.getId();
    }

    private void saveDelivery(String userId, UUID paymentId, DeliveryStatus status, String itemId, int[] c) {
        ItemDelivery d = new ItemDelivery();
        d.setPaymentId(paymentId);
        d.setUserId(userId);
        d.setItemId(itemId);
        d.setStatus(status);
        itemRepo.save(d);
        c[1]++;
    }

    public record SeedSummary(int accounts, int payments, int items) {}
}
