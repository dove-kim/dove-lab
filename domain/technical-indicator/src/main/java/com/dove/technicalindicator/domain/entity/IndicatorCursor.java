package com.dove.technicalindicator.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "INDICATOR_CURSOR")
@Comment("calculator별·종목별 기술적 지표 계산 커서. 신규 calculator 추가 시 그 cursorType만 별도로 backfill")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class IndicatorCursor {

    @EmbeddedId
    private IndicatorCursorId id;

    @Column(name = "LAST_CALCULATED_DATE", nullable = false)
    @Comment("지표 계산이 마지막으로 진행된 날짜 (다음 advance는 그 다음 개장일)")
    private LocalDate lastCalculatedDate;

    public void advance(LocalDate date) {
        this.lastCalculatedDate = date;
    }

    public void rewindTo(LocalDate date) {
        this.lastCalculatedDate = date;
    }
}
