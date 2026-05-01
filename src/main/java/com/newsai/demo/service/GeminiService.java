package com.newsai.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.*;

import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, String> getSummary(String text) {

        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String prompt = """
        Return ONLY valid JSON. No explanation, no text.

        Format:
        {"summary":"...","whyItMatters":"..."}

        News:
        """ + text;

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode root = mapper.readTree(response.getBody());

            String aiText = root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            System.out.println("AI RAW RESPONSE: " + aiText);

            // 🔥 CLEAN RESPONSE
            String cleaned = aiText
                    .replace("```json", "")
                    .replace("```", "")
                    .replace("\n", " ")
                    .trim();

            int start = cleaned.indexOf("{");
            int end = cleaned.lastIndexOf("}");

            if (start == -1 || end == -1 || start >= end) {
                throw new RuntimeException("Invalid AI format: " + aiText);
            }

            cleaned = cleaned.substring(start, end + 1);

            JsonNode json = mapper.readTree(cleaned);

            if (!json.has("summary") || !json.has("whyItMatters")) {
                throw new RuntimeException("Missing fields: " + cleaned);
            }

            return Map.of(
                    "summary", json.get("summary").asText(),
                    "whyItMatters", json.get("whyItMatters").asText()
            );

        } catch (HttpClientErrorException e) {
            // 🔥 Handle API errors properly
            System.out.println("Gemini API ERROR: " + e.getResponseBodyAsString());

            return Map.of(
                    "summary", "Invalid API key or quota issue",
                    "whyItMatters", "Check Gemini configuration"
            );

        } catch (Exception e) {
            System.out.println("AI parsing failed: " + e.getMessage());

            return Map.of(
                    "summary", "AI unavailable",
                    "whyItMatters", "Try again later"
            );
        }
    }
}