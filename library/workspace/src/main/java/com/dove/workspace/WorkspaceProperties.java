package com.dove.workspace;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 임시 작업 파일 루트 설정 — 로컬은 프로젝트 내부 경로(env), 운영은 컨테이너 전용 폴더.
 */
@ConfigurationProperties(prefix = "workspace")
@Getter
@Setter
public class WorkspaceProperties {

    /**
     * 작업 파일 루트. 비면 OS 임시폴더 하위(dove-work)를 쓴다. 운영은 컨테이너 볼륨 경로 주입.
     */
    private String baseDir = "";
}
