package com.example.mylibrary.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.List;

@Service
public class IsbnService {
    private final RestTemplate restTemplate = new RestTemplate();

    public BookMetadata fetchMetadataByIsbn(String isbn) {
        isbn = isbn.replaceAll("[^0-9X]", "");

        // OpenLibrary
        try {
            String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            String key = "ISBN:" + isbn;

            if (response != null && response.containsKey(key)) {
                Map<String, Object> bookData = (Map<String, Object>) response.get(key);

                // Название книги
                String title = (String) bookData.get("title");

                // Автор
                List<Map<String, String>> authors = (List<Map<String, String>>) bookData.get("authors");
                String author = (authors != null && !authors.isEmpty()) ? authors.get(0).get("name") : "Неизвестный автор";

                // Обложка книги
                String coverUrl = null;

                if (bookData.containsKey("cover")) {
                    Map<String, String> cover = (Map<String, String>) bookData.get("cover");
                    coverUrl = cover.get("large");
                }

                return new BookMetadata(title, author, coverUrl);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Google Books
        try {
            String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                if (!items.isEmpty()) {
                    Map<String, Object> volumeInfo = (Map<String, Object>) items.get(0).get("volumeInfo");

                    // Название книги
                    String title = (String) volumeInfo.get("title");

                    // Автор
                    List<String> authors = (List<String>) volumeInfo.get("authors");

                    String author = (authors != null && !authors.isEmpty()) ? authors.get(0) : "Неизвестный автор";

                    // Обложка
                    String coverUrl = null;

                    if (volumeInfo.containsKey("imageLinks")) {
                        Map<String, String> imageLinks = (Map<String, String>) volumeInfo.get("imageLinks");
                        coverUrl = imageLinks.get("thumbnail");
                    }

                    return new BookMetadata(title, author, coverUrl);
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    public record BookMetadata(
            String title,
            String author,
            String coverUrl
    ) {}
}