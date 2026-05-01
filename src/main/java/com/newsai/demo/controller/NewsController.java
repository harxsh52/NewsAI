package com.newsai.demo.controller;

import com.newsai.demo.model.NewsArticle;
import com.newsai.demo.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @GetMapping
    public List<NewsArticle> getNews() {
        return newsService.getTechNews();
    }
}