package com.dove.screening.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(
    name = "STOCK_SET",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_STOCK_SET_MEMBER_NAME", columnNames = {"MEMBER_ID", "NAME"})
    },
    indexes = {
        @Index(name = "IDX_STOCK_SET_MEMBER_ID", columnList = "MEMBER_ID")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Comment("종목 세트 고유 ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    @Comment("소유 회원 ID")
    private Long memberId;

    @Column(name = "NAME", nullable = false, length = 100)
    @Comment("세트 이름 (사용자 내 고유)")
    private String name;

    @Column(name = "CODES", nullable = false, columnDefinition = "TEXT")
    @Comment("종목코드 목록 (쉼표 구분)")
    private String codes;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    public List<String> getCodeList() {
        if (codes == null || codes.isBlank()) return List.of();
        return Arrays.stream(codes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public Set<String> getCodeSet() {
        return Set.copyOf(getCodeList());
    }

    public static StockSet create(Long memberId, String name, List<String> codeList) {
        StockSet s = new StockSet();
        s.memberId = memberId;
        s.name = name;
        s.codes = String.join(",", codeList);
        s.createdAt = LocalDateTime.now();
        s.updatedAt = LocalDateTime.now();
        return s;
    }

    public void update(String name, List<String> codeList) {
        this.name = name;
        this.codes = String.join(",", codeList);
        this.updatedAt = LocalDateTime.now();
    }
}
