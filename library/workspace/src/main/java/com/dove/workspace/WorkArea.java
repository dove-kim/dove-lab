package com.dove.workspace;

import java.nio.file.Path;

/**
 * 한 작업(scope) 전용 임시 디렉터리 핸들. try-with-resources 종료 시 디렉터리 전체를 삭제한다.
 */
public final class WorkArea implements AutoCloseable {

    private final Path dir;

    WorkArea(Path dir) {
        this.dir = dir;
    }

    /**
     * 작업 디렉터리 경로.
     */
    public Path dir() {
        return dir;
    }

    /**
     * 작업 디렉터리 하위의 파일 경로를 만든다.
     */
    public Path resolve(String fileName) {
        return dir.resolve(fileName);
    }

    /**
     * 작업 디렉터리 전체를 삭제한다(작업 종료 후 자원 반납).
     */
    @Override
    public void close() {
        WorkspaceFiles.deleteRecursively(dir);
    }
}
