package com.via.shinvia.report.controller;

import com.via.shinvia.report.dto.ReportCardSelection;
import com.via.shinvia.report.dto.request.ReportLayoutSaveRequest;
import com.via.shinvia.report.dto.response.ReportCardOptionResponse;
import com.via.shinvia.report.dto.response.ReportHeaderResponse;
import com.via.shinvia.report.dto.response.ReportLayoutResponse;
import com.via.shinvia.report.service.ReportCardService;
import com.via.shinvia.report.service.ReportHeaderService;
import com.via.shinvia.report.service.PlaywrightPdfRenderer;
import com.via.shinvia.report.service.provider.ReportCardDataProvider;
import com.via.shinvia.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private final CurrentUser currentUser;
    private final ReportHeaderService reportHeaderService;
    private final ReportCardService reportCardService;
    private final PlaywrightPdfRenderer playwrightPdfRenderer;

    @GetMapping("/report")
    public String reportPage(Model model) {
        model.addAttribute("serviceName", "금융 라이프 플랜");
        model.addAttribute("pdfMode", false);
        return "report/report";
    }

    @GetMapping("/report/render")
    public String reportRender(@RequestParam(defaultValue = "false") boolean pdf, Model model, HttpServletRequest request) {
        if (!pdf || !isLoopbackRequest(request)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("serviceName", "금융 라이프 플랜");
        model.addAttribute("pdfMode", pdf);
        return "report/report";
    }

    @GetMapping("/api/report/header")
    @ResponseBody
    public ResponseEntity<ReportHeaderResponse> header(Authentication authentication) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(reportHeaderService.getHeader(userId));
    }

    @GetMapping("/api/report/card-options")
    @ResponseBody
    public ResponseEntity<List<ReportCardOptionResponse>> cardOptions(Authentication authentication) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(reportCardService.listOptions(userId));
    }

    @GetMapping("/api/report/card-data")
    @ResponseBody
    public ResponseEntity<ReportCardDataProvider.CardData> cardData(
            @RequestParam String cardKey, @RequestParam(required = false) Long refId, Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(reportCardService.getCardData(cardKey, userId, refId));
    }

    @GetMapping("/api/report/layout")
    @ResponseBody
    public ResponseEntity<ReportLayoutResponse> getLayout(Authentication authentication) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(new ReportLayoutResponse(reportCardService.getLayout(userId)));
    }

    @PostMapping("/api/report/layout")
    @ResponseBody
    public ResponseEntity<ReportLayoutResponse> saveLayout(
            @RequestBody ReportLayoutSaveRequest request,
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        List<ReportCardSelection> cards = request.cards() == null ? List.of() : request.cards();
        reportCardService.saveLayout(userId, cards);
        return ResponseEntity.ok(new ReportLayoutResponse(cards));
    }

    @GetMapping("/api/report/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> pdf(
            @RequestParam(defaultValue = "false") boolean preview,
            Authentication authentication,
            HttpServletRequest request
    ) {
        Long userId = currentUser.getUserId(authentication);
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("PDF 요청의 로그인 세션을 찾을 수 없어요.");
        }
        byte[] pdfBytes = playwrightPdfRenderer.generatePdf(userId, session.getId());
        String disposition = (preview ? "inline" : "attachment") + "; filename=\"personal-report.pdf\"";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(pdfBytes);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    private boolean isLoopbackRequest(HttpServletRequest request) {
        try {
            return InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
