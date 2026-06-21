package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ggteam.cs.persistence.repository.AccountRepository;
import com.ggteam.cs.persistence.repository.ItemDeliveryRepository;
import com.ggteam.cs.persistence.repository.OperatorRepository;
import com.ggteam.cs.persistence.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SimDataSeederTest {

    @Mock AccountRepository accountRepo;
    @Mock PaymentRepository paymentRepo;
    @Mock ItemDeliveryRepository itemRepo;
    @Mock OperatorRepository operatorRepo;

    private SimDataSeeder newSeeder() {
        return new SimDataSeeder(new ScenarioAssigner(), new SimProperties(),
                accountRepo, paymentRepo, itemRepo, operatorRepo, new BCryptPasswordEncoder());
    }

    @Test
    void itemTarget가_시나리오_시드보다_작으면_절단없이_시드건수를_반환한다() {
        when(accountRepo.save(any())).then(returnsFirstArg());
        when(paymentRepo.save(any())).then(returnsFirstArg());
        when(itemRepo.save(any())).then(returnsFirstArg());

        SimDataSeeder seeder = newSeeder();

        // count=100 시나리오는 63건의 지급을 생성 → itemTarget=10은 패딩을 트리거하지 않음
        SimDataSeeder.SeedSummary s = seeder.seed(new ScenarioAssigner().assign(100), 10);

        assertThat(s.items()).isEqualTo(63);   // 절단하지 않음
        assertThat(s.items()).isGreaterThan(10);
    }

    @Test
    void 계정100_결제100_아이템500을_시드한다() {
        when(accountRepo.save(any())).then(returnsFirstArg());
        when(paymentRepo.save(any())).then(returnsFirstArg());
        when(itemRepo.save(any())).then(returnsFirstArg());

        SimDataSeeder seeder = newSeeder();

        SimDataSeeder.SeedSummary s = seeder.seed(new ScenarioAssigner().assign(100), 500);

        assertThat(s.accounts()).isEqualTo(100);
        assertThat(s.payments()).isEqualTo(100);
        assertThat(s.items()).isEqualTo(500);
    }
}
