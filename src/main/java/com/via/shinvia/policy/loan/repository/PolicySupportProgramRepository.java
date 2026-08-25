package com.via.shinvia.policy.loan.repository;

import com.via.shinvia.policy.loan.entity.PolicySupportProgram;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
// 맞춤대출 상품 DB 조회 및 저장 기능
public interface PolicySupportProgramRepository {

    Optional<PolicySupportProgram> findByExternalSeq(
            @Param("externalSeq") String externalSeq
    );

    Optional<PolicySupportProgram> findById(
            @Param("programId") Long programId
    );

    List<PolicySupportProgram> searchPrograms(
            @Param("keyword") String keyword,
            @Param("target") String target,
            @Param("usage") String usage,
            @Param("amount") String amount,
            @Param("ageGroup") String ageGroup,
            @Param("region") String region,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long countPrograms(
            @Param("keyword") String keyword,
            @Param("target") String target,
            @Param("usage") String usage,
            @Param("amount") String amount,
            @Param("ageGroup") String ageGroup,
            @Param("region") String region
    );

    int insert(PolicySupportProgram program);

    int update(PolicySupportProgram program);
}
