package com.via.shinvia.policy.loan.service;

import com.via.shinvia.policy.loan.dto.api.LoanProductApiItem;
import com.via.shinvia.policy.loan.entity.PolicySupportProgram;
import com.via.shinvia.policy.loan.converter.PolicySupportProgramConverter;
import com.via.shinvia.policy.loan.repository.PolicySupportProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
// 맞춤대출 상품 저장 및 갱신 기능
public class PolicySupportSaveService {

    private final PolicySupportProgramConverter converter;

    private final PolicySupportProgramRepository repository;

    @Transactional
    public int saveAll(
            List<LoanProductApiItem> apiItems
    ) {
        int processedCount = 0;
        int insertCount = 0;
        int updateCount = 0;

        for (LoanProductApiItem item : apiItems) {

            if (item.getSeq() == null
                    || item.getSeq().isBlank()) {
                continue;
            }

            PolicySupportProgram entity =
                    repository
                            .findByExternalSeq(
                                    item.getSeq()
                            )
                            .orElse(null);

            if (entity == null) {

                repository.insert(
                        converter.toEntity(item)
                );

                insertCount++;

            } else {

                converter.updateEntity(
                        entity,
                        item
                );

                repository.update(entity);

                updateCount++;
            }

            processedCount++;
        }

        log.info(
                "정책상품 저장 완료 - 전체={}, 신규={}, 수정={}",
                processedCount,
                insertCount,
                updateCount
        );

        return processedCount;
    }
}
