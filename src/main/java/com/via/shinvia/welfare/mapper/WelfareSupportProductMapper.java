package com.via.shinvia.welfare.mapper;

import com.via.shinvia.welfare.dto.WelfareServiceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface WelfareSupportProductMapper {

    int upsert(WelfareServiceDto entity);

    int deactivateAll();

    List<WelfareServiceDto> findAll();

    long count();

    Optional<WelfareServiceDto> findByServId(@Param("servId") String servId);
}
