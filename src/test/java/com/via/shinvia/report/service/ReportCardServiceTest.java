package com.via.shinvia.report.service;

import com.via.shinvia.report.dto.ReportCardSelection;
import com.via.shinvia.report.dto.response.ReportCardOptionResponse;
import com.via.shinvia.report.entity.ReportCardLayout;
import com.via.shinvia.report.mapper.ReportCardLayoutMapper;
import com.via.shinvia.report.service.provider.ReportCardDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ReportCardLayoutMapper layoutMapper;

    private final FakeProvider available1 = new FakeProvider("FUTURESIM", "미래 금융 시뮬레이터", true);
    private final FakeProvider available2 = new FakeProvider("ASSET_DEBT", "자산·부채 현황", true);
    private final FakeProvider stub1 = new FakeProvider("SURPLUS_FUND", "여유자금 운용", false);
    private final FakeProvider stub2 = new FakeProvider("FINANCIAL_CYCLE_PLAN", "금융 라이프 플랜", false);

    private ReportCardService service;

    @BeforeEach
    void setUp() {
        service = new ReportCardService(List.of(available1, available2, stub1, stub2), layoutMapper);
    }

    @Test
    void 사용_가능한_카드와_준비중_카드를_모두_옵션으로_돌려준다() {
        List<ReportCardOptionResponse> options = service.listOptions(USER_ID);

        assertThat(options).hasSize(4);
        assertThat(options).filteredOn(ReportCardOptionResponse::available)
                .extracting(ReportCardOptionResponse::cardKey)
                .containsExactlyInAnyOrder("FUTURESIM", "ASSET_DEBT");
        assertThat(options).filteredOn(o -> !o.available())
                .extracting(ReportCardOptionResponse::cardKey)
                .containsExactlyInAnyOrder("SURPLUS_FUND", "FINANCIAL_CYCLE_PLAN");
    }

    @Test
    void 준비중_카드의_데이터를_요청하면_예외가_발생한다() {
        assertThatThrownBy(() -> service.getCardData("SURPLUS_FUND", USER_ID, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 존재하지_않는_카드키를_요청하면_예외가_발생한다() {
        assertThatThrownBy(() -> service.getCardData("NO_SUCH_CARD", USER_ID, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 사용_가능한_카드는_데이터를_그대로_돌려준다() {
        ReportCardDataProvider.CardData data = service.getCardData("FUTURESIM", USER_ID, null);

        assertThat(data.cardKey()).isEqualTo("FUTURESIM");
    }

    @Test
    void refId를_지정하면_provider에_그대로_전달된다() {
        ReportCardDataProvider.CardData data = service.getCardData("FUTURESIM", USER_ID, 42L);

        assertThat(data.headlineValue()).isEqualTo("refId=42");
    }

    @Test
    void 준비중_카드를_배치에_저장하려_하면_예외가_발생한다() {
        assertThatThrownBy(() -> service.saveLayout(USER_ID, List.of(
                new ReportCardSelection("FUTURESIM", null), new ReportCardSelection("SURPLUS_FUND", null)
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 배치_저장은_기존_배치를_지우고_순서대로_refId와_함께_다시_저장한다() {
        service.saveLayout(USER_ID, List.of(
                new ReportCardSelection("ASSET_DEBT", null), new ReportCardSelection("FUTURESIM", 7L)
        ));

        verify(layoutMapper).deleteByUserId(USER_ID);
        verify(layoutMapper).insert(USER_ID, "ASSET_DEBT", null, 0);
        verify(layoutMapper).insert(USER_ID, "FUTURESIM", 7L, 1);
    }

    @Test
    void 배치_조회는_저장된_순서대로_카드키와_refId를_돌려준다() {
        ReportCardLayout first = new ReportCardLayout();
        first.setCardKey("ASSET_DEBT");
        ReportCardLayout second = new ReportCardLayout();
        second.setCardKey("FUTURESIM");
        second.setRefId(7L);
        when(layoutMapper.findAllByUserIdOrderByDisplayOrder(USER_ID)).thenReturn(List.of(first, second));

        assertThat(service.getLayout(USER_ID)).containsExactly(
                new ReportCardSelection("ASSET_DEBT", null), new ReportCardSelection("FUTURESIM", 7L)
        );
    }

    private static final class FakeProvider implements ReportCardDataProvider {
        private final String cardKey;
        private final String title;
        private final boolean available;

        private FakeProvider(String cardKey, String title, boolean available) {
            this.cardKey = cardKey;
            this.title = title;
            this.available = available;
        }

        @Override
        public String getCardKey() {
            return cardKey;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public CardData getCardData(Long userId, Long refId) {
            String headlineValue = refId == null ? "value" : "refId=" + refId;
            return new CardData(cardKey, title, "headline", headlineValue, List.of(), null, null, null);
        }
    }
}
