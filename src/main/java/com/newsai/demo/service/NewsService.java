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

        String url = "https://newsapi.org/v2/everything?q=AI OR artificial intelligence OR machine learning OR startups OR tech&language=en&sortBy=publishedAt&pageSize=10&apiKey=" + API_KEY;

        List<NewsArticle> result = new ArrayList<>();

        try {
            String response = restTemplate.getForObject(url, String.class);

            if (response == null) return result;

            JsonNode root = mapper.readTree(response);
            JsonNode articles = root.get("articles");

            if (articles == null || !articles.isArray()) return result;

            for (JsonNode article : articles) {

                // 🔥 limit AI calls (cost + latency control)
                if (result.size() >= 5) break;

                // skip invalid title
                if (article.get("title") == null || article.get("title").isNull()) continue;

                String title = article.get("title").asText();

                // safe description
                String description = (article.get("description") != null && !article.get("description").isNull())
                        ? article.get("description").asText()
                        : null;

                if (description == null || description.isBlank()) continue;

                String urlLink = (article.get("url") != null && !article.get("url").isNull())
                        ? article.get("url").asText()
                        : "";

                NewsArticle news = new NewsArticle();
                news.setTitle(title);
                news.setDescription(description);
                news.setUrl(urlLink);

                // 🔥 AI enrichment
                try {
                    Map<String, String> ai = geminiService.getSummary(description);

                    news.setSummary(ai.getOrDefault("summary", "AI unavailable"));
                    news.setWhyItMatters(ai.getOrDefault("whyItMatters", "Try again later"));

                } catch (Exception e) {
                    System.out.println("AI error: " + e.getMessage());
                    news.setSummary("AI unavailable");
                    news.setWhyItMatters("Try again later");
                }

                result.add(news);
            }

        } catch (Exception e) {
            System.out.println("News API error: " + e.getMessage());
        }

        return result;
    }
}