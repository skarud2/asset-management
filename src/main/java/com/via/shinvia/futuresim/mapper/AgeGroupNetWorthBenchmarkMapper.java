package com.via.shinvia.futuresim.mapper;

import com.via.shinvia.futuresim.entity.KosisAgeGroupNetWorth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgeGroupNetWorthBenchmarkMapper {

    KosisAgeGroupNetWorth findLatestByLabel(@Param("ageGroupLabel") String ageGroupLabel);

    List<KosisAgeGroupNetWorth> findAllLatest();
}
