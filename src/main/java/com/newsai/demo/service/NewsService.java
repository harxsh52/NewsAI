package com.newsai.demo.service;

import com.newsai.demo.model.NewsArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class NewsService {

    private final String API_KEY = "";

    public List<NewsArticle> getTechNews() {

        String url = "https://newsapi.org/v2/everything?q=technology OR AI OR startup&language=en&sortBy=publishedAt&apiKey=" + API_KEY;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        List<NewsArticle> result = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode articles = root.get("articles");

            for (JsonNode article : articles) {

                // Skip if missing title
                if (article.get("title") == null || article.get("title").isNull()) continue;

                String title = article.get("title").asText();

                // Handle description safely
                String description = (article.get("description") != null && !article.get("description").isNull())
                        ? article.get("description").asText()
                        : "No description available";

                // Skip bad entries
                if (description.equals("No description available")) continue;

                String urlLink = (article.get("url") != null && !article.get("url").isNull())
                        ? article.get("url").asText()
                        : "";

                NewsArticle news = new NewsArticle();
                news.setTitle(title);
                news.setDescription(description);
                news.setUrl(urlLink);

                result.add(news);
            }

        } catch (Exception e) {
            System.out.println("Error parsing news: " + e.getMessage());
        }

        return result;
    }
}