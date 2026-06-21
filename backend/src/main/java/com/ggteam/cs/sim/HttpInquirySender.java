package com.ggteam.cs.sim;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 자기 자신의 공개 접수 API(POST /api/v1/inquiries)를 호출해 실제 파이프라인을 태운다. */
@Component
@Profile("sim")
public class HttpInquirySender implements InquirySender {

    private final RestClient restClient;

    public HttpInquirySender(@Value("${server.port:8080}") int port) {
        this.restClient = RestClient.create("http://localhost:" + port);
    }

    @Override
    public void send(PlannedInquiry inq) {
        Map<String, Object> body = Map.of(
                "customerInfo", Map.of("userId", inq.userId(), "nickname", inq.userId() + "님", "channel", "WEB"),
                "customerType", inq.type().name(),
                "content", inq.content());
        restClient.post().uri("/api/v1/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
