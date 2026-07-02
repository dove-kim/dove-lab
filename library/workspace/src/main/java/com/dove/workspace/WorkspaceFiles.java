package com.dove.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 작업 파일 삭제 유틸(best-effort).
 */
final class WorkspaceFiles {

    private WorkspaceFiles() {
    }

    /**
     * 디렉터리와 그 내용을 재귀적으로 삭제한다(실패는 무시).
     */
    static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(WorkspaceFiles::deleteQuietly);
        } catch (IOException ignored) {
            // best-effort
        }
    }

    /**
     * 디렉터리 내용만 삭제하고 디렉터리 자체는 남긴다.
     */
    static void cleanContents(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> children = Files.list(dir)) {
            children.forEach(WorkspaceFiles::deleteRecursively);
        } catch (IOException ignored) {
            // best-effort
        }
    }

    static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort
        }
    }
}
