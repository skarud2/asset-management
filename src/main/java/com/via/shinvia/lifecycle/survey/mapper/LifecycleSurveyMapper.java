package com.via.shinvia.lifecycle.survey.mapper;

import com.via.shinvia.lifecycle.survey.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LifecycleSurveyMapper {

    /*
     * =========================================================
     * 1. 기본 생활정보
     * =========================================================
     */

    // 회원의 기본 생활정보 조회
    LifecycleBaseSurveyResponse findBaseSurveyByUserId(
            @Param("userId") Long userId
    );

    // 기본 생활정보 최초 저장
    int insertBaseSurvey(
            @Param("userId") Long userId,
            @Param("request") LifecycleBaseSurveyRequest request
    );

    // 기본 생활정보 수정
    int updateBaseSurvey(
            @Param("userId") Long userId,
            @Param("request") LifecycleBaseSurveyRequest request
    );


    /*
     * =========================================================
     * 2. 결혼
     * =========================================================
     */

    // 결혼 이벤트 저장
    int insertMarriageEvent(
            @Param("scenarioId") Long scenarioId,
            @Param("eventOrder") Integer eventOrder,
            @Param("request") MarriageSurveyRequest request,
            @Param("surveyData") String surveyData
    );

    // 결혼 이벤트 조회
    MarriageSurveyResponse findMarriageEventById(
            @Param("lifecycleEventId") Long lifecycleEventId
    );

    // 결혼 이벤트 수정
    int updateMarriageEvent(
            @Param("lifecycleEventId") Long lifecycleEventId,
            @Param("request") MarriageSurveyRequest request,
            @Param("surveyData") String surveyData
    );


    /*
     * =========================================================
     * 3. 출산
     * =========================================================
     */

    // 출산 이벤트 저장
    int insertChildbirthEvent(
            @Param("scenarioId") Long scenarioId,
            @Param("eventOrder") Integer eventOrder,
            @Param("request") ChildbirthSurveyRequest request,
            @Param("surveyData") String surveyData
    );

    // 출산 이벤트 조회
    ChildbirthSurveyResponse findChildbirthEventById(
            @Param("lifecycleEventId") Long lifecycleEventId
    );

    // 출산 이벤트 수정
    int updateChildbirthEvent(
            @Param("lifecycleEventId") Long lifecycleEventId,
            @Param("request") ChildbirthSurveyRequest request,
            @Param("surveyData") String surveyData
    );


    /*
     * =========================================================
     * 4. 차량구매
     * =========================================================
     */

    // 차량구매 이벤트 저장
    int insertVehicleEvent(
            @Param("scenarioId") Long scenarioId,
            @Param("eventOrder") Integer eventOrder,
            @Param("request") VehicleSurveyRequest request,
            @Param("surveyData") String surveyData
    );

    // 차량구매 이벤트 조회
    VehicleSurveyResponse findVehicleEventById(
            @Param("lifecycleEventId") Long lifecycleEventId
    );

    // 차량구매 이벤트 수정
    int updateVehicleEvent(
            @Param("lifecycleEventId") Long lifecycleEventId,
            @Param("request") VehicleSurveyRequest request,
            @Param("surveyData") String surveyData
    );


    /*
     * =========================================================
     * 5. 월세
     * =========================================================
     */

    // 월세 이벤트 저장
    int insertMonthlyRentEvent(
            @Param("scenarioId") Long scenarioId,
            @Param("eventOrder") Integer eventOrder,
            @Param("request") MonthlyRentSurveyRequest request,
            @Param("surveyData") String surveyData
    );

    // 월세 이벤트 조회
    MonthlyRentSurveyResponse findMonthlyRentEventById(
            @Param("lifecycleEventId") Long lifecycleEventId
    );

    // 월세 이벤트 수정
    int updateMonthlyRentEvent(
            @Param("lifecycleEventId") Long lifecycleEventId,
            @Param("request") MonthlyRentSurveyRequest request,
            @Param("surveyData") String surveyData
    );


    /*
     * =========================================================
     * 6. 전세
     * =========================================================
     */

    // 전세 이벤트 저장
    int insertJeonseEvent(
            @Param("scenarioId") Long scenarioId,
            @Param("eventOrder") Integer eventOrder,
            @Param("request") JeonseSurveyRequest request,
            @Param("surveyData") String surveyData
    );

    // 전세 이벤트 조회
    JeonseSurveyResponse findJeonseEventById(
            @Param("lifecycleEventId") Long lifecycleEventId
    );

    // 전세 이벤트 수정
    int updateJeonseEvent(
            @Param("lifecycleEventId") Long lifecycleEventId,
            @Param("request") JeonseSurveyRequest request,
            @Param("surveyData") String surveyData
    );


    /*
     * =========================================================
     * 7. 주택구매
     * =========================================================
     */

    // 주택구매 이벤트 저장
    int insertHomePurchaseEvent(
            @Param("scenarioId") Long scenarioId,
            @Param("eventOrder") Integer eventOrder,
            @Param("request") HomePurchaseSurveyRequest request,
            @Param("surveyData") String surveyData
    );

    // 주택구매 이벤트 조회
    HomePurchaseSurveyResponse findHomePurchaseEventById(
            @Param("lifecycleEventId") Long lifecycleEventId
    );

    // 주택구매 이벤트 수정
    int updateHomePurchaseEvent(
            @Param("lifecycleEventId") Long lifecycleEventId,
            @Param("request") HomePurchaseSurveyRequest request,
            @Param("surveyData") String surveyData
    );


    /*
     * =========================================================
     * 8. 대출상환
     * =========================================================
     */

    // 대출상환 이벤트 저장
    int insertRepaymentEvent(
            @Param("scenarioId") Long scenarioId,
            @Param("eventOrder") Integer eventOrder,
            @Param("request") RepaymentSurveyRequest request,
            @Param("surveyData") String surveyData
    );

    // 대출상환 이벤트 조회
    RepaymentSurveyResponse findRepaymentEventById(
            @Param("lifecycleEventId") Long lifecycleEventId
    );

    // 대출상환 이벤트 수정
    int updateRepaymentEvent(
            @Param("lifecycleEventId") Long lifecycleEventId,
            @Param("request") RepaymentSurveyRequest request,
            @Param("surveyData") String surveyData
    );


    /*
     * =========================================================
     * 9. 이벤트 공통
     * =========================================================
     */

    // 특정 시나리오의 이벤트 ID 목록 조회
    List<Long> findEventIdsByScenarioId(
            @Param("scenarioId") Long scenarioId
    );

    List<LifecycleTimelineEventResponse> findTimelineEventsByScenarioId(
            @Param("scenarioId") Long scenarioId
    );

    // 이벤트 삭제
    int deleteEvent(
            @Param("lifecycleEventId") Long lifecycleEventId
    );

    // 다음 event_order 계산
    Integer findNextEventOrder(
            @Param("scenarioId") Long scenarioId
    );

    Long findLatestEventId(
            @Param("scenarioId") Long scenarioId,
            @Param("eventType") String eventType
    );
}
