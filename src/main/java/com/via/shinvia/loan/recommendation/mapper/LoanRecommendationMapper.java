package com.via.shinvia.loan.recommendation.mapper;

import com.via.shinvia.loan.recommendation.model.CreditCandidateRow;
import com.via.shinvia.loan.recommendation.model.HousingCandidateRow;
import com.via.shinvia.loan.recommendation.model.LoanRecommendationSaveRow;
import com.via.shinvia.loan.recommendation.model.OptionChoice;
import com.via.shinvia.loan.recommendation.model.UserFinancialProfileView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LoanRecommendationMapper {

    UserFinancialProfileView findProfileByLoginEmail(
            @Param("loginEmail") String loginEmail
    );

    List<OptionChoice> findRateTypeOptions();

    List<OptionChoice> findRepaymentTypeOptions();

    List<OptionChoice> findCollateralTypeOptions();

    List<CreditCandidateRow> findCreditCandidates();

    List<HousingCandidateRow> findHousingCandidates(
            @Param("loanType") String loanType
    );

    int deactivateRecommendations(
            @Param("userFinancialProfileId") Long userFinancialProfileId,
            @Param("loanType") String loanType
    );

    int upsertRecommendation(LoanRecommendationSaveRow row);
}
