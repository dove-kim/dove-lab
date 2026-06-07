package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KisCurrentPriceResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseSuccessResponse() throws Exception {
        String json = """
                {
                  "rt_cd": "0",
                  "msg_cd": "MCA00000",
                  "msg1": "정상처리 되었습니다.",
                  "output": {
                    "stck_shrn_iscd": "005930",
                    "rprs_mrkt_kor_name": "KOSPI200",
                    "bstp_kor_isnm": "전기.전자",
                    "stck_prpr": "74800",
                    "prdy_vrss": "-300",
                    "prdy_vrss_sign": "5",
                    "prdy_ctrt": "-0.40",
                    "stck_oprc": "75000",
                    "stck_hgpr": "75200",
                    "stck_lwpr": "74500",
                    "stck_mxpr": "97200",
                    "stck_llam": "52400",
                    "stck_sdpr": "75100",
                    "acml_vol": "12345678",
                    "acml_tr_pbmn": "923456789000",
                    "hts_avls": "4461570",
                    "lstn_stcn": "5969782550",
                    "per": "14.23",
                    "pbr": "1.05",
                    "eps": "5256.00",
                    "bps": "71264.00"
                  }
                }
                """;

        KisCurrentPriceResponse response = objectMapper.readValue(json, KisCurrentPriceResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOutput().getStockCode()).isEqualTo("005930");
        assertThat(response.getOutput().getCurrentPriceLong()).isEqualTo(74800L);
        assertThat(response.getOutput().getPriceChangeLong()).isEqualTo(-300L);
        assertThat(response.getOutput().getPriceChangeRateDouble()).isEqualTo(-0.40);
        assertThat(response.getOutput().getOpenPriceLong()).isEqualTo(75000L);
        assertThat(response.getOutput().getHighPriceLong()).isEqualTo(75200L);
        assertThat(response.getOutput().getLowPriceLong()).isEqualTo(74500L);
        assertThat(response.getOutput().getAccumulatedVolumeLong()).isEqualTo(12345678L);
        assertThat(response.getOutput().getPerDouble()).isEqualTo(14.23);
        assertThat(response.getOutput().getPbrDouble()).isEqualTo(1.05);
    }

    @Test
    void shouldParseFailureResponse() throws Exception {
        String json = """
                {
                  "rt_cd": "1",
                  "msg_cd": "EGW00123",
                  "msg1": "종목코드 오류입니다.",
                  "output": {}
                }
                """;

        KisCurrentPriceResponse response = objectMapper.readValue(json, KisCurrentPriceResponse.class);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessageCode()).isEqualTo("EGW00123");
    }

    @Test
    void shouldReturnZeroForBlankNumericFields() throws Exception {
        String json = """
                {
                  "rt_cd": "0",
                  "msg_cd": "MCA00000",
                  "msg1": "",
                  "output": {
                    "stck_prpr": "",
                    "per": "",
                    "acml_vol": ""
                  }
                }
                """;

        KisCurrentPriceResponse response = objectMapper.readValue(json, KisCurrentPriceResponse.class);

        assertThat(response.getOutput().getCurrentPriceLong()).isZero();
        assertThat(response.getOutput().getPerDouble()).isZero();
        assertThat(response.getOutput().getAccumulatedVolumeLong()).isZero();
    }
}
