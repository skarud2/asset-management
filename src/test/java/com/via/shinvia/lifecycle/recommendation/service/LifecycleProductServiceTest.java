package com.via.shinvia.lifecycle.recommendation.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleProductDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.recommendation.adapter.LoanRecommendationAdapter;
import com.via.shinvia.lifecycle.recommendation.adapter.PolicyRecommendationAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecycleProductServiceTest {

    @Mock
    private LoanRecommendationAdapter loanRecommendationAdapter;

    @Mock
    private PolicyRecommendationAdapter policyRecommendationAdapter;

    @InjectMocks
    private LifecycleProductService lifecycleProductService;

    @Test
    void getRecommendedProductsReturnsListWithoutException() {
        when(loanRecommendationAdapter.recommend(
                anyString(),
                any(LifecycleEventType.class),
                any(),
                any(),
                anyInt()
        )).thenReturn(List.of());
        when(policyRecommendationAdapter.recommend(
                any(),
                any(LifecycleEventType.class),
                anyInt()
        )).thenReturn(List.of());

        List<LifecycleProductDto> products = assertDoesNotThrow(() ->
                lifecycleProductService.getRecommendedProducts(
                        1L,
                        "user@example.com",
                        LifecycleEventType.JEONSE
                )
        );

        assertNotNull(products);
        assertTrue(products.isEmpty());
    }

    @Test
    void getRecommendedProductsMergesDeduplicatesAndLimitsResults() {
        LifecycleProductDto loan1 = LifecycleProductDto.builder()
                .productId(1L)
                .productType("JEONSE_LOAN")
                .productName("전세대출 A")
                .build();

        LifecycleProductDto duplicatedLoan1 = LifecycleProductDto.builder()
                .productId(1L)
                .productType("JEONSE_LOAN")
                .productName("전세대출 A 중복")
                .build();

        LifecycleProductDto loan2 = LifecycleProductDto.builder()
                .productId(2L)
                .productType("JEONSE_LOAN")
                .productName("전세대출 B")
                .build();

        LifecycleProductDto policy1 = LifecycleProductDto.builder()
                .productId(10L)
                .productType("POLICY_LOAN")
                .productName("전세 정책대출 A")
                .build();

        LifecycleProductDto duplicatedPolicy1 = LifecycleProductDto.builder()
                .productId(10L)
                .productType("POLICY_LOAN")
                .productName("전세 정책대출 A 중복")
                .build();

        LifecycleProductDto policy2 = LifecycleProductDto.builder()
                .productId(11L)
                .productType("POLICY_LOAN")
                .productName("전세 정책대출 B")
                .build();

        LifecycleProductDto policy3 = LifecycleProductDto.builder()
                .productId(12L)
                .productType("POLICY_LOAN")
                .productName("전세 정책대출 C")
                .build();

        LifecycleProductDto policy4 = LifecycleProductDto.builder()
                .productId(13L)
                .productType("POLICY_LOAN")
                .productName("전세 정책대출 D")
                .build();

        when(loanRecommendationAdapter.recommend(
                anyString(),
                any(LifecycleEventType.class),
                any(),
                any(),
                anyInt()
        )).thenReturn(List.of(
                loan1,
                duplicatedLoan1,
                loan2
        ));

        when(policyRecommendationAdapter.recommend(
                any(),
                any(LifecycleEventType.class),
                anyInt()
        )).thenReturn(List.of(
                policy1,
                duplicatedPolicy1,
                policy2,
                policy3,
                policy4
        ));

        List<LifecycleProductDto> products =
                lifecycleProductService.getRecommendedProducts(
                        1L,
                        "user@example.com",
                        LifecycleEventType.JEONSE
                );

        assertNotNull(products);
        assertEquals(5, products.size());

        assertEquals("전세대출 A", products.get(0).getProductName());
        assertEquals("전세대출 B", products.get(1).getProductName());
        assertEquals("전세 정책대출 A", products.get(2).getProductName());
        assertEquals("전세 정책대출 B", products.get(3).getProductName());
        assertEquals("전세 정책대출 C", products.get(4).getProductName());
    }

    @Test
    void marriageReturnsOnlyExplicitMarriagePurposeProducts() {
        LifecycleProductDto livingLoan = product(1L, "i-ONE근로자생활안정자금대출");
        LifecycleProductDto mortgage = product(2L, "내집마련디딤돌대출");
        LifecycleProductDto medicalLoan = product(3L, "산재근로자 생활안정자금 융자(의료비)");
        LifecycleProductDto weddingLoan = product(4L, "근로자 혼례비 융자");

        when(loanRecommendationAdapter.recommend(
                anyString(), any(LifecycleEventType.class), any(), any(), anyInt()
        )).thenReturn(List.of(livingLoan, weddingLoan));
        when(policyRecommendationAdapter.recommend(
                any(), any(LifecycleEventType.class), anyInt()
        )).thenReturn(List.of(mortgage, medicalLoan));

        List<LifecycleProductDto> products =
                lifecycleProductService.getRecommendedProducts(
                        1L,
                        "user@example.com",
                        LifecycleEventType.MARRIAGE,
                        new BigDecimal("10000000"),
                        36
                );

        assertEquals(1, products.size());
        assertEquals("근로자 혼례비 융자", products.get(0).getProductName());
    }

    @Test
    void homePurchaseExcludesLivingLoanAndKeepsMortgageProducts() {
        LifecycleProductDto livingLoan = product(1L, "i-ONE근로자생활안정자금대출");
        LifecycleProductDto mortgage = product(2L, "수익공유형모기지");
        LifecycleProductDto newlywed = product(3L, "신혼부부전용 구입자금");

        when(loanRecommendationAdapter.recommend(
                anyString(), any(LifecycleEventType.class), any(), any(), anyInt()
        )).thenReturn(List.of(livingLoan, mortgage));
        when(policyRecommendationAdapter.recommend(
                any(), any(LifecycleEventType.class), anyInt()
        )).thenReturn(List.of(newlywed));

        List<LifecycleProductDto> products =
                lifecycleProductService.getRecommendedProducts(
                        1L,
                        "user@example.com",
                        LifecycleEventType.HOME_PURCHASE,
                        new BigDecimal("300000000"),
                        360
                );

        assertEquals(2, products.size());
        assertEquals("수익공유형모기지", products.get(0).getProductName());
        assertEquals("신혼부부전용 구입자금", products.get(1).getProductName());
    }

    @Test
    void childbirthExcludesProductsWithSpecialEligibilityConditions() {
        LifecycleProductDto generalChildcare = product(1L, "우리아이 육아 적금");
        LifecycleProductDto multiChild = product(2L, "다자녀 행복 대출");
        LifecycleProductDto singleParent = product(3L, "한부모 자녀 지원 적금");
        LifecycleProductDto youthParent = product(4L, "청년 부모 출산 우대 적금");
        LifecycleProductDto unrelated = product(5L, "직장인 행복 신용대출");

        when(loanRecommendationAdapter.recommend(
                anyString(), any(LifecycleEventType.class), any(), any(), anyInt()
        )).thenReturn(List.of(generalChildcare, multiChild, unrelated));
        when(policyRecommendationAdapter.recommend(
                any(), any(LifecycleEventType.class), anyInt()
        )).thenReturn(List.of(singleParent, youthParent));

        List<LifecycleProductDto> products = lifecycleProductService.getRecommendedProducts(
                1L,
                "user@example.com",
                LifecycleEventType.CHILDBIRTH
        );

        assertEquals(2, products.size());
        assertEquals("우리아이 육아 적금", products.get(0).getProductName());
        assertEquals("청년 부모 출산 우대 적금", products.get(1).getProductName());
    }

    private LifecycleProductDto product(Long id, String name) {
        return LifecycleProductDto.builder()
                .productId(id)
                .productType("POLICY_LOAN")
                .productName(name)
                .build();
    }
}
