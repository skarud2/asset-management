package com.via.shinvia.futuresim.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppConfigMapper {

    String findValueByKey(@Param("configKey") String configKey);
}
