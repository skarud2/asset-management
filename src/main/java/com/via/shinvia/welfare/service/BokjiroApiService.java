package com.via.shinvia.welfare.service;

import com.via.shinvia.welfare.client.BokjiroApiClient;
import com.via.shinvia.welfare.dto.BokjiroServiceDetailDto;
import com.via.shinvia.welfare.dto.BokjiroListRequestDTO;
import com.via.shinvia.welfare.dto.BokjiroApiResponse;
import com.via.shinvia.welfare.dto.WelfareServiceDto;
import com.via.shinvia.welfare.mapper.WelfareSupportProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BokjiroApiService {

    private final BokjiroApiClient bokjiroApiClient;
    private final WelfareSupportProductMapper bokjiroRepository;

    public BokjiroApiResponse searchWelfareList(BokjiroListRequestDTO request) {
        log.info("복지로 복지서비스 목록 조회 서비스 실행: {}", request);
        return bokjiroApiClient.fetchWelfareList(request);
    }

    public BokjiroServiceDetailDto getWelfareDetail(String servId) {
        log.info("복지로 복지서비스 상세 조회 서비스 실행: servId={}", servId);
        return bokjiroApiClient.fetchWelfareDetail(servId);
    }

    public long getSavedDbCount() {
        return bokjiroRepository.count();
    }

    public List<WelfareServiceDto> getSavedDbList() {
        return bokjiroRepository.findAll();
    }
}
