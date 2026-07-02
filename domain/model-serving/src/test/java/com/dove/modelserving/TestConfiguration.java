package com.dove.modelserving;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * @DataJpaTest 슬라이스가 엔티티·설정을 찾도록 하는 테스트 전용 부트 설정.
 */
@SpringBootApplication
@EntityScan("com.dove")
public class TestConfiguration {
}
