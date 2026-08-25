package com.via.shinvia.welfare.controller;

import com.via.shinvia.welfare.dto.BokjiroServiceDetailDto;
import com.via.shinvia.welfare.dto.BokjiroListRequestDTO;
import com.via.shinvia.welfare.dto.BokjiroApiResponse;
import com.via.shinvia.welfare.dto.WelfareServiceDto;
import com.via.shinvia.welfare.service.BokjiroApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Bokjiro Welfare API", description = "복지로 중앙부처 복지서비스 Open API v2.0 연동 컨트롤러")
@RestController
@RequestMapping("/api/bokjiro")
@RequiredArgsConstructor
public class BokjiroWelfareApiController {

    private final BokjiroApiService bokjiroWelfareService;

    @Operation(
            summary = "중앙부처 복지서비스 목록 조회 (API 직접 호출)",
            description = "생애주기, 관심주제, 가구유형, 검색어 등을 조건으로 복지로 API를 직접 호출하여 목록을 조회합니다."
    )
    @GetMapping("/welfare-list")
    public ResponseEntity<BokjiroApiResponse> getWelfareList(
            @ModelAttribute BokjiroListRequestDTO request
    ) {
        BokjiroApiResponse response = bokjiroWelfareService.searchWelfareList(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "중앙부처 복지서비스 상세 조회 (API 직접 호출)",
            description = "서비스 ID(servId)를 사용하여 복지서비스의 상세 정보(대상자, 선정기준, 지원내용 등)를 조회합니다."
    )
    @GetMapping("/welfare-detail/{servId}")
    public ResponseEntity<BokjiroServiceDetailDto> getWelfareDetail(
            @Parameter(description = "복지서비스 ID (예: WLF00001188)", example = "WLF00001188")
            @PathVariable String servId
    ) {
        BokjiroServiceDetailDto response = bokjiroWelfareService.getWelfareDetail(servId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "DB(bokjiro 테이블)에 저장된 정책상품 개수 조회",
            description = "서버 시작 시 동기화되어 bokjiro 테이블에 저장된 총 복지 정책상품 개수를 조회합니다."
    )
    @GetMapping("/db-count")
    public ResponseEntity<Map<String, Object>> getDbCount() {
        long count = bokjiroWelfareService.getSavedDbCount();
        return ResponseEntity.ok(Map.of("totalSavedCount", count));
    }

    @Operation(
            summary = "DB(bokjiro 테이블)에 저장된 정책상품 전체 목록 조회",
            description = "bokjiro 테이블에 저장되어 있는 모든 복지 정책상품 목록을 조회합니다."
    )
    @GetMapping("/db-list")
    public ResponseEntity<List<WelfareServiceDto>> getDbList() {
        return ResponseEntity.ok(bokjiroWelfareService.getSavedDbList());
    }
}
