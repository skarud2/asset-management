package com.via.shinvia.marketdata;

import com.via.shinvia.marketdata.mapper.BondYieldCurveMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

// 현재 유일한 구현체 — bond_yield_curve 테이블(수동 입력값)에서 조회한다.
// KOFIA 채권정보센터 오픈API 승인 후에는 이 클래스를 그 API를 호출하는 구현체로 교체하면 된다.
@Component
@RequiredArgsConstructor
public class DbBondYieldCurveProvider implements BondYieldCurveProvider {

    private final BondYieldCurveMapper bondYieldCurveMapper;

    @Override
    public List<YieldCurvePoint> getYieldCurve(LocalDate asOfDate) {
        return bondYieldCurveMapper.findByAsOfDate(asOfDate).stream()
                .map(row -> new YieldCurvePoint(row.getTenorMonths(), row.getYieldRate()))
                .toList();
    }
}
