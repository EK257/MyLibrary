package com.example.mylibrary.controller;

import com.example.mylibrary.model.Book;
import com.example.mylibrary.model.Collection;
import com.example.mylibrary.repository.BookRepository;
import com.example.mylibrary.repository.CollectionRepository;
import com.example.mylibrary.service.BookService;
import com.example.mylibrary.service.IsbnService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class BookController {

    private final BookRepository bookRepository;
    private final BookService bookService;
    private final IsbnService isbnService;
    private final CollectionRepository collectionRepository;

    @Value("${app.upload.dir}")
    private String UPLOAD_DIR;

    public BookController(BookRepository bookRepository, BookService bookService, IsbnService isbnService, CollectionRepository collectionRepository) {
        this.bookRepository = bookRepository;
        this.bookService = bookService;
        this.isbnService = isbnService;
        this.collectionRepository = collectionRepository;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "bookType", required = false) List<String> bookTypes,
            @RequestParam(value = "status", required = false) List<String> statuses,
            @RequestParam(value = "genre", required = false) List<String> genres,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "sortBy", defaultValue = "title") String sortBy,
            Model model) {

        // Выбор способа сортировки книг
        Sort sort;
        switch (sortBy) {
            case "rating":
                sort = Sort.by(Sort.Direction.DESC, "rating");
                break;
            case "status":
                sort = Sort.by(Sort.Direction.ASC, "status");
                break;
            case "addedAt":
                sort = Sort.by(Sort.Direction.DESC, "addedAt");
                break;
            case "title":
            default:
                sort = Sort.by(Sort.Direction.ASC, "title");
                break;
        }

        // Подготовка параметров фильтрации
        List<String> typesFilter = (bookTypes != null && !bookTypes.isEmpty()) ? bookTypes : null;
        List<String> statusesFilter = (statuses != null && !statuses.isEmpty()) ? statuses : null;
        List<String> genresFilter = (genres != null && !genres.isEmpty()) ? genres : null;
        List<String> tagsFilter = (tags != null && !tags.isEmpty()) ? tags : null;
        String keywordFilter = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        // Получение списка книг с учетом фильтров и сортировки
        List<Book> filteredBooks = bookRepository.findFilteredBooks(keywordFilter, typesFilter, statusesFilter, genresFilter, tagsFilter, sort);

        // Список тегов
        List<String> allAvailableTags = Arrays.asList("Хобби", "Для работы", "Саморазвитие", "Учеба", "Отдых");

        model.addAttribute("books", filteredBooks);
        model.addAttribute("allCollections", collectionRepository.findAll());
        model.addAttribute("allAvailableTags", allAvailableTags);
        model.addAttribute("activeTypes", bookTypes);
        model.addAttribute("activeStatuses", statuses);
        model.addAttribute("activeGenres", genres);
        model.addAttribute("activeTags", tags);
        model.addAttribute("keyword", keywordFilter);
        model.addAttribute("sortBy", sortBy);

        return "index";
    }

    // Добавление книги
    @GetMapping("/books/add")
    public String showAddForm(
            @RequestParam(value = "searchQuery", required = false)
            String searchQuery,
            Model model) {

        // Выполнение поиска книги в OpenLibrary
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            List<Book> searchResults = bookService.searchOpenLibrary(searchQuery);

            model.addAttribute("searchResults", searchResults);
            model.addAttribute("searchQuery", searchQuery);
        }

        return "add";
    }

    // Сохранение новой книги
    @PostMapping("/books/add/save")
    public String saveNewBook(
            @ModelAttribute Book book,
            @RequestParam(value = "coverFile", required = false)
            MultipartFile coverFile,
            @RequestParam(value = "pdfFile", required = false)
            MultipartFile pdfFile,
            @RequestParam(value = "audioFile", required = false)
            MultipartFile audioFile) {

        try {
            // Сохранение обложки
            if (coverFile != null && !coverFile.isEmpty()) {
                book.setCoverImage(saveUploadedFile(coverFile));
            }
            // Сохранение файла электронной книги
            if ("EBOOK".equals(book.getBookType()) && pdfFile != null && !pdfFile.isEmpty()) {
                book.setFilePath(saveUploadedFile(pdfFile));
            }
            // Сохранение файла аудиокниги
            if ("AUDIO".equals(book.getBookType()) && audioFile != null && !audioFile.isEmpty()) {
                book.setFilePath(saveUploadedFile(audioFile));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Сохранение книги
        bookRepository.save(book);

        return "redirect:/";
    }

    // Отображение информации о книге
    @GetMapping("/books/{id}")
    public String bookDetails(
            @PathVariable("id") Long id,
            Model model) {

        Book book = bookRepository.findById(id).orElseThrow();
        model.addAttribute("book", book);

        return "book-details";
    }

    // Обновление информации о книге
    @PostMapping("/books/update/{id}")
    public String updateBook(
            @PathVariable("id") Long id,
            @ModelAttribute Book formBook,
            @RequestParam(value = "tags", required = false)
            List<String> tags,
            @RequestParam(value = "coverFile", required = false)
            MultipartFile coverFile,
            @RequestParam(value = "pdfFile", required = false)
            MultipartFile pdfFile,
            @RequestParam(value = "audioFile", required = false)
            MultipartFile audioFile,
            @RequestParam(value = "deleteFile", defaultValue = "false")
            boolean deleteFile) {

        Book existingBook = bookRepository.findById(id).orElseThrow();

        // Обновление основных данных книги
        existingBook.setBookType(formBook.getBookType());
        existingBook.setStatus(formBook.getStatus());
        existingBook.setGenre(formBook.getGenre());
        existingBook.setRating(formBook.getRating());
        existingBook.setReview(formBook.getReview());
        existingBook.setCoverUrl(formBook.getCoverUrl());
        existingBook.setPagesCount(formBook.getPagesCount());
        if (tags != null) {
            existingBook.setTags(tags);
        } else {
            existingBook.getTags().clear();
        }

        // Удаление прикрепленного файла
        if (deleteFile) {
            existingBook.setFilePath(null);
        }

        try {
            // Обновление обложки
            if (coverFile != null && !coverFile.isEmpty()) {
                existingBook.setCoverImage(saveUploadedFile(coverFile));
            }
            // Обновление файла электронной книги
            if ("EBOOK".equals(formBook.getBookType()) && pdfFile != null && !pdfFile.isEmpty()) {
                existingBook.setFilePath(saveUploadedFile(pdfFile)
                );
            }
            // Обновление файла аудиокниги
            if ("AUDIO".equals(formBook.getBookType()) && audioFile != null && !audioFile.isEmpty()) {
                existingBook.setFilePath(saveUploadedFile(audioFile)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Сохранение изменений
        bookRepository.save(existingBook);

        return "redirect:/books/" + id;
    }
    // Удаление книги из библиотеки
    @PostMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable("id") Long id) {
        bookRepository.deleteById(id);
        return "redirect:/";
    }
    // Формирование статистики по библиотеке
    @GetMapping("/analytics")
    public String getAnalytics(Model model) {
        List<Book> allBooks = bookRepository.findAll();
        // Общая статистика по книгам
        long totalBooks = allBooks.size();
        long readingBooks = allBooks.stream().filter(b -> "READING".equals(b.getStatus())).count();
        long completedBooks = allBooks.stream().filter(b -> "COMPLETED".equals(b.getStatus())).count();

        // Список запланированных книг
        List<Book> unreadBooks = allBooks.stream().filter(b -> "IN_PLANS".equals(b.getStatus())).collect(Collectors.toList());

        // Данные для диаграмм
        long paperCount = allBooks.stream().filter(b -> "PAPER".equals(b.getBookType()) || "Бумажная".equals(b.getBookType())).count();
        long ebookCount = allBooks.stream().filter(b -> "EBOOK".equals(b.getBookType()) || "Электронная".equals(b.getBookType())).count();
        long audioCount = allBooks.stream().filter(b -> "AUDIO".equals(b.getBookType()) || "Аудиокнига".equals(b.getBookType())).count();

        List<Long> formatData = Arrays.asList(paperCount, ebookCount, audioCount);

        Map<String, Long> genresMap = allBooks.stream().filter(b -> b.getGenre() != null && !b.getGenre().trim().isEmpty()).collect(Collectors.groupingBy(Book::getGenre, Collectors.counting()));

        // Передача статистики на страницу аналитики
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("readingBooks", readingBooks);
        model.addAttribute("completedBooks", completedBooks);
        model.addAttribute("unreadBooks", unreadBooks);
        model.addAttribute("genreLabels", genresMap.keySet());
        model.addAttribute("genreData", genresMap.values());
        model.addAttribute("formatData", formatData);

        return "analytics";
    }

    // Отображение всех коллекций
    @GetMapping("/collections")
    public String showCollections(Model model) {
        model.addAttribute("collections", collectionRepository.findAll());

        return "collections";
    }

    // Добавление книги в одну или несколько коллекций
    @PostMapping("/collections/add-book")
    public String addBookToCollections(
            @RequestParam("bookId") Long bookId,
            @RequestParam(required = false)
            List<Long> collectionIds) {

        if (collectionIds != null) {
            Book book = bookRepository.findById(bookId).orElseThrow();

            for (Long collId : collectionIds) {
                Collection coll = collectionRepository.findById(collId).orElseThrow();

                if (!coll.getBooks().contains(book)) {
                    coll.getBooks().add(book);
                    collectionRepository.save(coll);
                }
            }
        }

        return "redirect:/";
    }

    // Создание новой пользовательской коллекции
    @PostMapping("/collections/create")
    public String createCollection(@RequestParam("name") String name) {
        if (name != null && !name.trim().isEmpty()) {
            Collection collection = new Collection();
            collection.setName(name.trim());
            collectionRepository.save(collection);
        }

        return "redirect:/collections";
    }

    // Отображение содержимого коллекции
    @GetMapping("/collections/{id}")
    public String showCollectionDetails(
            @PathVariable("id") Long id,
            Model model) {

        Collection collection = collectionRepository.findById(id).orElseThrow();
        model.addAttribute("collection", collection);

        return "collection-details";
    }

    // Удаление книги из коллекции
    @PostMapping("/collections/{collId}/remove/{bookId}")
    public String removeBookFromCollection(
            @PathVariable("collId") Long collId,
            @PathVariable("bookId") Long bookId) {

        Collection collection = collectionRepository.findById(collId).orElseThrow();
        Book book = bookRepository.findById(bookId).orElseThrow();

        if (collection.getBooks().contains(book)) {
            collection.getBooks().remove(book);
            collectionRepository.save(collection);
        }

        return "redirect:/collections/" + collId;
    }

    // Удаление коллекции
    @PostMapping("/collections/{id}/delete")
    public String deleteCollection(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            if (collectionRepository.existsById(id)) {
                collectionRepository.deleteById(id);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return "redirect:/collections";
    }

    // Сохранение загруженного файла
    private String saveUploadedFile(MultipartFile file) throws Exception {
        // Создание директории для хранения файлов
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        // Генерация уникального имени файла
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);
        // Копирование файла на диск
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/uploads/" + fileName;
    }
}
