package com.dove.technicalindicator.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@Getter
@Setter
@ConfigurationProperties("indicator.cursor")
public class IndicatorCursorProperties {
    private LocalDate initialDate = LocalDate.of(2010, 1, 1);
    private String sweepCron = "0 0 3 * * ?";
}
