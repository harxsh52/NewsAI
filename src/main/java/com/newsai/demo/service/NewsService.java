package com.newsai.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NewsService {

    private final String API_KEY = "";

    public String getTechNews() {
        String url = "https://newsapi.org/v2/top-headlines?category=technology&language=en&apiKey=" + API_KEY;

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, String.class);
    }
}