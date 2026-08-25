package com.via.shinvia.lifecycle.scenario.event;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;

/**
 * 7대 생애주기 이벤트 계산기 공통 인터페이스
 */
public interface LifecycleEventCalculator {

    /**
     * 지원하는 이벤트 타입 (MARRIAGE, CHILDBIRTH, VEHICLE_PURCHASE 등)
     */
    LifecycleEventType getEventType();

    /**
     * 이벤트 발생에 따른 금융 상태 전이 및 결과 계산
     *
     * @param beforeState 이벤트 직전 금융 상태
     * @param input       B 파트에서 전달된 이벤트 입력 데이터 (비용, 지원금 등)
     * @return 이벤트 계산 결과 (직후 상태, 발생 비용, 지원 혜택, 부족 자금, 요약 문구 등)
     */
    LifecycleEventResult calculate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input);
}
