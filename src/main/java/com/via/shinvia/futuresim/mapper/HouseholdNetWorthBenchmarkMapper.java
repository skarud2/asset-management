package com.via.shinvia.futuresim.mapper;

import com.via.shinvia.futuresim.entity.KosisHouseholdNetWorth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HouseholdNetWorthBenchmarkMapper {

    KosisHouseholdNetWorth findLatestByLabel(@Param("householdSizeLabel") String householdSizeLabel);

    List<KosisHouseholdNetWorth> findAllLatest();
}
