package com.via.shinvia.welfare.mapper;

import com.via.shinvia.welfare.dto.LocalWelfareServiceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface LocalWelfareSupportProductMapper {

    int upsert(LocalWelfareServiceDto entity);

    int deactivateAll();

    List<LocalWelfareServiceDto> findAll();

    long count();

    Optional<LocalWelfareServiceDto> findByServId(@Param("servId") String servId);
}
