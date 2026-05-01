package com.newsai.demo.model;

import lombok.Data;

@Data
public class NewsArticle {
    private String title;
    private String description;
    private String url;
    private String summary;
    private String whyItMatters;
}