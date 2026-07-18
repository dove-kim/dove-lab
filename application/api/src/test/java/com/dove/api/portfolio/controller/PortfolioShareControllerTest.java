package com.dove.api.portfolio.controller;

import com.dove.api.TestApiApplication;
import com.dove.api.support.WithApiUser;
import com.dove.auth.application.service.CredentialService;
import com.dove.auth.domain.entity.Credential;
import com.dove.auth.domain.enums.MemberRole;
import com.dove.portfolio.application.port.FxRatePort;
import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioShareService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.domain.enums.PortfolioSharePermission;
import com.dove.portfolio.domain.enums.TxType;
import com.dove.user.application.service.MemberProfileCommandService;
import com.dove.user.domain.entity.MemberProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 포트폴리오 계좌 공유 API 통합 테스트.
 */
@SpringBootTest(classes = TestApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class PortfolioShareControllerTest {

    private static final long OWNER_ID = 1L;
    private static final long GRANTEE_ID = 2L;

    @Autowired MockMvc mockMvc;
    @Autowired PortfolioAccountService accountService;
    @Autowired PortfolioTransactionService transactionService;
    @Autowired PortfolioShareService shareService;
    @Autowired MemberProfileCommandService memberProfileCommandService;
    @Autowired CredentialService credentialService;

    @MockitoBean OverseasPricePort overseasPricePort;
    @MockitoBean FxRatePort fxRatePort;

    @Nested
    @DisplayName("공유 부여")
    class Grant {
        @Test
        @DisplayName("인증 없으면 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/portfolio/shares")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithApiUser(memberId = OWNER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("소유 계좌를 아이디로 공유하면 OUT 공유가 생성된다")
        void shouldGrantByUsername() throws Exception {
            long accountId = accountService.create(OWNER_ID, "장투", null, null, "owner").getId();
            MemberProfile friend = memberProfileCommandService.save(
                    MemberProfile.create("friend@test.com", "친구", MemberRole.USER));
            credentialService.save(Credential.create(friend.getId(), "friend", "x"));

            mockMvc.perform(post("/portfolio/shares")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accountId\":" + accountId + ",\"granteeUsername\":\"friend\",\"permission\":\"READ\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.direction").value("OUT"))
                    .andExpect(jsonPath("$.permission").value("READ"))
                    .andExpect(jsonPath("$.grantee").value("친구 (friend)"));
        }

        @Test
        @WithApiUser(memberId = OWNER_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("없는 아이디로 공유하면 404")
        void shouldReturn404WhenGranteeUnknown() throws Exception {
            long accountId = accountService.create(OWNER_ID, "장투", null, null, "owner").getId();

            mockMvc.perform(post("/portfolio/shares")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"accountId\":" + accountId + ",\"granteeUsername\":\"nobody\",\"permission\":\"READ\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("공유받은 계좌 열람")
    class SharedView {
        @Test
        @WithApiUser(memberId = GRANTEE_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("공유받은 계좌 요약은 열람 가능하다")
        void shouldReadSharedSummary() throws Exception {
            MemberProfile owner = memberProfileCommandService.save(
                    MemberProfile.create("owner@test.com", "주인", MemberRole.USER));
            long accountId = accountService.create(owner.getId(), "미국", null, null, "owner").getId();
            transactionService.create(owner.getId(), accountId, TxType.BUY, LocalDate.of(2026, 7, 1), "삼성전자", "KRW",
                    new BigDecimal("10"), new BigDecimal("70000"), BigDecimal.valueOf(700_000), 0L, null, null, "owner");
            shareService.grant(owner.getId(), accountId, GRANTEE_ID, PortfolioSharePermission.READ, "owner");

            mockMvc.perform(get("/portfolio/shared/" + accountId + "/summary"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithApiUser(memberId = GRANTEE_ID, capabilities = {"PORTFOLIO_LEDGER"})
        @DisplayName("공유받지 않은 계좌는 403")
        void shouldReturn403WhenNotShared() throws Exception {
            MemberProfile owner = memberProfileCommandService.save(
                    MemberProfile.create("owner2@test.com", "주인2", MemberRole.USER));
            long accountId = accountService.create(owner.getId(), "비공유", null, null, "owner").getId();

            mockMvc.perform(get("/portfolio/shared/" + accountId + "/summary"))
                    .andExpect(status().isForbidden());
        }
    }
}
