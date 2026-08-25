package com.via.shinvia.service.mydata;

import com.via.shinvia.loan.account.entity.LoanAccount;
import com.via.shinvia.loan.account.mapper.LoanAccountMapper;
import com.via.shinvia.mydata.client.MyDataLoanClient;
import com.via.shinvia.mydata.client.dto.response.LoanItemResponseDto;
import com.via.shinvia.mydata.client.dto.response.LoanListResponseDto;
import com.via.shinvia.mydata.service.MyDataAuthService;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanAccountSyncService {

    private final MyDataConnectionService myDataConnectionService;
    private final MyDataAuthService myDataAuthService;
    private final MyDataLoanClient myDataLoanClient;
    private final LoanAccountMapper loanAccountMapper;

    /**
     * DB에 이미 저장된 보유 대출 목록만 조회한다 (목서버 재호출 없음).
     * 마이데이터 연동이 안 돼 있으면 에러 대신 빈 목록을 반환한다 - 대출 시뮬레이션 화면에서
     * "내 보유 대출" 위젯을 그릴 때, 연동 전 사용자도 화면 자체는 볼 수 있어야 하기 때문.
     */
    public List<LoanAccount> getMyLoans(Long userId) {
        Long connectionId;
        try {
            connectionId = myDataConnectionService.getConnectedConnectionId(userId);
        } catch (IllegalStateException e) {
            return List.of();
        }
        return loanAccountMapper.findAllByConnectionId(connectionId);
    }

    @Transactional
    public List<LoanAccount> syncLoans(Long userId) {

        /*
         * 1. 로그인 사용자의 CONNECTED MyData connection 조회
         */
        Long connectionId =
                myDataConnectionService
                        .getConnectedConnectionId(userId);

        /*
         * 2. Redis에서 connectionId(CI)에 대응하는 Access Token 조회
         *
         * 현재 프로젝트 OAuth 구조:
         * connectionId -> Mock의 userCi로 전달
         * Redis:
         * mydata:ci:at:{connectionId} -> accessToken
         */
        String accessToken =
                myDataAuthService
                        .getAccessToken(connectionId);

        /*
         * 3. Mock 보유대출 API 호출
         */
        LoanListResponseDto response =
                myDataLoanClient.getLoans(accessToken);

        if (response == null) {
            throw new IllegalStateException(
                    "Mock 보유대출 응답이 없습니다."
            );
        }

        /*
         * 현재 Mock 내부에 rsp_code가
         * 카드 = 0000
         * 계좌 = 00000
         * 로 혼재되어 있으므로 둘 다 성공으로 처리.
         *
         * Mock 대출 API 규격을 00000으로 확정하면
         * 이후 하나로 통일하면 됨.
         */
        if (!isSuccess(response.getRspCode())) {
            throw new IllegalStateException(
                    "Mock 보유대출 조회 실패. rspCode="
                            + response.getRspCode()
                            + ", rspMsg="
                            + response.getRspMsg()
            );
        }

        List<LoanItemResponseDto> loanList =
                response.getLoanList();

        if (loanList == null || loanList.isEmpty()) {

            log.info(
                    "[Loan Sync] 보유대출 없음 - userId={}, connectionId={}",
                    userId,
                    connectionId
            );

            return List.of();
        }

        /*
         * 4. loan_account에 INSERT / UPDATE
         */
        List<LoanAccount> savedLoans =
                new ArrayList<>();

        for (LoanItemResponseDto dto : loanList) {

            LoanAccount saved =
                    upsertLoan(
                            connectionId,
                            dto
                    );

            savedLoans.add(saved);
        }

        log.info(
                "[Loan Sync] 보유대출 동기화 완료 - userId={}, connectionId={}, count={}",
                userId,
                connectionId,
                savedLoans.size()
        );

        return savedLoans;
    }


    private LoanAccount upsertLoan(
            Long connectionId,
            LoanItemResponseDto dto
    ) {

        /*
         * 같은 사용자 connection의 동일 외부대출 확인
         */
        LoanAccount existing =
                loanAccountMapper.findByExternalLoanKey(
                        connectionId,
                        dto.getExternalLoanKey()
                );

        LocalDateTime now = LocalDateTime.now();

        LoanAccount loanAccount =
                LoanAccount.builder()

                        .loanAccountId(
                                existing == null
                                        ? null
                                        : existing.getLoanAccountId()
                        )

                        .connectionId(connectionId)

                        /*
                         * Mock external_loan_key
                         * -> 서비스 external_loan_key 그대로 저장
                         */
                        .externalLoanKey(
                                dto.getExternalLoanKey()
                        )

                        .loanType(
                                dto.getLoanType()
                        )

                        .principalAmount(
                                dto.getPrincipalAmount()
                        )

                        .currentBalance(
                                dto.getCurrentBalance()
                        )

                        .interestRate(
                                dto.getInterestRate()
                        )

                        .rateType(
                                dto.getRateType()
                        )

                        .repaymentType(
                                dto.getRepaymentType()
                        )

                        .disbursedAt(
                                dto.getDisbursedAt()
                        )

                        .maturityAt(
                                dto.getMaturityAt()
                        )

                        .loanStatus(
                                dto.getLoanStatus()
                        )

                        .dataAsOfAt(
                                dto.getDataAsOfAt()
                        )

                        .updatedAt(now)

                        .prepaymentFeeRate(
                                dto.getPrepaymentFeeRate()
                        )

                        .prepaymentFeeEndDate(
                                dto.getPrepaymentFeeEndDate()
                        )

                        .build();


        if (existing == null) {

            int inserted =
                    loanAccountMapper
                            .insertLoanAccount(
                                    loanAccount
                            );

            if (inserted != 1) {
                throw new IllegalStateException(
                        "보유대출 저장에 실패했습니다: "
                                + dto.getExternalLoanKey()
                );
            }

            log.info(
                    "[Loan Sync] INSERT externalLoanKey={}",
                    dto.getExternalLoanKey()
            );

        } else {

            int updated =
                    loanAccountMapper
                            .updateLoanAccount(
                                    loanAccount
                            );

            if (updated != 1) {
                throw new IllegalStateException(
                        "보유대출 수정에 실패했습니다: "
                                + dto.getExternalLoanKey()
                );
            }

            log.info(
                    "[Loan Sync] UPDATE externalLoanKey={}",
                    dto.getExternalLoanKey()
            );
        }

        return loanAccount;
    }


    private boolean isSuccess(String rspCode) {
        return "0000".equals(rspCode)
                || "00000".equals(rspCode);
    }
}