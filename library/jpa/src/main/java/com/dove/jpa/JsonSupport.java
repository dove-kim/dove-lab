package com.dove.jpa;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 컬럼 직렬화용 공유 ObjectMapper.
 *
 * <p>모르는(삭제된) 필드를 무시하는 관용적 reader라, 저장 구조에 필드가 추가·삭제돼도 옛 데이터가 깨지지 않는다.
 */
public final class JsonSupport {

    /**
     * 관용적 ObjectMapper — 알 수 없는 필드는 무시한다(필드 추가·삭제 안전).
     */
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonSupport() {
    }
}
