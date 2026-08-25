package com.via.shinvia.lifecycle.survey.controller;

import com.via.shinvia.lifecycle.survey.dto.*;
import com.via.shinvia.lifecycle.survey.service.LifecycleSurveyService;
import com.via.shinvia.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lifecycle/survey")
public class LifecycleSurveyApiController {

    private final LifecycleSurveyService lifecycleSurveyService;
    private final CurrentUser currentUser;


    /*
     * =========================================================
     * 1. 기본 생활정보
     * =========================================================
     */

    /**
     * 기본 생활정보 저장
     *
     * 최초 저장이면 INSERT,
     * 기존 설문이 있으면 UPDATE
     */
    @PostMapping("/base/{userId}")
    public ResponseEntity<Void> saveBaseSurvey(
            @PathVariable Long userId,
            @RequestBody LifecycleBaseSurveyRequest request
    ) {

        lifecycleSurveyService.saveBaseSurvey(userId, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/base")
    public ResponseEntity<Void> saveMyBaseSurvey(
            Authentication authentication,
            @RequestBody LifecycleBaseSurveyRequest request
    ) {
        Long userId = currentUser.getUserId(authentication);
        lifecycleSurveyService.saveBaseSurvey(userId, request);
        return ResponseEntity.ok().build();
    }


    /**
     * 기본 생활정보 조회
     */
    @GetMapping("/base/{userId}")
    public ResponseEntity<LifecycleBaseSurveyResponse> getBaseSurvey(
            @PathVariable Long userId
    ) {

        LifecycleBaseSurveyResponse response =
                lifecycleSurveyService.getBaseSurvey(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/base")
    public ResponseEntity<LifecycleBaseSurveyResponse> getMyBaseSurvey(
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        LifecycleBaseSurveyResponse response =
                lifecycleSurveyService.getBaseSurvey(userId);

        return response == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(response);
    }



    /*
     * =========================================================
     * 2. 결혼
     * =========================================================
     */

    // 결혼 이벤트 저장
    @PostMapping("/scenario/{scenarioId}/marriage")
    public ResponseEntity<Long> saveMarriageSurvey(
            @PathVariable Long scenarioId,
            @RequestBody MarriageSurveyRequest request
    ) {

        Long eventId = lifecycleSurveyService.saveMarriageSurvey(
                scenarioId,
                request
        );

        return ResponseEntity.ok(eventId);
    }


    // 결혼 이벤트 조회
    @GetMapping("/marriage/{eventId}")
    public ResponseEntity<MarriageSurveyResponse> getMarriageSurvey(
            @PathVariable Long eventId
    ) {

        return ResponseEntity.ok(
                lifecycleSurveyService.getMarriageSurvey(eventId)
        );
    }


    // 결혼 이벤트 수정
    @PutMapping("/marriage/{eventId}")
    public ResponseEntity<Void> updateMarriageSurvey(
            @PathVariable Long eventId,
            @RequestBody MarriageSurveyRequest request
    ) {

        lifecycleSurveyService.updateMarriageSurvey(
                eventId,
                request
        );

        return ResponseEntity.ok().build();
    }



    /*
     * =========================================================
     * 3. 출산
     * =========================================================
     */

    // 출산 이벤트 저장
    @PostMapping("/scenario/{scenarioId}/childbirth")
    public ResponseEntity<Long> saveChildbirthSurvey(
            @PathVariable Long scenarioId,
            @RequestBody ChildbirthSurveyRequest request
    ) {

        Long eventId = lifecycleSurveyService.saveChildbirthSurvey(
                scenarioId,
                request
        );

        return ResponseEntity.ok(eventId);
    }


    // 출산 이벤트 조회
    @GetMapping("/childbirth/{eventId}")
    public ResponseEntity<ChildbirthSurveyResponse> getChildbirthSurvey(
            @PathVariable Long eventId
    ) {

        return ResponseEntity.ok(
                lifecycleSurveyService.getChildbirthSurvey(eventId)
        );
    }


    // 출산 이벤트 수정
    @PutMapping("/childbirth/{eventId}")
    public ResponseEntity<Void> updateChildbirthSurvey(
            @PathVariable Long eventId,
            @RequestBody ChildbirthSurveyRequest request
    ) {

        lifecycleSurveyService.updateChildbirthSurvey(
                eventId,
                request
        );

        return ResponseEntity.ok().build();
    }



    /*
     * =========================================================
     * 4. 차량구매
     * =========================================================
     */

    // 차량구매 이벤트 저장
    @PostMapping("/scenario/{scenarioId}/vehicle")
    public ResponseEntity<Long> saveVehicleSurvey(
            @PathVariable Long scenarioId,
            @RequestBody VehicleSurveyRequest request
    ) {

        Long eventId = lifecycleSurveyService.saveVehicleSurvey(
                scenarioId,
                request
        );

        return ResponseEntity.ok(eventId);
    }


    // 차량구매 이벤트 조회
    @GetMapping("/vehicle/{eventId}")
    public ResponseEntity<VehicleSurveyResponse> getVehicleSurvey(
            @PathVariable Long eventId
    ) {

        return ResponseEntity.ok(
                lifecycleSurveyService.getVehicleSurvey(eventId)
        );
    }


    // 차량구매 이벤트 수정
    @PutMapping("/vehicle/{eventId}")
    public ResponseEntity<Void> updateVehicleSurvey(
            @PathVariable Long eventId,
            @RequestBody VehicleSurveyRequest request
    ) {

        lifecycleSurveyService.updateVehicleSurvey(
                eventId,
                request
        );

        return ResponseEntity.ok().build();
    }



    /*
     * =========================================================
     * 5. 월세
     * =========================================================
     */

    // 월세 이벤트 저장
    @PostMapping("/scenario/{scenarioId}/monthly-rent")
    public ResponseEntity<Long> saveMonthlyRentSurvey(
            @PathVariable Long scenarioId,
            @RequestBody MonthlyRentSurveyRequest request
    ) {

        Long eventId = lifecycleSurveyService.saveMonthlyRentSurvey(
                scenarioId,
                request
        );

        return ResponseEntity.ok(eventId);
    }


    // 월세 이벤트 조회
    @GetMapping("/monthly-rent/{eventId}")
    public ResponseEntity<MonthlyRentSurveyResponse> getMonthlyRentSurvey(
            @PathVariable Long eventId
    ) {

        return ResponseEntity.ok(
                lifecycleSurveyService.getMonthlyRentSurvey(eventId)
        );
    }


    // 월세 이벤트 수정
    @PutMapping("/monthly-rent/{eventId}")
    public ResponseEntity<Void> updateMonthlyRentSurvey(
            @PathVariable Long eventId,
            @RequestBody MonthlyRentSurveyRequest request
    ) {

        lifecycleSurveyService.updateMonthlyRentSurvey(
                eventId,
                request
        );

        return ResponseEntity.ok().build();
    }



    /*
     * =========================================================
     * 6. 전세
     * =========================================================
     */

    // 전세 이벤트 저장
    @PostMapping("/scenario/{scenarioId}/jeonse")
    public ResponseEntity<Long> saveJeonseSurvey(
            @PathVariable Long scenarioId,
            @RequestBody JeonseSurveyRequest request
    ) {

        Long eventId = lifecycleSurveyService.saveJeonseSurvey(
                scenarioId,
                request
        );

        return ResponseEntity.ok(eventId);
    }


    // 전세 이벤트 조회
    @GetMapping("/jeonse/{eventId}")
    public ResponseEntity<JeonseSurveyResponse> getJeonseSurvey(
            @PathVariable Long eventId
    ) {

        return ResponseEntity.ok(
                lifecycleSurveyService.getJeonseSurvey(eventId)
        );
    }


    // 전세 이벤트 수정
    @PutMapping("/jeonse/{eventId}")
    public ResponseEntity<Void> updateJeonseSurvey(
            @PathVariable Long eventId,
            @RequestBody JeonseSurveyRequest request
    ) {

        lifecycleSurveyService.updateJeonseSurvey(
                eventId,
                request
        );

        return ResponseEntity.ok().build();
    }



    /*
     * =========================================================
     * 7. 주택구매
     * =========================================================
     */

    // 주택구매 이벤트 저장
    @PostMapping("/scenario/{scenarioId}/home-purchase")
    public ResponseEntity<Long> saveHomePurchaseSurvey(
            @PathVariable Long scenarioId,
            @RequestBody HomePurchaseSurveyRequest request
    ) {

        Long eventId = lifecycleSurveyService.saveHomePurchaseSurvey(
                scenarioId,
                request
        );

        return ResponseEntity.ok(eventId);
    }


    // 주택구매 이벤트 조회
    @GetMapping("/home-purchase/{eventId}")
    public ResponseEntity<HomePurchaseSurveyResponse> getHomePurchaseSurvey(
            @PathVariable Long eventId
    ) {

        return ResponseEntity.ok(
                lifecycleSurveyService.getHomePurchaseSurvey(eventId)
        );
    }


    // 주택구매 이벤트 수정
    @PutMapping("/home-purchase/{eventId}")
    public ResponseEntity<Void> updateHomePurchaseSurvey(
            @PathVariable Long eventId,
            @RequestBody HomePurchaseSurveyRequest request
    ) {

        lifecycleSurveyService.updateHomePurchaseSurvey(
                eventId,
                request
        );

        return ResponseEntity.ok().build();
    }



    /*
     * =========================================================
     * 8. 대출상환
     * =========================================================
     */

    // 대출상환 이벤트 저장
    @PostMapping("/scenario/{scenarioId}/repayment")
    public ResponseEntity<Long> saveRepaymentSurvey(
            @PathVariable Long scenarioId,
            @RequestBody RepaymentSurveyRequest request
    ) {

        Long eventId = lifecycleSurveyService.saveRepaymentSurvey(
                scenarioId,
                request
        );

        return ResponseEntity.ok(eventId);
    }


    // 대출상환 이벤트 조회
    @GetMapping("/repayment/{eventId}")
    public ResponseEntity<RepaymentSurveyResponse> getRepaymentSurvey(
            @PathVariable Long eventId
    ) {

        return ResponseEntity.ok(
                lifecycleSurveyService.getRepaymentSurvey(eventId)
        );
    }


    // 대출상환 이벤트 수정
    @PutMapping("/repayment/{eventId}")
    public ResponseEntity<Void> updateRepaymentSurvey(
            @PathVariable Long eventId,
            @RequestBody RepaymentSurveyRequest request
    ) {

        lifecycleSurveyService.updateRepaymentSurvey(
                eventId,
                request
        );

        return ResponseEntity.ok().build();
    }



    /*
     * =========================================================
     * 9. 이벤트 공통
     * =========================================================
     */

    // 특정 시나리오에 포함된 이벤트 ID 목록 조회
    @GetMapping("/scenario/{scenarioId}/events")
    public ResponseEntity<List<Long>> getEventIds(
            @PathVariable Long scenarioId
    ) {

        return ResponseEntity.ok(
                lifecycleSurveyService.getEventIds(scenarioId)
        );
    }

    @GetMapping("/scenario/{scenarioId}/timeline")
    public ResponseEntity<List<LifecycleTimelineEventResponse>> getTimeline(
            @PathVariable Long scenarioId
    ) {
        return ResponseEntity.ok(
                lifecycleSurveyService.getTimelineEvents(scenarioId)
        );
    }


    // 이벤트 삭제
    @DeleteMapping("/event/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long eventId
    ) {

        lifecycleSurveyService.deleteEvent(eventId);

        return ResponseEntity.noContent().build();
    }
}
