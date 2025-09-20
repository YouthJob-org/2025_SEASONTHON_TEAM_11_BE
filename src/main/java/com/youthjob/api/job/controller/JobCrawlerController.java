package com.youthjob.api.job.controller;

import com.youthjob.api.job.service.Work24CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/jobs")
@RequiredArgsConstructor
public class JobCrawlerController {

    private final Work24CrawlerService service;

    @PostMapping("/crawl")
    public String crawlJobs() throws Exception {
        int saved = service.crawlAndSave();
        return "Saved " + saved + " job postings.";
    }
}
