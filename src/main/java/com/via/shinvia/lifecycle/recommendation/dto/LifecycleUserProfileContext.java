package com.via.shinvia.lifecycle.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleUserProfileContext {
    private Long userId;
    private String loginEmail;
    private String userName;
    private LocalDate birthDate;
    private Integer userAge;
    private BigDecimal annualIncome;
    private BigDecimal liquidAssetAmount;
    private BigDecimal realEstateAsset;
    private Boolean isHomeOwner;
    private String regionSido;
    private String regionSigungu;
    private Integer creditScore;
    private String employmentStatus;
}
