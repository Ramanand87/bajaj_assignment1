package com.example.assignment_app.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);
    private final RestTemplate restTemplate;
    private final ResourceLoader resourceLoader;

    @Value("${assignment.name}")
    private String name;

    @Value("${assignment.regno}")
    private String regNo;

    @Value("${assignment.email}")
    private String email;

    public AssignmentService(RestTemplate restTemplate, ResourceLoader resourceLoader) {
        this.restTemplate = restTemplate;
        this.resourceLoader = resourceLoader;
    }

    public void startFlow() {
        try {
            System.out.println("🚀 Starting assignment flow...");

            // Step 1: Generate Webhook
            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> reqBody = new HashMap<>();
            reqBody.put("name", "Ramanand Kumawat");
            reqBody.put("regNo", "112215146");
            reqBody.put("email", "112215146@cse.iiitp.ac.in");
            log.info("{}",reqBody);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(reqBody, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(generateUrl, request, Map.class);

            if (resp.getBody() == null) {
                System.err.println("❌ Failed to get webhook response");
                return;
            }

            String webhookUrl = (String) resp.getBody().get("webhook");
            String accessToken = (String) resp.getBody().get("accessToken");

            System.out.println("✅ Webhook: " + webhookUrl);
            System.out.println("✅ AccessToken: " + accessToken);

            // Step 2: Read SQL from file
            Resource resource = resourceLoader.getResource("classpath:final-query.sql");
            String finalQuery;
            try (InputStream is = resource.getInputStream()) {
                finalQuery = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }

            if (finalQuery.isBlank()) {
                System.err.println("❌ No SQL query found in final-query.sql");
                return;
            }

            // Step 3: Submit SQL
            HttpHeaders headers2 = new HttpHeaders();
            headers2.setContentType(MediaType.APPLICATION_JSON);
            headers2.set("Authorization", accessToken); // if fails, try headers2.setBearerAuth(accessToken)

            Map<String, String> finalBody = new HashMap<>();
            finalBody.put("finalQuery", finalQuery);

            HttpEntity<Map<String, String>> finalReq = new HttpEntity<>(finalBody, headers2);
            ResponseEntity<String> submitResp = restTemplate.postForEntity(webhookUrl, finalReq, String.class);

            System.out.println("📤 Submit Status: " + submitResp.getStatusCodeValue());
            System.out.println("📩 Response: " + submitResp.getBody());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
