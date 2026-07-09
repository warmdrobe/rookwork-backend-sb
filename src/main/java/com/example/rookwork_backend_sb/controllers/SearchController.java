package com.example.rookwork_backend_sb.controllers;

import com.example.rookwork_backend_sb.dtos.search.SearchResponseDto;
import com.example.rookwork_backend_sb.services.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponseDto> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(searchService.search(query));
    }
}
