package com.via.shinvia.lifecycle.survey.service;



import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.via.shinvia.lifecycle.survey.dto.*;
import com.via.shinvia.lifecycle.survey.mapper.LifecycleSurveyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LifecycleSurveyService {

    private final LifecycleSurveyMapper lifecycleSurveyMapper;

    // Java 객체를 JSON 문자열로 변환하기 위해 사용
    private final JsonMapper jsonMapper;


    /*
     * =========================================================
     * 1. 기본 생활정보
     * =========================================================
     */

    /**
     * 기본 생활정보 저장
     *
     * 기존 데이터가 없으면 INSERT,
     * 이미 있으면 UPDATE 한다.
     */
    @Transactional
    public void saveBaseSurvey(
            Long userId,
            LifecycleBaseSurveyRequest request
    ) {

        // 기존 기본설문 조회
        LifecycleBaseSurveyResponse existing =
                lifecycleSurveyMapper.findBaseSurveyByUserId(userId);

        if (existing == null) {

            // 처음 설문을 작성하는 경우
            lifecycleSurveyMapper.insertBaseSurvey(userId, request);

        } else {

            // 기존 설문을 다시 작성한 경우
            lifecycleSurveyMapper.updateBaseSurvey(userId, request);
        }
    }


    /**
     * 기본 생활정보 조회
     */
    @Transactional(readOnly = true)
    public LifecycleBaseSurveyResponse getBaseSurvey(Long userId) {

        return lifecycleSurveyMapper.findBaseSurveyByUserId(userId);
    }



    /*
     * =========================================================
     * 2. 결혼
     * =========================================================
     */

    /**
     * 결혼 이벤트 저장
     */
    @Transactional
    public Long saveMarriageSurvey(
            Long scenarioId,
            MarriageSurveyRequest request
    ) {

        // 시나리오에서 다음 이벤트 순서 계산
        Integer eventOrder =
                lifecycleSurveyMapper.findNextEventOrder(scenarioId);

        // Request DTO를 JSON으로 변환
        String surveyData = toJson(request);

        lifecycleSurveyMapper.insertMarriageEvent(
                scenarioId,
                eventOrder,
                request,
                surveyData
        );
        return lifecycleSurveyMapper.findLatestEventId(scenarioId, "MARRIAGE");
    }


    /**
     * 결혼 이벤트 조회
     */
    @Transactional(readOnly = true)
    public MarriageSurveyResponse getMarriageSurvey(Long lifecycleEventId) {

        return lifecycleSurveyMapper.findMarriageEventById(
                lifecycleEventId
        );
    }


    /**
     * 결혼 이벤트 수정
     */
    @Transactional
    public void updateMarriageSurvey(
            Long lifecycleEventId,
            MarriageSurveyRequest request
    ) {

        String surveyData = toJson(request);

        lifecycleSurveyMapper.updateMarriageEvent(
                lifecycleEventId,
                request,
                surveyData
        );
    }



    /*
     * =========================================================
     * 3. 출산
     * =========================================================
     */

    @Transactional
    public Long saveChildbirthSurvey(
            Long scenarioId,
            ChildbirthSurveyRequest request
    ) {

        Integer eventOrder =
                lifecycleSurveyMapper.findNextEventOrder(scenarioId);

        String surveyData = toJson(request);

        lifecycleSurveyMapper.insertChildbirthEvent(
                scenarioId,
                eventOrder,
                request,
                surveyData
        );
        return request.getLifecycleEventId();
    }


    @Transactional(readOnly = true)
    public ChildbirthSurveyResponse getChildbirthSurvey(
            Long lifecycleEventId
    ) {

        return lifecycleSurveyMapper.findChildbirthEventById(
                lifecycleEventId
        );
    }


    @Transactional
    public void updateChildbirthSurvey(
            Long lifecycleEventId,
            ChildbirthSurveyRequest request
    ) {

        String surveyData = toJson(request);

        lifecycleSurveyMapper.updateChildbirthEvent(
                lifecycleEventId,
                request,
                surveyData
        );
    }



    /*
     * =========================================================
     * 4. 차량구매
     * =========================================================
     */

    @Transactional
    public Long saveVehicleSurvey(
            Long scenarioId,
            VehicleSurveyRequest request
    ) {

        Integer eventOrder =
                lifecycleSurveyMapper.findNextEventOrder(scenarioId);

        String surveyData = toJson(request);

        lifecycleSurveyMapper.insertVehicleEvent(
                scenarioId,
                eventOrder,
                request,
                surveyData
        );
        return request.getLifecycleEventId();
    }


    @Transactional(readOnly = true)
    public VehicleSurveyResponse getVehicleSurvey(
            Long lifecycleEventId
    ) {

        return lifecycleSurveyMapper.findVehicleEventById(
                lifecycleEventId
        );
    }


    @Transactional
    public void updateVehicleSurvey(
            Long lifecycleEventId,
            VehicleSurveyRequest request
    ) {

        String surveyData = toJson(request);

        lifecycleSurveyMapper.updateVehicleEvent(
                lifecycleEventId,
                request,
                surveyData
        );
    }



    /*
     * =========================================================
     * 5. 월세
     * =========================================================
     */

    @Transactional
    public Long saveMonthlyRentSurvey(
            Long scenarioId,
            MonthlyRentSurveyRequest request
    ) {

        Integer eventOrder =
                lifecycleSurveyMapper.findNextEventOrder(scenarioId);

        String surveyData = toJson(request);

        lifecycleSurveyMapper.insertMonthlyRentEvent(
                scenarioId,
                eventOrder,
                request,
                surveyData
        );
        return lifecycleSurveyMapper.findLatestEventId(scenarioId, "MONTHLY_RENT");
    }


    @Transactional(readOnly = true)
    public MonthlyRentSurveyResponse getMonthlyRentSurvey(
            Long lifecycleEventId
    ) {

        return lifecycleSurveyMapper.findMonthlyRentEventById(
                lifecycleEventId
        );
    }


    @Transactional
    public void updateMonthlyRentSurvey(
            Long lifecycleEventId,
            MonthlyRentSurveyRequest request
    ) {

        String surveyData = toJson(request);

        lifecycleSurveyMapper.updateMonthlyRentEvent(
                lifecycleEventId,
                request,
                surveyData
        );
    }



    /*
     * =========================================================
     * 6. 전세
     * =========================================================
     */

    @Transactional
    public Long saveJeonseSurvey(
            Long scenarioId,
            JeonseSurveyRequest request
    ) {

        Integer eventOrder =
                lifecycleSurveyMapper.findNextEventOrder(scenarioId);

        String surveyData = toJson(request);

        lifecycleSurveyMapper.insertJeonseEvent(
                scenarioId,
                eventOrder,
                request,
                surveyData
        );
        return lifecycleSurveyMapper.findLatestEventId(scenarioId, "JEONSE");
    }


    @Transactional(readOnly = true)
    public JeonseSurveyResponse getJeonseSurvey(
            Long lifecycleEventId
    ) {

        return lifecycleSurveyMapper.findJeonseEventById(
                lifecycleEventId
        );
    }


    @Transactional
    public void updateJeonseSurvey(
            Long lifecycleEventId,
            JeonseSurveyRequest request
    ) {

        String surveyData = toJson(request);

        lifecycleSurveyMapper.updateJeonseEvent(
                lifecycleEventId,
                request,
                surveyData
        );
    }



    /*
     * =========================================================
     * 7. 주택구매
     * =========================================================
     */

    @Transactional
    public Long saveHomePurchaseSurvey(
            Long scenarioId,
            HomePurchaseSurveyRequest request
    ) {

        Integer eventOrder =
                lifecycleSurveyMapper.findNextEventOrder(scenarioId);

        String surveyData = toJson(request);

        lifecycleSurveyMapper.insertHomePurchaseEvent(
                scenarioId,
                eventOrder,
                request,
                surveyData
        );
        return lifecycleSurveyMapper.findLatestEventId(scenarioId, "HOME_PURCHASE");
    }


    @Transactional(readOnly = true)
    public HomePurchaseSurveyResponse getHomePurchaseSurvey(
            Long lifecycleEventId
    ) {

        return lifecycleSurveyMapper.findHomePurchaseEventById(
                lifecycleEventId
        );
    }


    @Transactional
    public void updateHomePurchaseSurvey(
            Long lifecycleEventId,
            HomePurchaseSurveyRequest request
    ) {

        String surveyData = toJson(request);

        lifecycleSurveyMapper.updateHomePurchaseEvent(
                lifecycleEventId,
                request,
                surveyData
        );
    }



    /*
     * =========================================================
     * 8. 대출상환
     * =========================================================
     */

    @Transactional
    public Long saveRepaymentSurvey(
            Long scenarioId,
            RepaymentSurveyRequest request
    ) {

        Integer eventOrder =
                lifecycleSurveyMapper.findNextEventOrder(scenarioId);

        String surveyData = toJson(request);

        lifecycleSurveyMapper.insertRepaymentEvent(
                scenarioId,
                eventOrder,
                request,
                surveyData
        );
        return lifecycleSurveyMapper.findLatestEventId(scenarioId, "REPAYMENT");
    }


    @Transactional(readOnly = true)
    public RepaymentSurveyResponse getRepaymentSurvey(
            Long lifecycleEventId
    ) {

        return lifecycleSurveyMapper.findRepaymentEventById(
                lifecycleEventId
        );
    }


    @Transactional
    public void updateRepaymentSurvey(
            Long lifecycleEventId,
            RepaymentSurveyRequest request
    ) {

        String surveyData = toJson(request);

        lifecycleSurveyMapper.updateRepaymentEvent(
                lifecycleEventId,
                request,
                surveyData
        );
    }



    /*
     * =========================================================
     * 9. 이벤트 공통
     * =========================================================
     */

    /**
     * 특정 시나리오에 포함된 이벤트 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getEventIds(Long scenarioId) {

        return lifecycleSurveyMapper.findEventIdsByScenarioId(
                scenarioId
        );
    }

    @Transactional(readOnly = true)
    public List<LifecycleTimelineEventResponse> getTimelineEvents(
            Long scenarioId
    ) {
        return lifecycleSurveyMapper.findTimelineEventsByScenarioId(
                scenarioId
        );
    }


    /**
     * 이벤트 삭제
     */
    @Transactional
    public void deleteEvent(Long lifecycleEventId) {

        lifecycleSurveyMapper.deleteEvent(lifecycleEventId);
    }



    /*
     * =========================================================
     * JSON 변환 공통 메서드
     * =========================================================
     */

    /**
     * Request DTO를 JSON 문자열로 변환한다.
     *
     * lifecycle_event.survey_data JSON 컬럼에 저장하기 위해 사용.
     */
    private String toJson(Object request) {

        try {
            return jsonMapper.writeValueAsString(request);

        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "생애주기 설문 데이터를 JSON으로 변환할 수 없습니다.",
                    e
            );
        }
    }
}
