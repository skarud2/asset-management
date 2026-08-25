package com.via.shinvia.mydata.service;

import com.via.shinvia.mydata.mapper.FinancialInstitutionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialInstitutionService {
    private final FinancialInstitutionMapper financialInstitutionMapper;

    public List<String> getBankOrgCodes() {
        return financialInstitutionMapper.findBankOrgCodes();
    }
}
