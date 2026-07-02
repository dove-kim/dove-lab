package com.dove.modelserving.infrastructure.scorer;

import com.dove.modelserving.application.exception.ModelScoringException;
import com.dove.workspace.Workspace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * DB에 보관된 모델 아티팩트(LONGBLOB)를 채점기가 읽을 임시 파일로 풀어 주는 도구.
 */
@Slf4j
@Component
public class ArtifactMaterializer {

    private static final String SCOPE = "model-artifacts";

    private final Workspace workspace;

    public ArtifactMaterializer(Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * 아티팩트 바이트를 모델 전용 작업 폴더의 임시 .pkl 파일로 쓰고 그 경로를 반환한다. JVM 종료 시 삭제 예약.
     *
     * @throws ModelScoringException 임시 파일 작성 실패 시
     */
    public Path materialize(Long modelId, byte[] artifact) {
        try {
            Path dir = workspace.scope(SCOPE);
            Path path = Files.createTempFile(dir, "model-" + modelId + "-", ".pkl");
            path.toFile().deleteOnExit();
            Files.write(path, artifact);
            return path;
        } catch (IOException e) {
            throw new ModelScoringException("ARTIFACT_WRITE_FAILED", "모델 아티팩트 임시 파일 작성 실패", e);
        }
    }

    /**
     * 임시 아티팩트 파일을 삭제한다(실패는 무시).
     */
    public void cleanup(Path path) {
        workspace.delete(path);
    }
}
