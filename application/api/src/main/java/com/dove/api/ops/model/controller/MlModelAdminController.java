package com.dove.api.ops.model.controller;

import com.dove.api.global.security.AuthenticatedUser;
import com.dove.api.global.security.authorization.RequireRole;
import com.dove.api.global.security.authorization.Role;
import com.dove.api.ops.model.dto.DeleteScoresRequest;
import com.dove.api.ops.model.dto.DeleteScoresResponse;
import com.dove.api.ops.model.dto.MlModelResponse;
import com.dove.api.ops.model.dto.ResetScoreCursorRequest;
import com.dove.modelserving.application.exception.InvalidModelMetaException;
import com.dove.modelserving.application.exception.ModelActivationException;
import com.dove.modelserving.application.exception.ModelNotFoundException;
import com.dove.modelserving.application.service.ModelLifecycleService;
import com.dove.modelserving.application.service.ModelQueryService;
import com.dove.modelserving.application.service.ModelRegistrationOverrides;
import com.dove.modelserving.application.service.ModelRegistrationService;
import com.dove.modelserving.application.service.ModelScoreCommandService;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.userfeature.application.service.MemberModelGrantCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ROOT 전용 — ML 모델 등록·생명주기·점수 데이터 관리 API.
 */
@RestController
@RequestMapping("/admin/ops/models")
@RequiredArgsConstructor
@RequireRole(Role.ROOT)
public class MlModelAdminController {

    private final ModelRegistrationService registrationService;
    private final ModelLifecycleService lifecycleService;
    private final ModelQueryService queryService;
    private final ModelScoreCommandService scoreCommandService;
    private final MemberModelGrantCommandService modelGrantCommandService;

    /**
     * 모델 아티팩트(.pkl)와 meta.json을 검증해 INACTIVE로 등록한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MlModelResponse register(@RequestParam("artifact") MultipartFile artifact,
                                    @RequestParam("meta") MultipartFile meta,
                                    @RequestParam(value = "name", required = false) String name,
                                    @RequestParam(value = "version", required = false) String version,
                                    @RequestParam(value = "zoneDesc", required = false) String zoneDesc,
                                    @RequestParam(value = "zoneConditions", required = false) String zoneConditions,
                                    @RequestParam(value = "scoreExchanges", required = false) String scoreExchanges,
                                    @RequestParam(value = "scorePriceType", required = false) String scorePriceType,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
        byte[] artifactBytes = readBytes(artifact);
        String metaJson = new String(readBytes(meta), StandardCharsets.UTF_8);
        ModelRegistrationOverrides overrides = new ModelRegistrationOverrides(
                name, version, zoneDesc, parseConditions(zoneConditions));
        try {
            return MlModelResponse.from(registrationService.register(
                    artifactBytes, metaJson, overrides,
                    parseExchanges(scoreExchanges), parsePriceType(scorePriceType),
                    user.username()));
        } catch (InvalidModelMetaException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    private static List<String> parseConditions(String value) {
        if (value == null || value.isBlank()) return null;
        List<String> conditions = new ArrayList<>();
        for (String line : value.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) conditions.add(trimmed);
        }
        return conditions;
    }

    /**
     * 등록된 모든 모델을 반환한다.
     */
    @GetMapping
    public List<MlModelResponse> list() {
        return queryService.findAll().stream().map(MlModelResponse::from).toList();
    }

    /**
     * 단일 모델을 반환한다.
     */
    @GetMapping("/{id}")
    public MlModelResponse get(@PathVariable Long id) {
        try {
            return MlModelResponse.from(queryService.findById(id));
        } catch (ModelNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND");
        }
    }

    /**
     * 드라이런 검증을 통과하면 모델을 ACTIVE로 전환한다.
     */
    @PostMapping("/{id}/activate")
    public MlModelResponse activate(@PathVariable Long id) {
        try {
            return MlModelResponse.from(lifecycleService.activate(id));
        } catch (ModelNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND");
        } catch (ModelActivationException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    /**
     * 모델을 INACTIVE로 전환한다.
     */
    @PostMapping("/{id}/deactivate")
    public MlModelResponse deactivate(@PathVariable Long id) {
        try {
            return MlModelResponse.from(lifecycleService.deactivate(id));
        } catch (ModelNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND");
        }
    }

    /**
     * 채점 커서를 지정 거래일로 되돌리고 그 이후 점수를 삭제한다. toDate가 null이면 미시작으로 되돌린다.
     */
    @PostMapping("/{id}/reset-cursor")
    public MlModelResponse resetCursor(@PathVariable Long id,
                                       @RequestBody(required = false) ResetScoreCursorRequest request) {
        try {
            return MlModelResponse.from(lifecycleService.resetScoreCursor(
                    id, request == null ? null : request.toDate()));
        } catch (ModelNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND");
        }
    }

    /**
     * 모델과 그 모델의 모든 점수를 함께 삭제한다.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        try {
            lifecycleService.delete(id);
            modelGrantCommandService.revokeAllForModel(id);
        } catch (ModelNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND");
        }
    }

    /**
     * 모델 점수를 삭제한다. ticker가 있으면 종목별, from/to가 있으면 기간별, 둘 다 없으면 전체 삭제.
     */
    @PostMapping("/{id}/scores/delete")
    public DeleteScoresResponse deleteScores(@PathVariable Long id,
                                             @RequestBody DeleteScoresRequest request) {
        if (!request.confirm()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DELETE_NOT_CONFIRMED");
        }
        long deleted;
        if (request.ticker() != null && !request.ticker().isBlank()) {
            deleted = scoreCommandService.deleteByTicker(id, request.ticker());
        } else if (request.from() != null || request.to() != null) {
            deleted = scoreCommandService.deleteByDateRange(id, request.from(), request.to());
        } else {
            deleted = scoreCommandService.deleteAll(id);
        }
        return new DeleteScoresResponse(deleted);
    }

    private static byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MISSING_FILE");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED");
        }
    }

    private static Set<StockExchange> parseExchanges(String value) {
        if (value == null || value.isBlank()) return Set.of();
        Set<StockExchange> exchanges = new LinkedHashSet<>();
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            try {
                exchanges.add(StockExchange.valueOf(trimmed.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_EXCHANGE");
            }
        }
        return exchanges;
    }

    private static PriceType parsePriceType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return PriceType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PRICE_TYPE");
        }
    }
}
