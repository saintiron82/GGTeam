package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ggteam.cs.sim.SimulationController.StartRequest;
import org.junit.jupiter.api.Test;

class SimulationControllerTest {

    private final SimulationService service = mock(SimulationService.class);
    private final SimulationController controller = new SimulationController(service);

    private SimulationStatus sample() {
        return new SimulationStatus(true, 100, 0, 0, 123L, 0, 0, 0.0, "agentcli");
    }

    @Test
    void start는_파라미터를_서비스에_위임한다() {
        when(service.start(10, 5, true)).thenReturn(sample());

        var resp = controller.start(new StartRequest(10, 5, true));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).start(10, 5, true);
    }

    @Test
    void start_바디_null이면_기본값_위임() {
        when(service.start(null, null, null)).thenReturn(sample());

        controller.start(null);

        verify(service).start(null, null, null);
    }

    @Test
    void status_위임() {
        when(service.status()).thenReturn(sample());

        var resp = controller.status();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).status();
    }
}
