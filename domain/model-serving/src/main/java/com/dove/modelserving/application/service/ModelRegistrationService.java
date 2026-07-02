package com.dove.modelserving.application.service;

import com.dove.modelserving.application.exception.InvalidModelMetaException;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelOutputType;
import com.dove.modelserving.domain.feature.FeatureResolver;
import com.dove.modelserving.domain.meta.FeatureHasher;
import com.dove.modelserving.domain.meta.ModelMeta;
import com.dove.modelserving.domain.meta.ModelMetaParser;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 모델 아티팩트·meta.json을 검증해 INACTIVE 상태로 등록하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ModelRegistrationService {

    private final ModelMetaParser metaParser;
    private final FeatureResolver featureResolver;
    private final MlModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    /**
     * 오버라이드 없이 meta.json 값 그대로 등록한다.
     *
     * @throws InvalidModelMetaException 스키마 누락·미지의 피처·해시 불일치 시
     */
    public MlModel register(byte[] artifact, String metaJson, Set<StockExchange> scoreExchanges,
                            PriceType scorePriceType, String createdBy) {
        return register(artifact, metaJson, ModelRegistrationOverrides.none(),
                scoreExchanges, scorePriceType, createdBy);
    }

    /**
     * 업로드된 모델을 검증 후 INACTIVE로 등록한다. 이름·버전·진입존은 오버라이드로 덮어쓸 수 있다.
     * 검증: ① meta 스키마 ② 전 피처가 레지스트리로 해석됨 ③ feature_hash 재계산 일치.
     * 채점 대상이 비면 거래소={KOSPI, KOSDAQ}, 주가유형=ADJUSTED로 둔다.
     *
     * @throws InvalidModelMetaException 스키마 누락·미지의 피처·해시 불일치 시
     */
    public MlModel register(byte[] artifact, String metaJson, ModelRegistrationOverrides overrides,
                            Set<StockExchange> scoreExchanges, PriceType scorePriceType, String createdBy) {
        ModelMeta meta = metaParser.parse(metaJson);
        String name = override(overrides.name(), meta.name());
        String version = override(overrides.version(), meta.version());
        if (isBlank(name) || isBlank(version) || isBlank(meta.outputType())
                || isBlank(meta.featureHash()) || meta.features() == null || meta.features().isEmpty()) {
            throw new InvalidModelMetaException("INVALID_META_SCHEMA");
        }
        validateFeatures(meta.features());
        validateFeatureHash(meta);

        ModelOutputType outputType = ModelOutputType.parseOrNull(meta.outputType().toUpperCase());
        if (outputType == null) {
            throw new InvalidModelMetaException("INVALID_OUTPUT_TYPE");
        }

        String finalMeta = applyOverrides(metaJson, name, version, overrides.zoneDesc(), overrides.zoneConditions());
        return modelRepository.save(MlModel.register(
                name, version, artifact, finalMeta, outputType, scoreExchanges, scorePriceType, createdBy));
    }

    /**
     * 사용자 입력 name·version·진입존을 metaJson에 반영해 새 JSON 문자열을 만든다(서빙이 meta에서 진입존을 읽기 때문).
     *
     * @throws InvalidModelMetaException JSON 가공 실패 시
     */
    private String applyOverrides(String metaJson, String name, String version,
                                  String zoneDesc, List<String> zoneConditions) {
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(metaJson);
            root.put("name", name);
            root.put("version", version);
            if (zoneDesc != null || zoneConditions != null) {
                ObjectNode zone = root.path("entry_zone").isObject()
                        ? (ObjectNode) root.get("entry_zone") : objectMapper.createObjectNode();
                if (zoneDesc != null) {
                    zone.put("desc", zoneDesc);
                }
                if (zoneConditions != null) {
                    ArrayNode arr = objectMapper.createArrayNode();
                    zoneConditions.forEach(arr::add);
                    zone.set("conditions", arr);
                }
                root.set("entry_zone", zone);
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new InvalidModelMetaException("META_PATCH_FAILED");
        }
    }

    private static String override(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private void validateFeatures(List<String> features) {
        for (String feature : features) {
            if (!featureResolver.isResolvable(feature)) {
                throw new InvalidModelMetaException("UNKNOWN_FEATURE: " + feature);
            }
        }
    }

    private void validateFeatureHash(ModelMeta meta) {
        String recomputed = FeatureHasher.hash(meta.features());
        if (!recomputed.equals(meta.featureHash())) {
            throw new InvalidModelMetaException("FEATURE_HASH_MISMATCH");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
