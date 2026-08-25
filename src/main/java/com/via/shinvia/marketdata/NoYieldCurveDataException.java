package com.via.shinvia.marketdata;

import java.time.LocalDate;

public class NoYieldCurveDataException extends RuntimeException {

    public NoYieldCurveDataException(LocalDate asOfDate) {
        super("기준일(" + asOfDate + ")에 해당하는 수익률곡선 데이터가 없어요");
    }
}
