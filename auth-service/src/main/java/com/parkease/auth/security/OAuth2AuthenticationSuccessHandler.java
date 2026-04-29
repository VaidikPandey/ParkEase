package com.parkease.auth.security;

import com.parkease.auth.domain.entity.User;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.util.CookieUtil;
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
    private final CookieUtil cookieUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String picUrl   = oAuth2User.getAttribute("picture");
        String googleId = oAuth2User.getAttribute("sub");

        boolean[] isNew = { false };
        User user = userRepository
                .findByOauthProviderAndOauthProviderId("google", googleId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .orElseGet(() -> {
                            isNew[0] = true;
                            User newUser = User.builder()
                                    .fullName(name)
                                    .email(email)
                                    .role(User.Role.DRIVER)
                                    .oauthProvider("google")
                                    .oauthProviderId(googleId)
                                    .profilePicUrl(picUrl)
                                    .isActive(true)
                                    .build();
                            return userRepository.save(newUser);
                        }));

        String accessToken  = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name(), user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Set tokens as HttpOnly cookies — no tokens in the redirect URL
        response.addHeader("Set-Cookie", cookieUtil.createAccessTokenCookie(accessToken).toString());
        response.addHeader("Set-Cookie", cookieUtil.createRefreshTokenCookie(refreshToken).toString());

        log.info("OAuth2 login successful for: {} (new={})", email, isNew[0]);

        String redirectUrl = "http://localhost:4200/oauth2/success"
                + "?isNewUser=" + isNew[0]
                + "&userId=" + user.getUserId()
                + "&email=" + java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8)
                + "&fullName=" + java.net.URLEncoder.encode(user.getFullName(), java.nio.charset.StandardCharsets.UTF_8)
                + "&role=" + user.getRole().name();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}