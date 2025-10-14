package com.example.iotserver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Tạo JWT token từ email
     */
    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email);
    }

    /**
     * Tạo token với claims và subject
     */
    private String createToken(Map<String, Object> claims, String subject) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiryDate = new Date(nowMillis + expiration);

        return Jwts.builder()
                .claims(claims) // ✅ Không có "set"
                .subject(subject) // ✅ Không có "set"
                .issuedAt(now) // ✅ Không có "set"
                .expiration(expiryDate) // ✅ Không có "set"
                .signWith(getSigningKey(), Jwts.SIG.HS256) // ✅ Dùng Jwts.SIG
                .compact();
    }

    /**
     * Lấy signing key từ secret (Base64 decoded)
     */
    private SecretKey getSigningKey() {
        // ✅ DECODE Base64 để đảm bảo đủ 256 bits
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Trích xuất email từ token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Trích xuất expiration date từ token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Trích xuất claim từ token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Trích xuất tất cả claims từ token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser() // ✅ Dùng parser() thay vì parserBuilder()
                .verifyWith(getSigningKey()) // ✅ Dùng verifyWith()
                .build()
                .parseSignedClaims(token) // ✅ Dùng parseSignedClaims()
                .getPayload(); // ✅ Dùng getPayload()
    }

    /**
     * Kiểm tra token có hết hạn không
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Xác thực token
     */
    public Boolean validateToken(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }
}
