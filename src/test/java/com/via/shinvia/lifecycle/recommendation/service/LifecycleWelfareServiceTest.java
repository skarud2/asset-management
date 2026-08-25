package com.via.shinvia.lifecycle.recommendation.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleSupportDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.policy.welfare.entity.WelfareSupportProduct;
import com.via.shinvia.policy.welfare.repository.WelfareSupportProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecycleWelfareServiceTest {

    @Mock
    private WelfareSupportProductRepository repository;

    @InjectMocks
    private LifecycleWelfareService service;

    @Test
    void excludesLocalProgramsWhenRegionIsUnknown() {
        WelfareSupportProduct national = product(
                1L, "전국 결혼축하금 지원", "BOKJIRO_NATIONAL", null, null, false
        );
        WelfareSupportProduct local = product(
                2L, "전라남도 결혼축하금", "BOKJIRO_LOCAL", "전라남도", "순천시", false
        );
        when(repository.findLifecycleCandidates(
                List.of("결혼", "신혼", "혼인", "예비부부"), null, null
        )).thenReturn(List.of(local, national));

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.MARRIAGE, null, null
        );

        assertEquals(1, result.size());
        assertEquals("전국 결혼축하금 지원", result.get(0).getSupportName());
    }

    @Test
    void excludesMarriageImmigrationAndEmploymentServices() {
        WelfareSupportProduct marriageGrant = product(
                1L, "청년부부 결혼축하금", "BOKJIRO_NATIONAL", null, null, false
        );
        WelfareSupportProduct interpretation = product(
                2L, "결혼이민자 통번역 서비스", "BOKJIRO_NATIONAL", null, null, false
        );
        WelfareSupportProduct employment = product(
                3L, "결혼이민자 일자리 지원", "BOKJIRO_NATIONAL", null, null, false
        );
        when(repository.findLifecycleCandidates(
                List.of("결혼", "신혼", "혼인", "예비부부"), null, null
        )).thenReturn(List.of(interpretation, employment, marriageGrant));

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.MARRIAGE, null, null
        );

        assertEquals(1, result.size());
        assertEquals("청년부부 결혼축하금", result.get(0).getSupportName());
    }

    @Test
    void homePurchaseKeepsPurchaseSupportAndExcludesCrisisHousing() {
        WelfareSupportProduct didimdol = product(
                1L, "내집마련 디딤돌 대출", "BOKJIRO_NATIONAL", null, null, false
        );
        WelfareSupportProduct singleParent = product(
                2L, "한부모가족 공동생활가정형 주거지원", "BOKJIRO_NATIONAL", null, null, false
        );
        WelfareSupportProduct crisis = product(
                3L, "긴급복지 주거지원", "BOKJIRO_NATIONAL", null, null, false
        );
        WelfareSupportProduct victim = product(
                4L, "폭력피해자 주거지원 사업", "BOKJIRO_NATIONAL", null, null, false
        );
        when(repository.findLifecycleCandidates(
                List.of("주택구입", "주택구매", "내집마련", "주택자금", "보금자리", "주거지원"),
                "서울특별시",
                "강남구"
        )).thenReturn(List.of(singleParent, crisis, victim, didimdol));

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.HOME_PURCHASE,
                "서울특별시",
                "강남구"
        );

        assertEquals(1, result.size());
        assertEquals("내집마련 디딤돌 대출", result.get(0).getSupportName());
    }

    @Test
    void ranksCandidatesBeforeApplyingFinalLimit() {
        List<WelfareSupportProduct> candidates = List.of(
                product(1L, "출산 관련 안내", "KINFA", null, null, false),
                product(2L, "전국 출산 지원", "BOKJIRO_NATIONAL", null, null, false),
                product(3L, "서울 출산 지원", "BOKJIRO_LOCAL", "서울특별시", null, false),
                product(4L, "강남구 출산 지원", "BOKJIRO_LOCAL", "서울특별시", "강남구", false),
                product(5L, "강남구 출산 신청 지원", "BOKJIRO_LOCAL", "서울특별시", "강남구", true),
                product(6L, "출산 축하금", "KINFA", null, null, false)
        );
        when(repository.findLifecycleCandidates(childbirthKeywords(), "서울특별시", "강남구"))
                .thenReturn(candidates);

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.CHILDBIRTH,
                "서울특별시",
                "강남구"
        );

        assertEquals(5, result.size());
        assertEquals("강남구 출산 신청 지원", result.get(0).getSupportName());
        assertEquals("강남구 출산 지원", result.get(1).getSupportName());
        assertEquals("서울 출산 지원", result.get(2).getSupportName());
        assertFalse(result.stream().anyMatch(item -> "출산 관련 안내".equals(item.getSupportName())));
        verify(repository).findLifecycleCandidates(childbirthKeywords(), "서울특별시", "강남구");
    }

    @Test
    void prefersBokjiroForNationalCrossSourceDuplicate() {
        WelfareSupportProduct kinfa = product(
                1L, "공공산림가꾸기", "KINFA", null, null, false
        );
        WelfareSupportProduct bokjiro = product(
                2L, "공공 산림가꾸기", "BOKJIRO_NATIONAL", null, null, false
        );
        when(repository.findLifecycleCandidates(childbirthKeywords(), "서울특별시", "강남구"))
                .thenReturn(List.of(kinfa, bokjiro));

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.CHILDBIRTH,
                "서울특별시",
                "강남구"
        );

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getWelfareSupportProductId());
    }

    @Test
    void keepsLocalAndKinfaProductsWhenOnlyGenericNameMatches() {
        WelfareSupportProduct kinfa = product(
                1L, "공공근로사업", "KINFA", null, null, false
        );
        kinfa.setInstitutionName("서민금융진흥원");
        WelfareSupportProduct local = product(
                2L, "공공근로사업", "BOKJIRO_LOCAL", "서울특별시", "강남구", false
        );
        local.setInstitutionName("강남구청");
        when(repository.findLifecycleCandidates(childbirthKeywords(), "서울특별시", "강남구"))
                .thenReturn(List.of(kinfa, local));

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.CHILDBIRTH,
                "서울특별시",
                "강남구"
        );

        assertEquals(2, result.size());
    }

    @Test
    void supportsVehiclePurchaseWithEventSpecificKeywords() {
        WelfareSupportProduct vehicle = product(
                1L, "친환경 자동차 구입비 지원", "BOKJIRO_NATIONAL", null, null, false
        );
        List<String> keywords = List.of("자동차", "차량구입", "차량구매", "구입비", "교통");
        when(repository.findLifecycleCandidates(keywords, "서울특별시", "강남구"))
                .thenReturn(List.of(vehicle));

        List<LifecycleSupportDto> result = service.getSupports(
                LifecycleEventType.VEHICLE_PURCHASE,
                "서울특별시",
                "강남구"
        );

        assertEquals(1, result.size());
        assertEquals("친환경 자동차 구입비 지원", result.get(0).getSupportName());
    }

    private List<String> childbirthKeywords() {
        return List.of("출산", "출생", "산모", "임산부", "신생아", "육아", "양육", "난임");
    }

    private WelfareSupportProduct product(
            Long id,
            String name,
            String sourceType,
            String sido,
            String sigungu,
            boolean complete
    ) {
        return WelfareSupportProduct.builder()
                .welfareSupportProductId(id)
                .productName(name)
                .sourceType(sourceType)
                .regionSido(sido)
                .regionSigungu(sigungu)
                .supportContent("출산 가정 지원")
                .applicationMethod(complete ? "온라인 신청" : null)
                .relatedUrl(complete ? "https://example.com/" + id : null)
                .build();
    }
}
