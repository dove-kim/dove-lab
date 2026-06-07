package com.dove.api.search.stock.dto;

import java.util.Map;

/**
 * 종목투자의견 한 건 (회원사별).
 */
public record InvestOpinionResponse(
        String date,
        String opinion,
        String prevOpinion,
        String broker,
        String goalPrice
) {
    public static InvestOpinionResponse from(Map<String, Object> m) {
        return new InvestOpinionResponse(
                str(m, "stck_bsop_date"),
                str(m, "invt_opnn"),
                str(m, "rgbf_invt_opnn"),
                str(m, "mbcr_name"),
                str(m, "hts_goal_prc"));
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString().trim();
    }
}
