package com.innowise.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.dto.TokenValidationRequestDto;
import com.innowise.userservice.dto.TokenValidationResponseDto;
import java.io.IOException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceClient {
    private final ObjectMapper objectMapper;
    private static final String AUTH_SERVICE_URL = "http://localhost:8082";

    public AuthServiceClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public TokenValidationResponseDto validateToken(String token) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(AUTH_SERVICE_URL + "/authentication/validate-token");
            httpPost.setHeader("Content-Type", "application/json");

            TokenValidationRequestDto request = new TokenValidationRequestDto(token);
            String jsonRequest = objectMapper.writeValueAsString(request);
            httpPost.setEntity(new StringEntity(jsonRequest));

            return httpClient.execute(httpPost, response -> {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(jsonResponse, TokenValidationResponseDto.class);
            });
        } catch (IOException e) {
            TokenValidationResponseDto errorResponse = new TokenValidationResponseDto();
            errorResponse.setValid(false);
            errorResponse.setMessage("Auth service unavailable: " + e.getMessage());
            return errorResponse;
        }
    }
}
