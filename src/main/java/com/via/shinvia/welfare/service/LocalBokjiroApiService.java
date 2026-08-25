package com.via.shinvia.welfare.service;

import com.via.shinvia.welfare.client.LocalBokjiroApiClient;
import com.via.shinvia.welfare.dto.LocalBokjiroDetailResponseDTO;
import com.via.shinvia.welfare.dto.LocalBokjiroListRequestDTO;
import com.via.shinvia.welfare.dto.LocalBokjiroListResponseDTO;
import com.via.shinvia.welfare.dto.LocalWelfareServiceDto;
import com.via.shinvia.welfare.mapper.LocalWelfareSupportProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalBokjiroApiService {

    private final LocalBokjiroApiClient localBokjiroApiClient;
    private final LocalWelfareSupportProductMapper localBokjiroRepository;

    public LocalBokjiroListResponseDTO searchLocalWelfareList(LocalBokjiroListRequestDTO request) {
        log.info("localbokjiro 지자체 복지서비스 목록 조회 서비스 실행: {}", request);
        return localBokjiroApiClient.fetchLocalWelfareList(request);
    }

    public LocalBokjiroDetailResponseDTO getLocalWelfareDetail(String servId) {
        log.info("localbokjiro 지자체 복지서비스 상세 조회 서비스 실행: servId={}", servId);
        return localBokjiroApiClient.fetchLocalWelfareDetail(servId);
    }

    public long getSavedDbCount() {
        return localBokjiroRepository.count();
    }

    public List<LocalWelfareServiceDto> getSavedDbList() {
        return localBokjiroRepository.findAll();
    }
}
