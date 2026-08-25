package com.via.shinvia.oauth2.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class SocialUser {
    private Long socialUserId;
    private Long userId;
    private SocialProvider provider;
    private String providerUserId;
    private String providerEmail;
    private LocalDateTime createdAt;
}
