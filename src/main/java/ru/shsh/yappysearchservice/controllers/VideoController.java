package ru.shsh.yappysearchservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.shsh.yappysearchservice.services.ElasticsearchService;
import ru.shsh.yappysearchservice.services.FirebaseService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VideoController {


    private final ElasticsearchService elasticsearchService;
    private final FirebaseService firebaseService;
    private final RestTemplate restTemplate;
    private static final Logger LOGGER = Logger.getLogger(VideoController.class.getName());

    public CompletableFuture<String> sendVideoToService1(String videoUrl) {
        return CompletableFuture.supplyAsync(() -> {
            String serviceUrl = "http://localhost:5000/process_video_url";
            Map<String, String> request = Map.of("video_url", videoUrl);
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(serviceUrl, request, String.class);
                return response.getBody();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error calling service 1", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> sendVideoFileToService1(MultipartFile videoFile) {
        return CompletableFuture.supplyAsync(() -> {
            String serviceUrl = "http://localhost:5000/process_video";
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("video", videoFile.getResource());
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(serviceUrl, body, String.class);
                return response.getBody();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error calling service 1", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> sendVideoUrlToServiceTextOnVideo(String videoUrl) {
        return CompletableFuture.supplyAsync(() -> {
            String serviceUrl = "http://localhost:5001/process_video_url";
            Map<String, String> request = Map.of("video_url", videoUrl);
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(serviceUrl, request, String.class);
                return response.getBody();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error calling service 2", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> sendVideoFileToServiceTextOnVideo(MultipartFile videoFile) {
        return CompletableFuture.supplyAsync(() -> {
            String serviceUrl = "http://localhost:5001/process_video";
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("video", videoFile.getResource());
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(serviceUrl, body, String.class);
                return response.getBody();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error calling service 2", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> processVideoOnService3(String videoUrl) {
        return CompletableFuture.supplyAsync(() -> {
            String serviceUrl = "http://localhost:5002/process_video_url";
            Map<String, String> request = Map.of("video_url", videoUrl);
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(serviceUrl, request, String.class);
                return response.getBody();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error calling service 3", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<String> processVideoFileOnService3(MultipartFile videoFile) {
        return CompletableFuture.supplyAsync(() -> {
            String serviceUrl = "http://localhost:5002/process_video";
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("video", videoFile.getResource());
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(serviceUrl, body, String.class);
                return response.getBody();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error calling service 3", e);
                throw new RuntimeException(e);
            }
        });
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(@RequestBody Map<String, String> request) {
        String videoUrl = request.get("videoUrl");
        String description = request.get("description");
        try {
            CompletableFuture<String> actionFuture = sendVideoToService1(videoUrl);
            CompletableFuture<String> textFuture = sendVideoUrlToServiceTextOnVideo(videoUrl);
            CompletableFuture<String> volumeFuture = processVideoOnService3(videoUrl);

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(actionFuture, textFuture, volumeFuture);

            CompletableFuture<Map<String, String>> combinedFuture = allFutures.thenApply(v -> {
                try {
                    String actionData = actionFuture.get();
                    String textData = textFuture.get();
                    String volumeData = volumeFuture.get();

                    elasticsearchService.indexVideo(videoUrl, description, actionData, textData, volumeData);

                    return Map.of("actionData", actionData, "textData", textData, "volumeData", volumeData);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error combining results", e);
                    throw new RuntimeException(e);
                }
            });

            combinedFuture.get();


            return ResponseEntity.ok("Upload successful and metadata processed");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Upload failed", e);
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/uploadVideo")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file,
                                              @RequestParam("description") String description) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Пожалуйста, выберите файл для загрузки.");
        }

        try {
            String urlUploadVideo = firebaseService.upload(file);

            // Отправка видео на три сервиса
            CompletableFuture<String> actionFuture = sendVideoFileToService1(file);
            CompletableFuture<String> textFuture = sendVideoFileToServiceTextOnVideo(file);
            CompletableFuture<String> volumeFuture = processVideoFileOnService3(file);

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(actionFuture, textFuture, volumeFuture);

            CompletableFuture<Map<String, String>> combinedFuture = allFutures.thenApply(v -> {
                try {
                    String actionData = actionFuture.get();
                    String textData = textFuture.get();
                    String volumeData = volumeFuture.get();

                    elasticsearchService.indexVideo(urlUploadVideo, description, actionData, textData, volumeData);

                    return Map.of("actionData", actionData, "textData", textData, "volumeData", volumeData);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error combining results", e);
                    throw new RuntimeException(e);
                }
            });

            combinedFuture.get();

            return ResponseEntity.ok("Видео успешно загружено и обработано.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Ошибка при загрузке видео", e);
            return ResponseEntity.status(500).body("Произошла ошибка при загрузке видео: " + e.getMessage());
        }
    }
}
