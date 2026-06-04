package com.example.mylibrary.service;

import com.example.mylibrary.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Book> searchOpenLibrary(String query) {

        List<Book> foundBooks = new ArrayList<>();

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://openlibrary.org/search.json?q=" + encodedQuery + "&limit=5";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode docs = objectMapper.readTree(response).path("docs");

            if (docs.isArray()) {
                for (JsonNode doc : docs) {
                    Book book = new Book();

                    // Название книги
                    book.setTitle(doc.path("title").asText("Без названия"));

                    // Автор книги
                    JsonNode authorNode = doc.path("author_name");
                    book.setAuthor((authorNode.isArray() && authorNode.size() > 0) ? authorNode.get(0).asText() : "Неизвестный автор");

                    // Год
                    book.setYear(doc.has("first_publish_year") ? doc.path("first_publish_year").asInt() : 0);

                    // Обложка книги
                    String coverUrl = "";

                    if (doc.has("cover_i")) {
                        coverUrl = "https://covers.openlibrary.org/b/id/" + doc.path("cover_i").asText() + "-L.jpg";
                    }
                    book.setCoverUrl(coverUrl);

                    book.setGenre("Другое");
                    foundBooks.add(book);
                }
            }

        } catch (Exception e) {
            System.out.println("API error: " + e.getMessage());
        }

        return foundBooks;
    }
}