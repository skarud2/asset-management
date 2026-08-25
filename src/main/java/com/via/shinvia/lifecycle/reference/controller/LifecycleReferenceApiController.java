package com.via.shinvia.lifecycle.reference.controller;

import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.reference.service.LifecycleReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lifecycle/references")
@RequiredArgsConstructor
public class LifecycleReferenceApiController {

    private static final String[] VEHICLE_MODELS = {
            "RAY", "K3", "AVANTE", "G70", "G80", "G90", "GV60", "GV70", "GV80"
    };
    private static final String[] VEHICLE_CONDITIONS = {"NEW", "USED"};

    private final LifecycleReferenceService referenceService;

    @GetMapping("/vehicle-prices")
    public Map<String, Map<String, BigDecimal>> vehiclePrices() {
        Map<String, Map<String, BigDecimal>> prices = new LinkedHashMap<>();
        for (String model : VEHICLE_MODELS) {
            Map<String, BigDecimal> conditionPrices = new LinkedHashMap<>();
            for (String condition : VEHICLE_CONDITIONS) {
                conditionPrices.put(condition, referenceService.getNationalAmount(
                        LifecycleEventType.VEHICLE_PURCHASE,
                        "VEHICLE_MODEL_PRICE_" + model + "_" + condition,
                        null
                ));
            }
            prices.put(model, conditionPrices);
        }
        return prices;
    }
}
