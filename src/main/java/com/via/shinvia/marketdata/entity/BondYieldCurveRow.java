package com.via.shinvia.marketdata.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// bond_yield_curve 조회 결과 매핑 전용 (MyBatis resultType)
@Getter
@Setter
@NoArgsConstructor
public class BondYieldCurveRow {

    private Integer tenorMonths;
    private BigDecimal yieldRate;
}
