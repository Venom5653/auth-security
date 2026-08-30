package authservice.controller;

import authservice.dto.AuthResponse;
import authservice.dto.UpdatePasswordRequest;
import authservice.dto.UpdateUsernameRequest;
import authservice.dto.UserProfileResponse;
import authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @PatchMapping("/username")
    public AuthResponse updateUsername(
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        return userService.updateUsername(request);
    }

    @PatchMapping("/password")
    public AuthResponse updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        return userService.updatePassword(request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(){
        userService.deleteUser();
    }

    @PostMapping(
            value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserProfileResponse> updateAvatar(
            @RequestParam("file") MultipartFile file
    ) {

        UserProfileResponse response =
                userService.updateAvatar(file);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/avatar/{fileName}")
    public ResponseEntity<Resource> getAvatar(
            @PathVariable String fileName
    ) {

        try {

            Path filePath = Paths
                    .get("uploads", "avatars")
                    .resolve(fileName)
                    .normalize();

            Resource resource =
                    new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType =
                    Files.probeContentType(filePath);

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(contentType)
                    )
                    .body(resource);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar() {
        userService.deleteAvatar();
    }

    @GetMapping("/internal/{username}")
    public ResponseEntity<UserProfileResponse> getUserByUsername(
            @PathVariable String username
    ) {
        return ResponseEntity.ok(
                userService.getUserByUsername(username)
        );
    }
    @GetMapping("/internal/id/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }
}