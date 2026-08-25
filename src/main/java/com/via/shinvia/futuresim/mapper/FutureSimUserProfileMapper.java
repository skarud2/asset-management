package com.via.shinvia.futuresim.mapper;

import com.via.shinvia.futuresim.entity.FutureSimUserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FutureSimUserProfileMapper {

    FutureSimUserProfile findByUserId(@Param("userId") Long userId);
}
