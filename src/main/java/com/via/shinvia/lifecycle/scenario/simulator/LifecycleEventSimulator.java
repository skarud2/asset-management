package com.via.shinvia.lifecycle.scenario.simulator;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;

/**
 * 생애주기 이벤트 시뮬레이터 인터페이스 (7대 시뮬레이터 공통 규격)
 */
public interface LifecycleEventSimulator {

    /**
     * 시뮬레이터가 처리하는 생애주기 이벤트 유형 반환
     */
    LifecycleEventType getEventType();

    /**
     * 이전 금융 상태(beforeState)와 이벤트 입력값(input)을 기반으로 시뮬레이션을 실행하고 결과 반환
     * (입력값/기준값이 없는 경우 0 또는 빈 문자열을 기본으로 안전하게 처리)
     */
    LifecycleEventResult simulate(LifecycleFinancialStateDto beforeState, LifecycleEventInput input);
}