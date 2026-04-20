package com.parkease.auth.security;

import com.parkease.auth.domain.entity.User;
import com.parkease.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email     = oAuth2User.getAttribute("email");
        String name      = oAuth2User.getAttribute("name");
        String picUrl    = oAuth2User.getAttribute("picture");
        String googleId  = oAuth2User.getAttribute("sub");

        // find existing user or create new one
        User user = userRepository
                .findByOauthProviderAndOauthProviderId("google", googleId)
                .orElseGet(() -> {
                    // check if email already registered normally
                    return userRepository.findByEmail(email)
                            .orElseGet(() -> {
                                User newUser = User.builder()
                                        .fullName(name)
                                        .email(email)
                                        .role(User.Role.DRIVER)  // default role for OAuth users
                                        .oauthProvider("google")
                                        .oauthProviderId(googleId)
                                        .profilePicUrl(picUrl)
                                        .isActive(true)
                                        .build();
                                return userRepository.save(newUser);
                            });
                });

        // generate JWT
        String accessToken  = jwtUtil.generateAccessToken(
                user.getEmail(), user.getRole().name(), user.getUserId()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("OAuth2 login successful for: {}", email);

        // redirect to frontend with tokens as query params
        // on Day 8 when Angular is ready, change this URL to your frontend
        String redirectUrl = "http://localhost:4200/oauth2/success"
                + "?accessToken=" + accessToken
                + "&refreshToken=" + refreshToken;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}