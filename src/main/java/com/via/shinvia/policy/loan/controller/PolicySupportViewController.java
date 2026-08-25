package com.via.shinvia.policy.loan.controller;

import com.via.shinvia.policy.loan.dto.PolicySupportProgramDTO;
import com.via.shinvia.policy.loan.service.PolicySupportProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
// 맞춤대출 목록 화면 제공 기능
public class PolicySupportViewController {

    private final PolicySupportProgramService
            policySupportProgramService;

    @GetMapping("/policy-support")
    public String supportList(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String target,
            @RequestParam(required = false, defaultValue = "") String usage,
            @RequestParam(required = false, defaultValue = "") String amount,
            @RequestParam(required = false, defaultValue = ""
            )
            String ageGroup,

            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String region,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size,

            Model model
    ) {
        if (page < 0) {
            page = 0;
        }

        if (size < 10
                || size > 50
                || size % 10 != 0) {

            size = 20;
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        Page<PolicySupportProgramDTO> programPage =
                policySupportProgramService.findPrograms(
                        keyword,
                        target,
                        usage,
                        amount,
                        ageGroup,
                        region,
                        pageable
                );

        /*
         * 검색 결과 페이지 수보다 큰 page 값이 들어온 경우
         * 첫 페이지로 다시 조회한다.
         */
        if (programPage.getTotalPages() > 0
                && page >= programPage.getTotalPages()) {

            page = 0;

            pageable =
                    PageRequest.of(
                            page,
                            size
                    );

            programPage =
                    policySupportProgramService.findPrograms(
                            keyword,
                            target,
                            usage,
                            amount,
                            ageGroup,
                            region,
                            pageable
                    );
        }

        int currentPage =
                programPage.getNumber();

        int totalPages =
                programPage.getTotalPages();

        int startPage = 0;
        int endPage = 0;

        if (totalPages > 0) {

            startPage = (currentPage / 10) * 10;
            endPage = Math.min(totalPages - 1, startPage + 9);
        }

        boolean filterApplied =
                hasValue(keyword)
                        || hasValue(target)
                        || hasValue(usage)
                        || hasValue(amount)
                        || hasValue(ageGroup)
                        || hasValue(region);

        model.addAttribute(
                "programPage",
                programPage
        );

        model.addAttribute(
                "programs",
                programPage.getContent()
        );

        model.addAttribute(
                "keyword",
                keyword
        );

        model.addAttribute(
                "target",
                target
        );

        model.addAttribute(
                "usage",
                usage
        );

        model.addAttribute(
                "amount",
                amount
        );

        model.addAttribute(
                "ageGroup",
                ageGroup
        );

        model.addAttribute(
                "region",
                region
        );

        model.addAttribute(
                "size",
                size
        );

        model.addAttribute(
                "currentPage",
                currentPage
        );

        model.addAttribute(
                "startPage",
                startPage
        );

        model.addAttribute(
                "endPage",
                endPage
        );

        model.addAttribute(
                "filterApplied",
                filterApplied
        );

        return "policy/support-list";
    }

    private boolean hasValue(
            String value
    ) {
        return value != null
                && !value.trim().isEmpty();
    }
}
