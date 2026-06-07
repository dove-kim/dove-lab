package com.dove.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * init.sql과 JPA 엔티티의 정합성을 빌드 시점에 검증한다.
 *
 * <p>엔티티 소스(@Table·@Column)와 scripts/init.sql을 대조해
 * (1) 모든 엔티티 테이블이 init.sql에 존재하고,
 * (2) 엔티티의 모든 컬럼이 init.sql에 정의돼 있으며,
 * (3) init.sql에 엔티티 없는 고아 테이블이 없는지 확인한다.
 *
 * <p>테스트가 ddl-auto(H2)로 돌아 init.sql을 검증하지 못하는 사각지대를 메운다.
 * (컬럼 존재·테이블 매핑 수준 검증 — 타입/길이는 범위 밖)
 */
class InitSqlSchemaConsistencyTest {

    private static final Pattern TABLE = Pattern.compile(
            "CREATE TABLE IF NOT EXISTS (\\w+)\\s*\\((.*?)\\n\\)\\s*COMMENT", Pattern.DOTALL);
    private static final Pattern SQL_COL = Pattern.compile(
            "^\\s*([A-Z_]+)\\s+(VARCHAR|BIGINT|INT|DOUBLE|FLOAT|DATE|DATETIME|TINYINT|JSON|TEXT)\\b");
    private static final Pattern COL = Pattern.compile(
            "@(?:Column|JoinColumn)\\(\\s*name\\s*=\\s*\"([A-Za-z_]+)\"");
    private static final Pattern TABLE_NAME = Pattern.compile("name\\s*=\\s*\"([A-Za-z_]+)\"");
    private static final Pattern EMBEDDED_ID = Pattern.compile("@EmbeddedId\\s+private\\s+(\\w+)\\b");

    @Test
    @DisplayName("init.sql 과 엔티티 컬럼이 정합한다")
    void initSqlMatchesEntities() throws IOException {
        Path root = projectRoot();
        Map<String, Set<String>> sqlTables = parseInitSql(Files.readString(root.resolve("scripts/init.sql")));

        Map<String, Set<String>> idCols = new HashMap<>();   // 임베디드 Id 클래스명 → 컬럼
        Map<String, EntityInfo> entities = new HashMap<>();   // 테이블명 → 엔티티 정보

        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> javaFiles = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().replace('\\', '/').contains("/src/main/"))
                    .filter(p -> !p.toString().replace('\\', '/').contains("/build/"))
                    .toList();

            for (Path f : javaFiles) {
                String src = Files.readString(f);
                if (src.contains("@Embeddable")) {
                    idCols.put(className(f), columns(src));
                }
                if (src.contains("@Entity")) {
                    String table = tableName(src);
                    if (table == null) continue;
                    entities.put(table, new EntityInfo(className(f), src));
                }
            }
        }

        // 임베디드 Id 컬럼 병합
        List<String> failures = new ArrayList<>();
        for (var e : entities.entrySet()) {
            String table = e.getKey();
            EntityInfo info = e.getValue();
            Set<String> ecols = new HashSet<>(columns(info.src()));
            Matcher m = EMBEDDED_ID.matcher(info.src());
            if (m.find()) {
                ecols.addAll(idCols.getOrDefault(m.group(1), Set.of()));
            }
            Set<String> scols = sqlTables.get(table);
            if (scols == null) {
                failures.add("[INIT 누락] 테이블 " + table + " (" + info.className() + ") 가 init.sql에 없음");
                continue;
            }
            Set<String> missing = new TreeSet<>(ecols);
            missing.removeAll(scols);
            if (!missing.isEmpty()) {
                failures.add("[컬럼 누락] " + table + " (" + info.className() + ") init.sql에 없음: " + missing);
            }
        }

        // 고아 테이블 (init.sql엔 있는데 엔티티 없음)
        Set<String> orphans = new TreeSet<>(sqlTables.keySet());
        orphans.removeAll(entities.keySet());
        if (!orphans.isEmpty()) {
            failures.add("[고아 테이블] init.sql에만 존재(엔티티 없음): " + orphans);
        }

        assertThat(failures)
                .as("init.sql ↔ 엔티티 불일치:\n" + String.join("\n", failures))
                .isEmpty();
    }

    private static Map<String, Set<String>> parseInitSql(String sql) {
        Map<String, Set<String>> tables = new HashMap<>();
        Matcher t = TABLE.matcher(sql);
        while (t.find()) {
            Set<String> cols = new HashSet<>();
            for (String line : t.group(2).split("\n")) {
                Matcher c = SQL_COL.matcher(line);
                if (c.find()) cols.add(c.group(1));
            }
            tables.put(t.group(1), cols);
        }
        return tables;
    }

    private static Set<String> columns(String src) {
        Set<String> cols = new HashSet<>();
        Matcher m = COL.matcher(src);
        while (m.find()) cols.add(m.group(1));
        return cols;
    }

    /** @Table( 직후 첫 name="..." 이 테이블명. 없으면 null. */
    private static String tableName(String src) {
        int idx = src.indexOf("@Table");
        if (idx < 0) return null;
        Matcher m = TABLE_NAME.matcher(src).region(idx, src.length());
        return m.find() ? m.group(1) : null;
    }

    private static String className(Path f) {
        String n = f.getFileName().toString();
        return n.substring(0, n.length() - ".java".length());
    }

    /** user.dir 에서 위로 올라가며 settings.gradle 이 있는 프로젝트 루트를 찾는다. */
    private static Path projectRoot() {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (p != null) {
            if (Files.exists(p.resolve("settings.gradle")) || Files.exists(p.resolve("settings.gradle.kts"))) {
                return p;
            }
            p = p.getParent();
        }
        throw new UncheckedIOException(new IOException("프로젝트 루트(settings.gradle)를 찾지 못함"));
    }
}
