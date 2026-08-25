package com.via.shinvia.report.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PlaywrightPdfRendererTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void 로그인_세션으로_렌더링_페이지를_두번_캡처한다() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/report/render", exchange -> renderPage(exchange, requestCount));
        server.start();

        PlaywrightPdfRenderer renderer = new PlaywrightPdfRenderer(
                "http://localhost:" + server.getAddress().getPort()
        );

        byte[] first = renderer.generatePdf(1L, "report-session");
        byte[] second = renderer.generatePdf(1L, "report-session");

        assertThat(first).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(second).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(requestCount.get()).isEqualTo(2);
        renderer.closeBrowser();
    }

    private void renderPage(HttpExchange exchange, AtomicInteger requestCount) throws IOException {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie == null || !cookie.contains("JSESSIONID=report-session")) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }
        requestCount.incrementAndGet();
        byte[] body = "<html><body data-charts-ready=\"true\"><main>개인화 리포트</main></body></html>"
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
