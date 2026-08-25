package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.scenario.simulator.LifecycleEventSimulator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 7대 생애주기 이벤트 시뮬레이터(Simulator)를 연쇄 실행(Sequence Execution)하는 서비스
 */
@Service
public class LifecycleEventSequenceService {

    private final LifecycleProjectionService projectionService;
    private final Map<LifecycleEventType, LifecycleEventSimulator> simulatorMap;

    public LifecycleEventSequenceService(
            LifecycleProjectionService projectionService,
            List<LifecycleEventSimulator> simulators
    ) {
        this.projectionService = projectionService;
        this.simulatorMap = simulators.stream()
                .collect(Collectors.toMap(
                        LifecycleEventSimulator::getEventType,
                        Function.identity()
                ));
    }

    public List<LifecycleEventResult> execute(
            LifecycleFinancialStateDto initialState,
            List<LifecycleEventInput> inputs
    ) {
        return execute(initialState, inputs, BigDecimal.ZERO, null);
    }

    public List<LifecycleEventResult> execute(
            LifecycleFinancialStateDto initialState,
            List<LifecycleEventInput> inputs,
            BigDecimal annualSalaryGrowthRate,
            BigDecimal annualInflationRate
    ) {
        if (initialState == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "초기 금융상태가 필요합니다."
            );
        }

        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }

        LifecycleFinancialStateDto currentState = initialState;

        List<LifecycleEventInput> orderedInputs = inputs.stream()
                .filter(input -> input != null && input.getEventType() != null)
                .sorted(eventOrderComparator())
                .toList();

        List<LifecycleEventResult> results = new java.util.ArrayList<>();

        for (LifecycleEventInput input : orderedInputs) {
            LifecycleEventSimulator simulator = findSimulator(input.getEventType());

            LifecycleFinancialStateDto projectedState = projectionService.project(
                    currentState,
                    input.getTargetDate(),
                    annualSalaryGrowthRate,
                    annualInflationRate
            );

            LifecycleEventResult result = simulator.simulate(projectedState, input);

            if (result == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "이벤트 시뮬레이션 결과가 비어있습니다. eventType=" + input.getEventType()
                );
            }

            normalizeResult(result, projectedState, input);

            results.add(result);
            currentState = result.getAfterState();
        }

        return results;
    }

    private LifecycleEventSimulator findSimulator(LifecycleEventType eventType) {
        LifecycleEventSimulator simulator = simulatorMap.get(eventType);

        if (simulator == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 생애주기 이벤트입니다. eventType=" + eventType
            );
        }

        return simulator;
    }

    private Comparator<LifecycleEventInput> eventOrderComparator() {
        return Comparator
                .comparing(this::sortTargetDate)
                .thenComparing(this::sortEventOrder)
                .thenComparing(this::sortLifecycleEventId);
    }

    private Integer sortEventOrder(LifecycleEventInput input) {
        return input.getEventOrder() != null
                ? input.getEventOrder()
                : Integer.MAX_VALUE;
    }

    private LocalDate sortTargetDate(LifecycleEventInput input) {
        return input.getTargetDate() != null
                ? input.getTargetDate()
                : LocalDate.MAX;
    }

    private Long sortLifecycleEventId(LifecycleEventInput input) {
        return input.getLifecycleEventId() != null
                ? input.getLifecycleEventId()
                : Long.MAX_VALUE;
    }

    private void normalizeResult(
            LifecycleEventResult result,
            LifecycleFinancialStateDto projectedState,
            LifecycleEventInput input
    ) {
        if (result.getLifecycleEventId() == null) {
            result.setLifecycleEventId(input.getLifecycleEventId());
        }

        if (result.getEventType() == null) {
            result.setEventType(input.getEventType());
        }

        if (result.getEventDate() == null) {
            result.setEventDate(
                    input.getTargetDate() != null
                            ? input.getTargetDate()
                            : projectedState.getStateDate()
            );
        }

        if (result.getBeforeState() == null) {
            result.setBeforeState(projectedState);
        }

        if (result.getAfterState() == null) {
            result.setAfterState(projectedState);
        }
    }
}