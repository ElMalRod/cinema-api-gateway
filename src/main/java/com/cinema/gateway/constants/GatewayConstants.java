package com.cinema.gateway.constants;

import java.util.Set;

public final class GatewayConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String CLAIM_USER_ID = "user_id";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EXP = "exp";
    public static final String RS256_ALGORITHM = "RS256";
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String PUBLIC_KEY_ENDPOINT = "/auth/public-key";
    public static final Set<String> ALLOWED_ROLES = Set.of(
            "SYSTEM_ADMIN",
            "CINEMA_ADMIN",
            "CLIENT",
            "ADVERTISER"
    );

    private GatewayConstants() {
    }
}
