package com.preptime.service;

import com.preptime.web.ApiException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Talks to the Python (LangChain + Gemini) service. Failures never break the app: callers get a fallback. */
@Component
public class AiClient {
    private final RestClient rest;

    public AiClient(@Value("${ai.service.url}") String url) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(60000);
        this.rest = RestClient.builder().baseUrl(url).requestFactory(f).build();
    }

    @SuppressWarnings("unchecked")
    public String chat(String topic, String subject, String message, List<Map<String, String>> history) {
        try {
            Map<String, Object> body = Map.of("topic", topic, "subject", subject, "message", message, "history", history);
            Map<String, Object> r = rest.post().uri("/chat").body(body).retrieve().body(Map.class);
            return r == null ? "" : String.valueOf(r.get("answer"));
        } catch (Exception e) {
            throw new ApiException(503, "The AI tutor is not available right now (check the Gemini key and quota). Please try again in a minute.");
        }
    }

    @SuppressWarnings("unchecked")
    public String planSummary(Map<String, Object> payload) {
        try {
            Map<String, Object> r = rest.post().uri("/plan-summary").body(payload).retrieve().body(Map.class);
            return r == null ? null : (String) r.get("summary");
        } catch (Exception e) {
            return null;
        }
    }
}
