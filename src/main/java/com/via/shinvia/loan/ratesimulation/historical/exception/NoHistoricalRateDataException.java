package com.via.shinvia.loan.ratesimulation.historical.exception;

import java.time.LocalDate;

public class NoHistoricalRateDataException extends RuntimeException {

    public NoHistoricalRateDataException(LocalDate startDate, LocalDate endDate) {
        super("해당 기간(" + startDate + " ~ " + endDate + ")에 조회되는 한국은행 기준금리 데이터가 없어요");
    }
}
