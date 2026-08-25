package com.via.shinvia.surplusfund.guideversion.controller;

import com.via.shinvia.security.CurrentUser;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionCreateRequest;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionCreateResponse;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionDetailResponse;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionRenameRequest;
import com.via.shinvia.surplusfund.guideversion.dto.GuideVersionSummaryResponse;
import com.via.shinvia.surplusfund.guideversion.service.SurplusFundGuideVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/surplus-funds/guide-versions")
@RequiredArgsConstructor
public class SurplusFundGuideVersionController {

    private final SurplusFundGuideVersionService guideVersionService;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<GuideVersionCreateResponse> create(
            Authentication authentication,
            @Valid @RequestBody GuideVersionCreateRequest request
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(guideVersionService.create(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<GuideVersionSummaryResponse>> findAll(
            Authentication authentication
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(guideVersionService.findAll(userId));
    }

    @GetMapping("/{guideVersionId}")
    public ResponseEntity<GuideVersionDetailResponse> findDetail(
            Authentication authentication,
            @PathVariable Long guideVersionId
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(
                guideVersionService.findDetail(userId, guideVersionId)
        );
    }

    @PatchMapping("/{guideVersionId}/name")
    public ResponseEntity<GuideVersionCreateResponse> rename(
            Authentication authentication,
            @PathVariable Long guideVersionId,
            @Valid @RequestBody GuideVersionRenameRequest request
    ) {
        Long userId = currentUser.getUserId(authentication);
        return ResponseEntity.ok(
                guideVersionService.rename(userId, guideVersionId, request.guideName())
        );
    }
}
