package authservice.service;


import authservice.dto.AuthResponse;
import authservice.dto.LoginRequest;
import authservice.dto.RegisterRequest;
import authservice.entity.User;
import authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public void register(RegisterRequest request) {

        if (userRepository
                .findByUsername(request.getUsername())
                .isPresent()) {

            throw new RuntimeException(
                    "Username already exists"
            );
        }

        User user = new User();

        user.setUsername(request.getUsername());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();
        String accessToken =
                jwtService.generateToken(userDetails);
        String refreshToken =
                jwtService.generateRefreshToken(userDetails);
        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new RuntimeException(
                    "Invalid refresh token"
            );
        }
        String username =
                jwtService.extractUsername(refreshToken);
        UserDetails userDetails =
                userDetailsService.loadUserByUsername(username);

        String newAccessToken =
                jwtService.generateToken(userDetails);
        return new AuthResponse(
                newAccessToken,
                refreshToken
        );
    }
}