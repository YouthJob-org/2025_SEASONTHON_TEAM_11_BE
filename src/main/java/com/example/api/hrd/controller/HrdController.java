package com.example.api.hrd.controller;

import com.example.api.hrd.dto.HrdCourseDto;
import com.example.api.hrd.service.HrdSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hrd")
public class HrdController {

    private final HrdSearchService searchService;

    @GetMapping("/courses")
    public ResponseEntity<List<HrdCourseDto>> search(
            @RequestParam String startDt,
            @RequestParam String endDt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String area1,
            @RequestParam(required = false) String ncs1,
            @RequestParam(defaultValue = "ASC") String sort,
            @RequestParam(defaultValue = "2") String sortCol
    ) {
        return ResponseEntity.ok(searchService.search(startDt, endDt, page, size, area1, ncs1, sort, sortCol));
    }

}