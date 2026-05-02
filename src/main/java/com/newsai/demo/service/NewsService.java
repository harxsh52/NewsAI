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

    // 🔥 CACHE (10 minutes)
    private List<NewsArticle> cachedNews = new ArrayList<>();
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION = 10 * 60 * 1000;

    public List<NewsArticle> getTechNews() {

        long now = System.currentTimeMillis();

        // ✅ Return cached data if still valid
        if (!cachedNews.isEmpty() && (now - lastFetchTime) < CACHE_DURATION) {
            return cachedNews;
        }

        String url = "https://newsapi.org/v2/everything?q=AI OR artificial intelligence OR machine learning OR startups OR tech"
                + "&language=en&sortBy=publishedAt&pageSize=10&apiKey=" + API_KEY;

        List<NewsArticle> result = new ArrayList<>();

        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return result;

            JsonNode articles = mapper.readTree(response).path("articles");
            if (!articles.isArray()) return result;

            for (JsonNode article : articles) {

                if (result.size() >= 2) break;

                String title = article.path("title").asText(null);
                String description = article.path("description").asText(null);

                // 🔥 Basic validation
                if (title == null || title.isBlank()) continue;
                if (description == null || description.isBlank() || description.length() < 50) continue;

                // 🔥 Filter non-tech content
                String content = (title + " " + description).toLowerCase();
                if (!isTechRelated(content)) continue;

                NewsArticle news = new NewsArticle();
                news.setTitle(title);
                news.setDescription(description);
                news.setUrl(article.path("url").asText(""));

                Map<String, String> ai = callAiWithRetry(description);

                news.setSummary(ai.getOrDefault("summary", "AI unavailable"));
                news.setWhyItMatters(ai.getOrDefault("whyItMatters", "Try again later"));

                result.add(news);

                // 🔥 Rate limit protection
                sleep(400);
            }

            // ✅ Save cache
            cachedNews = result;
            lastFetchTime = System.currentTimeMillis();

        } catch (Exception e) {
            System.out.println("News API error: " + e.getMessage());
        }

        return result;
    }

    // 🔥 Cleaner filter method
    private boolean isTechRelated(String text) {
        return text.contains("ai") ||
                text.contains("artificial intelligence") ||
                text.contains("machine learning") ||
                text.contains("tech");
    }

    // 🔥 Retry wrapper
    private Map<String, String> callAiWithRetry(String description) {
        try {
            return geminiService.getSummary(description);
        } catch (Exception e) {
            sleep(400);
            try {
                return geminiService.getSummary(description);
            } catch (Exception ex) {
                return Map.of(
                        "summary", "AI temporarily unavailable",
                        "whyItMatters", "Try again later"
                );
            }
        }
    }

    // 🔥 Utility method
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}