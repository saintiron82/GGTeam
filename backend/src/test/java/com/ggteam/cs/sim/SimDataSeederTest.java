package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ggteam.cs.persistence.repository.AccountRepository;
import com.ggteam.cs.persistence.repository.ItemDeliveryRepository;
import com.ggteam.cs.persistence.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimDataSeederTest {

    @Mock AccountRepository accountRepo;
    @Mock PaymentRepository paymentRepo;
    @Mock ItemDeliveryRepository itemRepo;

    @Test
    void 계정100_결제100_아이템500을_시드한다() {
        when(accountRepo.save(any())).then(returnsFirstArg());
        when(paymentRepo.save(any())).then(returnsFirstArg());
        when(itemRepo.save(any())).then(returnsFirstArg());

        SimDataSeeder seeder = new SimDataSeeder(
                new ScenarioAssigner(), new SimProperties(), accountRepo, paymentRepo, itemRepo);

        SimDataSeeder.SeedSummary s = seeder.seed(new ScenarioAssigner().assign(100), 500);

        assertThat(s.accounts()).isEqualTo(100);
        assertThat(s.payments()).isEqualTo(100);
        assertThat(s.items()).isEqualTo(500);
    }
}
