package com.dove.workspace;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 임시 작업 파일 관리자 — 앱 전용 루트 아래 scope 하위폴더로 파일을 격리하고, 시작 시·작업 후 정리한다.
 */
@Slf4j
@Component
public class Workspace {

    private final Path appRoot;

    /**
     * 루트 = {@code workspace.base-dir}/{앱이름}. 설정이 비면 OS 임시폴더 하위(dove-work)를 쓴다.
     */
    public Workspace(WorkspaceProperties properties,
                     @Value("${spring.application.name:app}") String appName) {
        String base = properties.getBaseDir().isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "dove-work").toString()
                : properties.getBaseDir();
        this.appRoot = Path.of(base, appName).toAbsolutePath();
    }

    /**
     * 시작 시 앱 전용 루트를 통째로 비운다 — 이전 실행이 비정상 종료(kill 등)로 남긴 파일 제거.
     */
    @PostConstruct
    void init() {
        WorkspaceFiles.cleanContents(appRoot);
        createDir(appRoot);
        log.info("작업 디렉터리 루트: {}", appRoot);
    }

    /**
     * scope 전용 작업 영역을 연다 — 기존 내용은 먼저 비우고(작업 전 정리), 핸들 close 시 삭제(작업 후 정리).
     */
    public WorkArea open(String scope) {
        Path dir = appRoot.resolve(scope);
        WorkspaceFiles.deleteRecursively(dir);
        createDir(dir);
        return new WorkArea(dir);
    }

    /**
     * scope 디렉터리를 반환한다(없으면 생성). 호출자가 파일 수명을 직접 관리하는 경우.
     */
    public Path scope(String scope) {
        Path dir = appRoot.resolve(scope);
        createDir(dir);
        return dir;
    }

    /**
     * 파일 하나를 best-effort 삭제한다.
     */
    public void delete(Path path) {
        WorkspaceFiles.deleteQuietly(path);
    }

    private void createDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("작업 디렉터리 생성 실패: {}", dir, e);
        }
    }
}
