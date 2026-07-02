package com.dove.modelserving.infrastructure.scorer;

import com.dove.modelserving.application.exception.ModelScoringException;
import com.dove.modelserving.application.port.ModelScorer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 외부 Python 채점기 스크립트를 ProcessBuilder로 호출하는 채점기 어댑터(stdin/stdout JSON).
 */
@Slf4j
@Component
public class ProcessModelScorer implements ModelScorer {

    private final ObjectMapper objectMapper;
    private final String pythonExecutable;
    private final String scriptPath;
    private final long timeoutSeconds;

    public ProcessModelScorer(ObjectMapper objectMapper,
                              @Value("${model.scorer.python:python}") String pythonExecutable,
                              @Value("${model.scorer.script:}") String scriptPath,
                              @Value("${model.scorer.timeout-seconds:120}") long timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public List<ScoredRow> score(PredictInput input) {
        Process process = startProcess();
        writeInput(process, input);
        String stdout = readStream(process.getInputStream());
        awaitExit(process, stdout);
        return parseOutput(stdout);
    }

    /**
     * 채점기 프로세스를 시작한다. stderr를 별도 스트림으로 유지(에러 코드 판별용)하지 않고 합치지 않는다.
     */
    private Process startProcess() {
        try {
            return new ProcessBuilder(pythonExecutable, scriptPath).start();
        } catch (Exception e) {
            throw new ModelScoringException("SCORER_START_FAILED", "채점기 프로세스 시작 실패", e);
        }
    }

    private void writeInput(Process process, PredictInput input) {
        try (OutputStream os = process.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(input));
        } catch (Exception e) {
            process.destroyForcibly();
            throw new ModelScoringException("SCORER_WRITE_FAILED", "채점기 입력 전달 실패", e);
        }
    }

    private String readStream(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining());
        } catch (Exception e) {
            throw new ModelScoringException("SCORER_READ_FAILED", "채점기 출력 수신 실패", e);
        }
    }

    /**
     * 프로세스 종료를 대기하고, 0이 아닌 종료코드는 stderr와 함께 에러로 매핑한다.
     */
    private void awaitExit(Process process, String stdout) {
        String stderr = readStream(process.getErrorStream());
        int exitCode;
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ModelScoringException("SCORER_TIMEOUT", "채점기 응답 시간 초과");
            }
            exitCode = process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ModelScoringException("SCORER_INTERRUPTED", "채점기 대기 중 인터럽트", e);
        }
        if (exitCode != 0) {
            throw new ModelScoringException(errorCodeOf(exitCode), stderr.isBlank() ? stdout : stderr);
        }
    }

    /**
     * 채점기 종료코드를 약속된 에러 코드로 매핑한다.
     */
    private static String errorCodeOf(int exitCode) {
        return switch (exitCode) {
            case 1 -> "MODEL_LOAD_FAILED";
            case 2 -> "FEATURE_MISMATCH";
            case 3 -> "PREDICT_ERROR";
            default -> "SCORER_UNKNOWN_ERROR";
        };
    }

    private List<ScoredRow> parseOutput(String stdout) {
        PredictOutput output;
        try {
            output = objectMapper.readValue(stdout, PredictOutput.class);
        } catch (Exception e) {
            throw new ModelScoringException("SCORER_BAD_OUTPUT", "채점기 출력 파싱 실패", e);
        }
        if (output == null || !"ok".equals(output.status()) || output.scores() == null) {
            throw new ModelScoringException("SCORER_BAD_OUTPUT", "채점기 비정상 결과: " + stdout);
        }
        return output.scores();
    }
}
