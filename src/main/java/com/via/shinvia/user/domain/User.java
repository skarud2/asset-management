package com.via.shinvia.user.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter @ToString(exclude="passwordHash")
@Builder
public class User {
    private Long userId;
    private String loginEmail;
    private String passwordHash;
    private String userName;
    private String phoneNumber;
    private LocalDate birthDate;
    private UserStatus userStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserRole userRole;
}
