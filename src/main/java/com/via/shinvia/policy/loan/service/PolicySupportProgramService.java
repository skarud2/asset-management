package com.via.shinvia.policy.loan.service;

import com.via.shinvia.policy.loan.dto.PolicySupportProgramDTO;
import com.via.shinvia.policy.loan.entity.PolicySupportProgram;
import com.via.shinvia.policy.loan.repository.PolicySupportProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 맞춤대출 상품 검색 및 상세조회 기능
public class PolicySupportProgramService {

    private final PolicySupportProgramRepository repository;

    /**
     * 정책상품 목록 및 상세검색
     */
    public Page<PolicySupportProgramDTO> findPrograms(
            String keyword,
            String target,
            String usage,
            String amount,
            String ageGroup,
            String region,
            Pageable pageable
    ) {
        String normalizedKeyword =
                normalize(keyword);

        String normalizedTarget =
                normalize(target);

        String normalizedUsage =
                normalize(usage);

        String normalizedAmount =
                normalize(amount);

        String normalizedAgeGroup =
                normalize(ageGroup);

        String normalizedRegion =
                normalize(region);

        var programs = repository.searchPrograms(
                normalizedKeyword,
                normalizedTarget,
                normalizedUsage,
                normalizedAmount,
                normalizedAgeGroup,
                normalizedRegion,
                pageable.getOffset(),
                pageable.getPageSize()
        );

        long total = repository.countPrograms(
                normalizedKeyword,
                normalizedTarget,
                normalizedUsage,
                normalizedAmount,
                normalizedAgeGroup,
                normalizedRegion
        );

        Page<PolicySupportProgram> programPage =
                new PageImpl<>(programs, pageable, total);

        return programPage.map(
                PolicySupportProgramDTO::from
        );
    }

    /**
     * 모달 상세조회
     */
    public PolicySupportProgramDTO findById(
            Long programId
    ) {
        PolicySupportProgram entity =
                repository.findById(programId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "정책상품을 찾을 수 없습니다. programId="
                                                + programId
                                )
                        );

        if (!Boolean.TRUE.equals(entity.getActive())) {
            throw new IllegalArgumentException(
                    "현재 제공되지 않는 정책상품입니다. programId="
                            + programId
            );
        }

        return PolicySupportProgramDTO.from(entity);
    }

    private String normalize(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("\\s+", " ");
    }
}
