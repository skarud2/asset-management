package com.via.shinvia.lifecycle.scenario.mapper;

import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioRecord;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResultDto;
import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioResultRecord;

@Mapper
public interface LifecycleScenarioMapper {

    Long findActiveScenarioIdByUserId(@Param("userId") Long userId);

    int insertScenario(@Param("scenario") LifecycleScenarioRecord scenario);

    List<LifecycleScenarioResponse> findScenariosByUserId(
            @Param("userId") Long userId
    );

    LifecycleScenarioResponse findScenarioByIdAndUserId(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );

    int updateScenario(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId,
            @Param("scenarioName") String scenarioName,
            @Param("description") String description,
            @Param("status") String status
    );

    int archiveScenario(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );

    int deleteSimulationResultByScenarioIdAndUserId(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );

    int clearSimulationResult(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );

    int updateSimulationResult(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId,
            @Param("resultJson") String resultJson
    );

    String findSimulationResult(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );

    int ensureSimulationResultTable();

    int countSimulationResultColumn(@Param("columnName") String columnName);

    int addOrderedExpenseAmountsColumn();

    int addOrderedEventCostsColumn();

    int addOneTimeCostBreakdownColumn();

    int addMonthlyExpenseBreakdownColumn();

    int addDetailedAnalysisColumn();

    int backfillSimulationResults(@Param("userId") Long userId);

    int insertSimulationResultHistory(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId,
            @Param("result") LifecycleScenarioResultDto result,
            @Param("orderedExpenseAmountsJson") String orderedExpenseAmountsJson,
            @Param("orderedEventCostsJson") String orderedEventCostsJson,
            @Param("oneTimeCostBreakdownJson") String oneTimeCostBreakdownJson,
            @Param("monthlyExpenseBreakdownJson") String monthlyExpenseBreakdownJson,
            @Param("detailedAnalysisJson") String detailedAnalysisJson
    );

    LifecycleScenarioResultRecord findLatestSimulationResultRecord(@Param("userId") Long userId);

    LifecycleScenarioResultRecord findSimulationResultRecordByScenarioId(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );

    LifecycleScenarioResultRecord findSimulationResultRecordById(
            @Param("resultId") Long resultId,
            @Param("userId") Long userId
    );

    List<LifecycleScenarioResultRecord> findSimulationResultRecordsByUserId(@Param("userId") Long userId);

    int markScenarioCompleted(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );
}
