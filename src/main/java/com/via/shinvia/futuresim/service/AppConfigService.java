package com.via.shinvia.futuresim.service;

import com.via.shinvia.futuresim.mapper.AppConfigMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// 화면에 노출하거나 사용자가 조정하지 않는, 계산 전용 고정 가정값을 app_config에서 읽어온다
// (예: FUTURESIM_ASSUMED_RETURN_RATE). "여유자금 운용 방식 선택" 같은 사용자 대면 기능과는
// 무관하며, 그 기능은 별도의 "여유자금 운용" 메뉴에서 다룬다.
@Service
public class AppConfigService {

    private final AppConfigMapper appConfigMapper;

    public AppConfigService(AppConfigMapper appConfigMapper) {
        this.appConfigMapper = appConfigMapper;
    }

    public BigDecimal getDecimal(String configKey) {
        String value = appConfigMapper.findValueByKey(configKey);
        if (value == null) {
            throw new IllegalStateException("설정값이 없습니다: " + configKey);
        }
        return new BigDecimal(value);
    }
}
