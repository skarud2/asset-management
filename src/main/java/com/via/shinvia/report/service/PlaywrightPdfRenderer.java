package com.via.shinvia.report.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.Margin;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaywrightPdfRenderer {

    private static final double RENDER_TIMEOUT_MILLIS = 60_000;

    private final Object browserMonitor = new Object();
    private final String internalBaseUrl;

    private Playwright playwright;
    private Browser browser;

    public PlaywrightPdfRenderer(@Value("${report.pdf.internal-base-url:http://localhost:8080}") String internalBaseUrl) {
        this.internalBaseUrl = stripTrailingSlash(internalBaseUrl);
    }

    public byte[] generatePdf(Long userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.isBlank()) {
            throw new IllegalStateException("PDF 요청의 로그인 세션을 찾을 수 없어요.");
        }
        try (BrowserContext context = browser().newContext()) {
            context.addCookies(List.of(new Cookie("JSESSIONID", sessionId).setUrl(internalBaseUrl)));
            Page page = context.newPage();
            page.setDefaultTimeout(RENDER_TIMEOUT_MILLIS);
            page.setViewportSize(794, 1123);
            page.navigate(internalBaseUrl + "/report/render?pdf=true",
                    new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                            .setTimeout(RENDER_TIMEOUT_MILLIS));
            page.waitForSelector("body[data-charts-ready='true']",
                    new Page.WaitForSelectorOptions().setTimeout(RENDER_TIMEOUT_MILLIS));
            page.evaluate("() => document.fonts.ready");
            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("6mm").setRight("10mm").setBottom("10mm").setLeft("10mm")));
        } catch (Exception e) {
            throw new IllegalStateException("개인화 리포트 PDF 생성에 실패했어요.", e);
        }
    }

    private Browser browser() {
        synchronized (browserMonitor) {
            if (browser == null || !browser.isConnected()) {
                closeBrowser();
                playwright = Playwright.create();
                browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            }
            return browser;
        }
    }

    @PreDestroy
    void closeBrowser() {
        synchronized (browserMonitor) {
            if (browser != null) {
                browser.close();
                browser = null;
            }
            if (playwright != null) {
                playwright.close();
                playwright = null;
            }
        }
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
