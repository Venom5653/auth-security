package authservice.dto;

public record UserProfileResponse(Long id,
                                 String username,
                                 String avatar) {
}
