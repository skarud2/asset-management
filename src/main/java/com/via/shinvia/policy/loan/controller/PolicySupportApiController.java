package com.via.shinvia.policy.loan.controller;

import com.via.shinvia.policy.loan.dto.PolicySupportProgramDTO;
import com.via.shinvia.policy.loan.service.PolicySupportProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/policy-support")
// 맞춤대출 상세 API 제공 기능
public class PolicySupportApiController {

    private final PolicySupportProgramService
            policySupportProgramService;

    /**
     * 정책지원 상품 상세정보 JSON 조회
     */
    @GetMapping("/{programId}")
    public ResponseEntity<PolicySupportProgramDTO> getProgramDetail(
            @PathVariable Long programId
    ) {
        try {
            return ResponseEntity.ok(
                    policySupportProgramService.findById(programId)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
