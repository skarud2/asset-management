package com.via.shinvia.marketdata;

import java.time.LocalDate;
import java.util.List;

// 수익률곡선 데이터 소스 인터페이스.
// 지금은 DbBondYieldCurveProvider(수동 입력 DB 테이블)만 구현체로 존재하고,
// KOFIA 채권정보센터 오픈API 승인 후에는 이 인터페이스의 새 구현체(예: KofiaBondYieldCurveProvider)로
// 교체하면 된다 (ForwardRateCurveService 등 사용하는 쪽은 인터페이스로만 의존하므로 무변경).
public interface BondYieldCurveProvider {

    List<YieldCurvePoint> getYieldCurve(LocalDate asOfDate);
}
