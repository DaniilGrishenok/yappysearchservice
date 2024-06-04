package ru.shsh.yappysearchservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {

    @PostMapping("/api/ping")
    public ResponseEntity<String> dfd(){
        return ResponseEntity.ok("pong");
    }
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "query", required = false) String query, Model model) {
        model.addAttribute("query", query);
        // В реальном приложении сюда добавляется логика поиска и получение результатов
        return "search";
    }

    @GetMapping("/video")
    public String video() {
        return "video";
    }
}
