package ru.shsh.yappysearchservice.services;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ElasticsearchService {
    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    public void indexVideo(String videoUrl, String description, String actionData, String textData, String volumeData) throws Exception {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("video_url", videoUrl);
        jsonMap.put("description", description);

        // Преобразование JSON строк в Map для добавления в индекс
        Map<String, Object> actionMap = objectMapper.readValue(actionData, HashMap.class);
        Map<String, Object> textMap = objectMapper.readValue(textData, HashMap.class);
        Map<String, Object> volumeMap = objectMapper.readValue(volumeData, HashMap.class);

        jsonMap.put("actions", actionMap);
        jsonMap.put("text", textMap);
        jsonMap.put("audio_transcript", volumeMap);

        IndexRequest indexRequest = new IndexRequest("videos")
                .source(objectMapper.writeValueAsString(jsonMap), XContentType.JSON);

        client.index(indexRequest, RequestOptions.DEFAULT);
    }

    public List<Map<String, Object>> searchVideos(String query) throws Exception {
        SearchRequest searchRequest = new SearchRequest("videos");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // Используем multi_match для поиска по нескольким полям
        MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(query,
                        "description",
                        "actions.results.description",  // путь к вложенному полю
                        "text.coherent_text",  // путь к вложенному полю
                        "audio_transcript.text")  // путь к вложенному полю
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX); // Учитываем фразу целиком

        searchSourceBuilder.query(multiMatchQuery);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        List<Map<String, Object>> results = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Map<String, Object> source = new HashMap<>();
            source.put("video_url", hit.getSourceAsMap().get("video_url"));
            source.put("description", hit.getSourceAsMap().get("description"));
            results.add(source);
        }

        return results;
    }
}
