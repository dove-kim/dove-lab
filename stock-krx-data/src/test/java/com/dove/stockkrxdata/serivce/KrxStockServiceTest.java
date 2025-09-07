package com.dove.stockkrxdata.serivce;

import com.dove.stockkrxdata.client.KrxStockClient;
import com.dove.stockkrxdata.client.KrxStockResponse;
import com.dove.stockkrxdata.dto.KrxStockInfo;
import com.dove.stockkrxdata.entity.KrxDailyData;
import com.dove.stockkrxdata.enums.KrxDailyDataStatus;
import com.dove.stockkrxdata.enums.KrxMarketType;
import com.dove.stockkrxdata.repository.KrxDailyDataRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KrxStockServiceTest {

    @Mock
    private KrxStockClient krxStockClient;

    @Mock
    private KrxDailyDataRepository krxDailyDataRepository;

    @InjectMocks
    private KrxStockService krxStockService;

    private LocalDate testDate;
    private String testAuthKey;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2023, 1, 2);
        testAuthKey = "test-api-key";

        // @Value로 주입되는 krxApiAuthKey 필드에 테스트용 값 설정
        ReflectionTestUtils.setField(krxStockService, "krxApiAuthKey", testAuthKey);
    }

    private KrxStockResponse createMockKrxStockResponse(String baseDateStr, String stockCode, String stockName,
                                                        String marketName, String closingPrice, String openingPrice,
                                                        String highPrice, String lowPrice, String tradingVolume) {
        KrxStockResponse.Data data = new KrxStockResponse.Data(
                baseDateStr, stockCode, stockName, marketName, "-",
                closingPrice, "0", "0", openingPrice, highPrice, lowPrice,
                tradingVolume, "0", "0", "0"
        );
        return new KrxStockResponse(List.of(data));
    }

    private KrxStockResponse createMalformedKrxStockResponse(String baseDateStr, String stockCode, String stockName,
                                                             String marketName, String closingPrice) {
        // 숫자가 와야 할 곳에 문자열을 넣어 파싱 오류 유도
        KrxStockResponse.Data data = new KrxStockResponse.Data(
                baseDateStr, stockCode, stockName, marketName, "-",
                closingPrice, "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER",
                "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER", "NOT_A_NUMBER"
        );
        return new KrxStockResponse(List.of(data));
    }

    @Test
    @DisplayName("KOSPI 주식 데이터를 성공적으로 가져오고 저장한다")
    void getStockListBy_kospi_success() {
        // Given
        KrxStockResponse mockResponse = createMockKrxStockResponse(
                "20100104", "004560", "BNG스틸", "KOSPI",
                "8910", "8660", "8910", "8650", "138442"
        );
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(testDate)))
                .thenReturn(mockResponse);

        // When
        List<KrxStockInfo> result = krxStockService.getStockListBy(KrxMarketType.KOSPI, testDate);

        // Then
        assertThat(result).hasSize(1);
        KrxStockInfo stockInfo = result.get(0);
        assertThat(stockInfo.stockCode()).isEqualTo("004560");
        assertThat(stockInfo.stockName()).isEqualTo("BNG스틸");
        assertThat(stockInfo.krxMarketType()).isEqualTo(KrxMarketType.KOSPI);
        assertThat(stockInfo.closingPrice()).isEqualByComparingTo(new BigDecimal("8910"));
        assertThat(stockInfo.tradingVolume()).isEqualTo(138442L);

        // 환경변수에서 설정된 API 키가 사용되었는지 검증
        verify(krxStockClient, times(1)).getDailyKospiStockInfo(eq(testAuthKey), eq(testDate));

        // KrxDailyDataRepository.save가 SUCCESS 상태로 호출되었는지 검증
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataRepository, times(1)).save(captor.capture());
        KrxDailyData savedLog = captor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(KrxDailyDataStatus.SUCCESS);
        assertThat(savedLog.getRawData()).isEqualTo(mockResponse.toJson());
        assertThat(savedLog.getBaseDate()).isEqualTo(testDate);
        assertThat(savedLog.getKrxMarketType()).isEqualTo(KrxMarketType.KOSPI);
    }

    @Test
    @DisplayName("KOSDAQ 주식 데이터를 성공적으로 가져오고 저장한다")
    void getStockListBy_kosdaq_success() {
        // Given
        KrxStockResponse mockResponse = createMockKrxStockResponse(
                "20100104", "069110", "3H", "KOSDAQ",
                "1030", "1050", "1070", "1015", "39144"
        );
        when(krxStockClient.getDailyKosdaqStockInfo(eq(testAuthKey), eq(testDate)))
                .thenReturn(mockResponse);

        // When
        List<KrxStockInfo> result = krxStockService.getStockListBy(KrxMarketType.KOSDAQ, testDate);

        // Then
        assertThat(result).hasSize(1);
        KrxStockInfo stockInfo = result.get(0);
        assertThat(stockInfo.stockCode()).isEqualTo("069110");
        assertThat(stockInfo.stockName()).isEqualTo("3H");
        assertThat(stockInfo.krxMarketType()).isEqualTo(KrxMarketType.KOSDAQ);
        assertThat(stockInfo.closingPrice()).isEqualByComparingTo(new BigDecimal("1030"));
        assertThat(stockInfo.tradingVolume()).isEqualTo(39144L);

        // 환경변수에서 설정된 API 키가 사용되었는지 검증
        verify(krxStockClient, times(1)).getDailyKosdaqStockInfo(eq(testAuthKey), eq(testDate));

        // KrxDailyDataRepository.save가 SUCCESS 상태로 호출되었는지 검증
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataRepository, times(1)).save(captor.capture());
        KrxDailyData savedLog = captor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(KrxDailyDataStatus.SUCCESS);
        assertThat(savedLog.getRawData()).isEqualTo(mockResponse.toJson());
        assertThat(savedLog.getBaseDate()).isEqualTo(testDate);
        assertThat(savedLog.getKrxMarketType()).isEqualTo(KrxMarketType.KOSDAQ);
    }

    @Test
    @DisplayName("API 응답의 OutBlock_1이 비어있을 경우 빈 리스트를 반환하고 로그를 저장한다")
    void getStockListBy_emptyResponseDataList() {
        // Given
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(testDate)))
                .thenReturn(new KrxStockResponse(Collections.emptyList()));

        // When
        List<KrxStockInfo> result = krxStockService.getStockListBy(KrxMarketType.KOSPI, testDate);

        // Then
        assertThat(result).isEmpty();

        // KrxDailyDataRepository.save가 RESPONSE_NULL 상태로 호출되었는지 검증
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataRepository, times(1)).save(captor.capture());
        KrxDailyData savedLog = captor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(KrxDailyDataStatus.BODY_NULL);
        assertThat(savedLog.getRawData()).isNull();
    }

    @Test
    @DisplayName("API 응답 자체가 null일 경우 빈 리스트를 반환하고 로그를 저장한다")
    void getStockListBy_nullResponse() {
        // Given
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(testDate)))
                .thenReturn(null);

        // When
        List<KrxStockInfo> result = krxStockService.getStockListBy(KrxMarketType.KOSPI, testDate);

        // Then
        assertThat(result).isEmpty();

        // KrxDailyDataRepository.save가 RESPONSE_NULL 상태로 호출되었는지 검증
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataRepository, times(1)).save(captor.capture());
        KrxDailyData savedLog = captor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(KrxDailyDataStatus.BODY_NULL);
        assertThat(savedLog.getRawData()).isNull();
    }

    @Test
    @DisplayName("API 응답의 데이터 파싱 중 오류가 발생할 경우 빈 리스트를 반환하고 로그를 저장한다")
    void getStockListBy_parsingError() {
        // Given
        KrxStockResponse mockResponse = createMalformedKrxStockResponse(
                "20100104", "004560", "BNG스틸", "KOSPI", "INVALID_PRICE"
        );
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(testDate)))
                .thenReturn(mockResponse);

        // When
        List<KrxStockInfo> result = krxStockService.getStockListBy(KrxMarketType.KOSPI, testDate);

        // Then
        assertThat(result).isEmpty();

        // KrxDailyDataRepository.save가 RESPONSE_PARSE_ERROR 상태로 호출되었는지 검증
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataRepository, times(1)).save(captor.capture());
        KrxDailyData savedLog = captor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(KrxDailyDataStatus.BODY_ERROR);
        assertThat(savedLog.getRawData()).isNull();
    }

    @Test
    @DisplayName("FeignException.Unauthorized (401) 발생 시 빈 리스트를 반환하고 로그를 저장한다")
    void getStockListBy_unauthorizedException() {
        // Given
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(testDate)))
                .thenThrow(new FeignException.Unauthorized(
                        "Unauthorized",
                        Request.create(Request.HttpMethod.GET,
                                "/test",
                                Collections.emptyMap(),
                                Request.Body.empty(),
                                new RequestTemplate()),
                        new byte[0],
                        Collections.emptyMap())
                );

        // When
        List<KrxStockInfo> result = krxStockService.getStockListBy(KrxMarketType.KOSPI, testDate);

        // Then
        assertThat(result).isEmpty();

        // 올바른 API 키가 사용되었는지 검증 (인증 실패이지만 키는 전달되어야 함)
        verify(krxStockClient, times(1)).getDailyKospiStockInfo(eq(testAuthKey), eq(testDate));

        // KrxDailyDataRepository.save가 AUTH_FAILED 상태로 호출되었는지 검증
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataRepository, times(1)).save(captor.capture());
        KrxDailyData savedLog = captor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(KrxDailyDataStatus.API_AUTH_FAILED);
        assertThat(savedLog.getRawData()).isNull();
    }

    @Test
    @DisplayName("일반적인 FeignException 발생 시 빈 리스트를 반환하고 로그를 저장한다")
    void getStockListBy_genericFeignException() {
        // Given
        when(krxStockClient.getDailyKospiStockInfo(eq(testAuthKey), eq(testDate)))
                .thenThrow(new FeignException.ServiceUnavailable(
                        "Service Unavailable",
                        Request.create(Request.HttpMethod.GET,
                                "/test",
                                Collections.emptyMap(),
                                Request.Body.empty(),
                                new RequestTemplate()),
                        new byte[0],
                        Collections.emptyMap())
                );

        // When
        List<KrxStockInfo> result = krxStockService.getStockListBy(KrxMarketType.KOSPI, testDate);

        // Then
        assertThat(result).isEmpty();

        // API 호출이 이루어졌는지 검증
        verify(krxStockClient, times(1)).getDailyKospiStockInfo(eq(testAuthKey), eq(testDate));

        // KrxDailyDataRepository.save가 FAILED 상태로 호출되었는지 검증
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataRepository, times(1)).save(captor.capture());
        KrxDailyData savedLog = captor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(KrxDailyDataStatus.API_FAILED);
        assertThat(savedLog.getRawData()).isNull();
    }

    @Test
    @DisplayName("지원하지 않는 시장 유형일 경우 빈 리스트를 반환하고 로그를 저장한다")
    void getStockListBy_unsupportedMarketType() {
        // Given
        KrxMarketType unsupportedType = KrxMarketType.KONEX;

        // When
        List<KrxStockInfo> result = krxStockService.getStockListBy(unsupportedType, testDate);

        // Then
        assertThat(result).isEmpty();

        // krxStockClient의 어떤 메서드도 호출되지 않았음을 검증 (KONEX는 UnsupportedOperationException을 발생시키므로)
        verify(krxStockClient, never()).getDailyKospiStockInfo(any(), any());
        verify(krxStockClient, never()).getDailyKosdaqStockInfo(any(), any());

        // KrxDailyDataRepository.save가 UNSUPPORTED_MARKET 상태로 호출되었는지 검증
        ArgumentCaptor<KrxDailyData> captor = ArgumentCaptor.forClass(KrxDailyData.class);
        verify(krxDailyDataRepository, times(1)).save(captor.capture());
        KrxDailyData savedLog = captor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(KrxDailyDataStatus.UNSUPPORTED_MARKET_TYPE);
        assertThat(savedLog.getRawData()).isNull();
        assertThat(savedLog.getKrxMarketType()).isEqualTo(unsupportedType);
    }

    @Test
    @DisplayName("API 키가 올바르게 설정되어 있는지 테스트")
    void verifyApiKeyIsProperlyInjected() {
        // Given & When
        String injectedAuthKey = (String) ReflectionTestUtils.getField(krxStockService, "krxApiAuthKey");

        // Then
        assertThat(injectedAuthKey).isEqualTo(testAuthKey);
    }
}