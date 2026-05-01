package com.newsai.demo.service;

import com.newsai.demo.model.NewsArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NewsService {

    @Value("${news.api.key}")
    private String API_KEY;

    @Autowired
    private GeminiService geminiService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<NewsArticle> getTechNews() {

        String url = "https://newsapi.org/v2/everything?q=AI OR artificial intelligence OR machine learning OR startups OR tech" +
                "&language=en&sortBy=publishedAt&pageSize=10&apiKey=" + API_KEY;

        List<NewsArticle> result = new ArrayList<>();

        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return result;

            JsonNode root = mapper.readTree(response);
            JsonNode articles = root.path("articles");

            if (!articles.isArray()) return result;

            for (JsonNode article : articles) {

                // 🔥 reduce load (important)
                if (result.size() >= 2) break;

                String title = article.path("title").asText(null);
                if (title == null || title.isBlank()) continue;

                String description = article.path("description").asText(null);
                if (description == null || description.isBlank() || description.length() < 50) continue;

                // 🔥 filter non-tech content
                String lower = (title + " " + description).toLowerCase();
                if (!(lower.contains("ai") ||
                        lower.contains("artificial intelligence") ||
                        lower.contains("machine learning") ||
                        lower.contains("tech"))) {
                    continue;
                }

                String urlLink = article.path("url").asText("");

                NewsArticle news = new NewsArticle();
                news.setTitle(title);
                news.setDescription(description);
                news.setUrl(urlLink);

                // 🔥 AI with retry
                Map<String, String> aiResult = callAiWithRetry(description);

                news.setSummary(aiResult.getOrDefault("summary", "AI unavailable"));
                news.setWhyItMatters(aiResult.getOrDefault("whyItMatters", "Try again later"));

                result.add(news);

                // 🔥 small delay (avoid rate limit)
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        } catch (Exception e) {
            System.out.println("News API error: " + e.getMessage());
        }

        return result;
    }

    // 🔥 helper method
    private Map<String, String> callAiWithRetry(String description) {

        try {
            return geminiService.getSummary(description);
        } catch (Exception e) {

            try {
                Thread.sleep(400);
                return geminiService.getSummary(description);
            } catch (Exception ex) {
                return Map.of(
                        "summary", "AI temporarily unavailable",
                        "whyItMatters", "Try again later"
                );
            }
        }
    }
}