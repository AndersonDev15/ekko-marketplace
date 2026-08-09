
package com.ekko.api_gateway.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OAuth2TestController {

    @GetMapping("/test/oauth2")
    public Map<String, Object> testOAuth2(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        return Map.of(
                "authenticated", true,
                "name", oauth2User.getAttribute("preferred_username"),
                "email", oauth2User.getAttribute("email"),
                "subject", oauth2User.getAttribute("sub")
        );
    }
}

