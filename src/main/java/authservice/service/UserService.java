package authservice.service;

import authservice.dto.AuthResponse;
import authservice.dto.UpdatePasswordRequest;
import authservice.dto.UpdateUsernameRequest;
import authservice.dto.UserProfileResponse;
import authservice.entity.User;
import authservice.exception.UserNotFoundException;
import authservice.repository.UserRepository;
import authservice.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserProfileResponse updateAvatar(
            MultipartFile file
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String username =
                authentication.getName();

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found"
                                )
                        );

        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "Файл не выбран"
            );
        }

        if (file.getSize() > 5 * 1024 * 1024) {

            throw new RuntimeException(
                    "Размер файла не должен превышать 5 MB"
            );
        }
        String contentType =
                file.getContentType();

        String extension;

        if ("image/jpeg".equals(contentType)) {

            extension = ".jpg";

        } else if ("image/png".equals(contentType)) {

            extension = ".png";

        } else if ("image/webp".equals(contentType)) {

            extension = ".webp";

        } else {

            throw new RuntimeException(
                    "Разрешены только JPG, PNG и WEBP"
            );
        }

        Path uploadDirectory =
                Paths.get("uploads", "avatars");

        try {

            Files.createDirectories(
                    uploadDirectory
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Не удалось создать папку для аватаров",
                    e
            );
        }
        if (user.getAvatar() != null &&
                !user.getAvatar().isBlank()) {

            try {

                String oldAvatar =
                        user.getAvatar();

                String oldFileName =
                        Paths
                                .get(oldAvatar)
                                .getFileName()
                                .toString();

                Path oldFile =
                        uploadDirectory.resolve(
                                oldFileName
                        );

                Files.deleteIfExists(oldFile);

            } catch (Exception e) {

                System.err.println(
                        "Не удалось удалить старый аватар: "
                                + e.getMessage()
                );
            }
        }
        String fileName =
                UUID.randomUUID() + extension;

        Path filePath =
                uploadDirectory.resolve(fileName);
        try {

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Не удалось сохранить аватар",
                    e
            );
        }

        String avatarUrl =
                "/api/users/avatar/" + fileName;

        user.setAvatar(avatarUrl);

        userRepository.save(user);

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatar()
        );
    }

    @Transactional
    public AuthResponse updateUsername(
            UpdateUsernameRequest request
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String currentUsername =
                authentication.getName();

        User user =
                userRepository
                        .findByUsername(currentUsername)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found"
                                )
                        );

        String newUsername =
                request.getUsername();

        // Проверяем, что новое имя действительно отличается
        if (user.getUsername().equals(newUsername)) {

            throw new RuntimeException(
                    "New username must be different"
            );
        }

        // Проверяем, что такое имя уже не занято
        if (userRepository.existsByUsername(newUsername)) {

            throw new RuntimeException(
                    "Username already exists"
            );
        }

        // Меняем username
        user.setUsername(newUsername);

        UserDetails userDetails =
                new CustomUserDetails(user);

        // Новый access token
        String newAccessToken =
                jwtService.generateToken(userDetails);

        // Новый refresh token
        String newRefreshToken =
                jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken
        );
    }

    @Transactional
    public AuthResponse updatePassword(
            UpdatePasswordRequest request
    ) {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String currentUsername =
                authentication.getName();

        User user =
                userRepository
                        .findByUsername(currentUsername)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found"
                                )
                        );

        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPassword()
        )) {

            throw new RuntimeException(
                    "Invalid old password"
            );
        }

        String encodedPassword =
                passwordEncoder.encode(
                        request.getNewPassword()
                );
        user.setPassword(encodedPassword);
        UserDetails userDetails =
                new CustomUserDetails(user);

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken
        );
    }

    @Transactional
    public void deleteUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String username = authentication.getName();
        User user = userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new UserNotFoundException(username)
                        );
        if (user.getAvatar() != null &&
                !user.getAvatar().isBlank()) {
            try {
                String fileName = Paths.get(user.getAvatar())
                                .getFileName()
                                .toString();
                Path avatarPath = Paths.get("uploads", "avatars")
                                .resolve(fileName);
                Files.deleteIfExists(avatarPath);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Не удалось удалить аватар пользователя", e
                );
            }
        }
        userRepository.delete(user);
        SecurityContextHolder.clearContext();}

    @Transactional
    public UserProfileResponse getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String username = authentication.getName();

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found"
                                )
                        );

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatar()
        );
    }

    @Transactional
    public void deleteAvatar() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();
        String username = authentication.getName();
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"));
        String avatar = user.getAvatar();
        if (avatar == null || avatar.isBlank()) {
            return;
        }
        try {
            String fileName =
                    Paths.get(avatar)
                            .getFileName()
                            .toString();
            Path avatarPath = Paths.get("uploads", "avatars")
                    .resolve(fileName);
            Files.deleteIfExists(avatarPath);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось удалить файл аватара", e);
        }
        user.setAvatar(null);
        userRepository.save(user);
    }

    @Transactional
    public UserProfileResponse getUserByUsername(String username) {

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found: " + username
                                )
                        );

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatar()
        );
    }

    public UserProfileResponse getUserById(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(id)
                );

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatar()
        );
    }
}