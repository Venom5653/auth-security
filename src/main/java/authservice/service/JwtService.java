package authservice.service;

import authservice.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;


    public String generateToken(UserDetails userDetails) {

        return generateToken(userDetails, accessTokenExpiration, "access");
    }


    public String generateRefreshToken(UserDetails userDetails) {

        return generateToken(userDetails, refreshTokenExpiration, "refresh");
    }


    private String generateToken(UserDetails userDetails, long expiration, String type) {

        Date now = new Date();

        var builder = Jwts.builder().subject(userDetails.getUsername()).claim("type", type).issuedAt(now).expiration(new Date(now.getTime() + expiration));

        if (userDetails instanceof CustomUserDetails customUserDetails) {

            builder.claim("userId", customUserDetails.getId());
        }

        return builder.signWith(getKey()).compact();
    }


    public String extractUsername(String token) {

        return parseToken().apply(token).getSubject();
    }


    public Long extractUserId(String token) {

        return parseToken().apply(token).get("userId", Long.class);
    }


    public String extractTokenType(String token) {

        return parseToken().apply(token).get("type", String.class);
    }


    public boolean isTokenValid(String token, UserDetails userDetails) {

        try {

            String username = extractUsername(token);

            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);

        } catch (Exception e) {

            return false;
        }
    }


    public boolean isRefreshTokenValid(String token) {

        try {

            String type = extractTokenType(token);

            return "refresh".equals(type) && !isTokenExpired(token);

        } catch (Exception e) {

            return false;
        }
    }

    private boolean isTokenExpired(String token) {

        return parseToken().apply(token).getExpiration().before(new Date());
    }

    private java.util.function.Function<String, Claims> parseToken() {

        return token -> Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getKey() {

        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}