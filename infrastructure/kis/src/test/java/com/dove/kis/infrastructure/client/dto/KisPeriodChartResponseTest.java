package com.dove.kis.infrastructure.client.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KisPeriodChartResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseOutput2Array() throws Exception {
        String json = """
                {
                  "rt_cd": "0",
                  "msg_cd": "MCA00000",
                  "msg1": "정상처리 되었습니다.",
                  "output1": {
                    "stck_shrn_iscd": "000660",
                    "hts_kor_isnm": "SK하이닉스",
                    "stck_prpr": "112000",
                    "per": "8.49",
                    "pbr": "1.26",
                    "eps": "13190.00"
                  },
                  "output2": [
                    {
                      "stck_bsop_date": "20220509",
                      "stck_clpr": "112000",
                      "stck_oprc": "111000",
                      "stck_hgpr": "113000",
                      "stck_lwpr": "110500",
                      "acml_vol": "2106409",
                      "acml_tr_pbmn": "236062833000",
                      "flng_cls_code": "00",
                      "prdy_vrss_sign": "2",
                      "prdy_vrss": "1000"
                    },
                    {
                      "stck_bsop_date": "20220506",
                      "stck_clpr": "111000",
                      "stck_oprc": "109000",
                      "stck_hgpr": "111500",
                      "stck_lwpr": "108000",
                      "acml_vol": "3680049",
                      "acml_tr_pbmn": "401234567000",
                      "flng_cls_code": "00",
                      "prdy_vrss_sign": "5",
                      "prdy_vrss": "-2000"
                    }
                  ]
                }
                """;

        KisPeriodChartResponse response = objectMapper.readValue(json, KisPeriodChartResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOutput1().getStockCode()).isEqualTo("000660");
        assertThat(response.getOutput2()).hasSize(2);

        KisPeriodChartBar first = response.getOutput2().get(0);
        assertThat(first.getTradingDate()).isEqualTo("20220509");
        assertThat(first.getClosePriceLong()).isEqualTo(112000L);
        assertThat(first.getOpenPriceLong()).isEqualTo(111000L);
        assertThat(first.getHighPriceLong()).isEqualTo(113000L);
        assertThat(first.getLowPriceLong()).isEqualTo(110500L);
        assertThat(first.getAccumulatedVolumeLong()).isEqualTo(2106409L);
        assertThat(first.getPriceChangeLong()).isEqualTo(1000L);
        assertThat(first.getChangeCode()).isEqualTo("00");
    }

    @Test
    void shouldReturnEmptyListWhenOutput2IsNull() throws Exception {
        String json = """
                {
                  "rt_cd": "0",
                  "msg_cd": "MCA00000",
                  "msg1": "",
                  "output1": {}
                }
                """;

        KisPeriodChartResponse response = objectMapper.readValue(json, KisPeriodChartResponse.class);

        assertThat(response.getOutput2()).isNull();
    }
}
