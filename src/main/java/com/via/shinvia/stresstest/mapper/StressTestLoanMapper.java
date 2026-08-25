package com.via.shinvia.stresstest.mapper;

import com.via.shinvia.stresstest.entity.StressTestLoanRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StressTestLoanMapper {

    List<StressTestLoanRow> findNormalLoansByUserId(@Param("userId") Long userId);
}
