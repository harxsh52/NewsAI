package com.newsai.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NewsService {

    private final String API_KEY = "d3869fed1ee249bc975d21cad454267b";

    public String getTechNews() {
        String url = "https://newsapi.org/v2/top-headlines?category=technology&language=en&apiKey=" + API_KEY;

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, String.class);
    }
}