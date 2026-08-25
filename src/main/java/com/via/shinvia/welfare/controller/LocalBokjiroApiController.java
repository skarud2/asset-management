package com.via.shinvia.welfare.controller;

import com.via.shinvia.welfare.dto.LocalBokjiroDetailResponseDTO;
import com.via.shinvia.welfare.dto.LocalBokjiroListRequestDTO;
import com.via.shinvia.welfare.dto.LocalBokjiroListResponseDTO;
import com.via.shinvia.welfare.dto.LocalWelfareServiceDto;
import com.via.shinvia.welfare.service.LocalBokjiroApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "지자체 복지서비스 API (localbokjiro)", description = "지자체 복지서비스 Open API 조회 및 DB 저장 관련 컨트롤러")
@RestController
@RequestMapping("/api/localbokjiro")
@RequiredArgsConstructor
public class LocalBokjiroApiController {

    private final LocalBokjiroApiService localBokjiroService;

    @Operation(summary = "지자체 복지서비스 목록 조회 (API 실시간)", description = "공공데이터포털 지자체 복지서비스 Open API를 호출하여 목록을 조회합니다.")
    @PostMapping("/welfare-list")
    public ResponseEntity<LocalBokjiroListResponseDTO> searchLocalWelfareList(@RequestBody(required = false) LocalBokjiroListRequestDTO request) {
        if (request == null) {
            request = LocalBokjiroListRequestDTO.builder().build();
        }
        LocalBokjiroListResponseDTO response = localBokjiroService.searchLocalWelfareList(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "지자체 복지서비스 상세 조회 (API 실시간)", description = "서비스 ID(servId)를 기반으로 지자체 복지서비스 상세정보를 조회합니다.")
    @GetMapping("/welfare-detail/{servId}")
    public ResponseEntity<LocalBokjiroDetailResponseDTO> getLocalWelfareDetail(@PathVariable("servId") String servId) {
        LocalBokjiroDetailResponseDTO response = localBokjiroService.getLocalWelfareDetail(servId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "DB 저장된 지자체 복지서비스 총 건수 조회", description = "DB localbokjiro 테이블에 저장된 전체 데이터 수(COUNT)를 조회합니다.")
    @GetMapping("/db-count")
    public ResponseEntity<Long> getSavedDbCount() {
        long count = localBokjiroService.getSavedDbCount();
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "DB 저장된 지자체 복지서비스 목록 전체 조회", description = "DB localbokjiro 테이블에 저장된 모든 지자체 복지서비스 목록을 조회합니다.")
    @GetMapping("/db-list")
    public ResponseEntity<List<LocalWelfareServiceDto>> getSavedDbList() {
        List<LocalWelfareServiceDto> list = localBokjiroService.getSavedDbList();
        return ResponseEntity.ok(list);
    }
}
