package ru.shsh.yappysearchservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.shsh.yappysearchservice.services.ElasticsearchService;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final ElasticsearchService elasticsearchService;
    @GetMapping("/")
    public String home() {
        return "search";
    }
    @GetMapping("/search")
    public String search(@RequestParam("query") String query, Model model) {
        try {
            List<Map<String, Object>> results = elasticsearchService.searchVideos(query);
            model.addAttribute("videos", results);
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while searching for videos: " + e.getMessage());
        }
        return "search";
    }
}
