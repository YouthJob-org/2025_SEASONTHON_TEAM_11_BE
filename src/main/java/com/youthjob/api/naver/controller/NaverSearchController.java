// src/main/java/com/youthjob/api/naver/controller/NaverSearchController.java
package com.youthjob.api.naver.controller;

import com.youthjob.api.naver.service.NaverSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/naver")
public class NaverSearchController {

    private final NaverSearchService naverSearchService;

    @GetMapping(value = "/blogs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> blogs(
            @RequestParam("q") String query,
            @RequestParam(value = "display", defaultValue = "5") int display,
            @RequestParam(value = "start", defaultValue = "1") int start,
            @RequestParam(value = "sort", defaultValue = "sim") String sort
    ) {
        String json = naverSearchService.searchBlogs(query, display, start, sort);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }
}
