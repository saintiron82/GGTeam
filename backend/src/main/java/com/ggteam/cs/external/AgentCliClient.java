package com.ggteam.cs.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 로컬 에이전트 CLI(`claude -p`) 기반 LlmClient (시뮬레이션/데모용).
 * Bedrock 자격증명 없이 실제 LLM 품질을 얻기 위해 외부 CLI 프로세스를 호출한다.
 *
 * <p>프롬프트는 stdin으로, 시스템 지침은 systemFlag 인자로 전달하고 stdout 전체를 응답으로 사용한다.
 * 활성화: app.ai.llm-client=agentcli
 */
@Component
@ConditionalOnProperty(name = "app.ai.llm-client", havingValue = "agentcli")
public class AgentCliClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AgentCliClient.class);

    private final List<String> baseCommand;
    private final String systemFlag;
    private final int timeoutSeconds;

    public AgentCliClient(
            @Value("${app.ai.agentcli.command:claude -p}") String command,
            @Value("${app.ai.agentcli.system-flag:--append-system-prompt}") String systemFlag,
            @Value("${app.ai.agentcli.timeout-seconds:120}") int timeoutSeconds) {
        this.baseCommand = List.of(command.trim().split("\\s+"));
        this.systemFlag = systemFlag;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        List<String> args = new ArrayList<>(baseCommand);
        if (request.system() != null && !request.system().isBlank()) {
            args.add(systemFlag);
            args.add(request.system());
        }

        Process process = null;
        try {
            process = new ProcessBuilder(args).start();

            // 프롬프트를 stdin으로 전달 (프로세스가 stdin을 이미 닫았을 수 있어 IOException 무시)
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write((request.prompt() == null ? "" : request.prompt())
                        .getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // 프로세스가 stdin을 받지 않는 경우
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new LlmTimeoutException("agentcli 호출 타임아웃(" + timeoutSeconds + "s)", null);
            }
            int exit = process.exitValue();
            if (exit != 0) {
                String err = readStream(process, false);
                throw new LlmApiException("agentcli 비정상 종료 exit=" + exit + " : " + err, null);
            }
            // 출력은 LLM 응답(≤수 KB)이므로 종료 후 일괄 읽어도 파이프 버퍼 한계 내
            return new LlmResponse(readStream(process, true));

        } catch (LlmTimeoutException | LlmApiException e) {
            throw e;
        } catch (InterruptedException e) {
            destroy(process);
            Thread.currentThread().interrupt();
            throw new LlmApiException("agentcli 대기 중 인터럽트", e);
        } catch (Exception e) {
            destroy(process);
            throw new LlmApiException("agentcli 실행 오류: " + e.getMessage(), e);
        }
    }

    private String readStream(Process process, boolean stdout) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stdout ? process.getInputStream() : process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            log.warn("agentcli 스트림 읽기 실패: {}", e.getMessage());
        }
        return sb.toString().strip();
    }

    private void destroy(Process process) {
        if (process != null) {
            process.destroyForcibly();
        }
    }
}
