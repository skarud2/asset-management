package com.via.shinvia.report.mapper;

import com.via.shinvia.report.entity.ReportCardLayout;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportCardLayoutMapper {

    List<ReportCardLayout> findAllByUserIdOrderByDisplayOrder(@Param("userId") Long userId);

    void deleteByUserId(@Param("userId") Long userId);

    void insert(@Param("userId") Long userId, @Param("cardKey") String cardKey, @Param("refId") Long refId,
                @Param("displayOrder") int displayOrder);
}
