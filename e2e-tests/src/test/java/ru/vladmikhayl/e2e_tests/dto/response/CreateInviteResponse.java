package ru.vladmikhayl.e2e_tests.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInviteResponse {
    private String token;
    private Instant expiresAt;
}
