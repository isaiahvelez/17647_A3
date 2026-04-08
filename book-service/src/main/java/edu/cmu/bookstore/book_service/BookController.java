package edu.cmu.bookstore.book_service;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import edu.cmu.bookstore.book_service.records.Book;

@RestController
public class BookController {

    private final BookRepository bookRepo;
    private final LlmService llmService;
    private final Validation validation;
    private final RelatedBooksService relatedBooksService;

    public BookController(
        BookRepository bookRepo, 
        LlmService llmService, 
        Validation validation,
        RelatedBooksService relatedBooksService
    ) {
        this.bookRepo = bookRepo;
        this.llmService = llmService;
        this.validation = validation;
        this.relatedBooksService = relatedBooksService;
    }

    // Add Book Endpoint
    @PostMapping("/books")
    public ResponseEntity<Object> addBook(@RequestBody Book newBook) {

        if (!validation.validBook(newBook)) {
            return ResponseEntity.status(400).build();
        }

        // Do we already have this Book?
        if (bookRepo.existsById(newBook.ISBN())) {
            return ResponseEntity
                .status(422)
                .body(java.util.Map.of("message", "This ISBN already exists in the system."));
        }

        String initialSummary = llmService.createInitialSummary(
            newBook.title(),
            newBook.Author(),
            newBook.description(),
            newBook.genre()
        );

        Book bookToSave = new Book(
            newBook.ISBN(),
            newBook.title(),
            newBook.Author(),
            newBook.description(),
            newBook.genre(),
            newBook.price(),
            newBook.quantity(),
            initialSummary
        );

        Book savedBook = bookRepo.save(bookToSave);
        llmService.generateAndSaveSummary(
            savedBook.ISBN(),
            savedBook.title(),
            savedBook.Author(),
            savedBook.description(),
            savedBook.genre()
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(savedBook.ISBN())
            .toUri();

        Book responseBook = new Book(
            savedBook.ISBN(),
            savedBook.title(),
            savedBook.Author(),
            savedBook.description(),
            savedBook.genre(),
            savedBook.price(),
            savedBook.quantity(),
            null
        );

        return ResponseEntity.created(location).body(responseBook);
    }

    // Update Book Endpoint
    @PutMapping("/books/{ISBN}")
    public ResponseEntity<Object> updateBook(
        @PathVariable String ISBN,
        @RequestBody Book updatedBook) {

        // Validation Check
        if (!validation.validBook(updatedBook)) {
            return ResponseEntity.status(400).build();
        }

        // ISBN in the payload must match the ISBN in the URL
        if (!ISBN.equals(updatedBook.ISBN())) {
            return ResponseEntity.status(400).build();
        }

        // Do we have this book?
        if (!bookRepo.existsById(ISBN)) {
            return ResponseEntity.status(404).build();
        }

        String existingSummary = bookRepo.findById(ISBN)
            .map(Book::summary)
            .orElse(null);

        Book bookToSave = new Book(
            ISBN,
            updatedBook.title(),
            updatedBook.Author(),
            updatedBook.description(),
            updatedBook.genre(),
            updatedBook.price(),
            updatedBook.quantity(),
            existingSummary
        );

        Book savedBook = bookRepo.save(bookToSave);

        Book responseBook = new Book(
            savedBook.ISBN(),
            savedBook.title(),
            savedBook.Author(),
            savedBook.description(),
            savedBook.genre(),
            savedBook.price(),
            savedBook.quantity(),
            null
        );

        return ResponseEntity.status(200).body(responseBook);
    }

    // Retrieve Book Endpoint
    @GetMapping({"/books/isbn/{ISBN}", "/books/{ISBN}"})
    public ResponseEntity<Book> retrieveBook(@PathVariable String ISBN) {

        return bookRepo.findById(ISBN)
            .map(book -> ResponseEntity.status(200).body(book))
            .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @GetMapping("/books/{ISBN}/related-books")
    public ResponseEntity<Object> relatedBooks(@PathVariable String ISBN) {
        RelatedBooksResult result = relatedBooksService.fetchRelatedBooks(ISBN);

        return switch (result.status()) {
            case SUCCESS -> ResponseEntity.ok(result.books());
            case NO_CONTENT -> ResponseEntity.noContent().build();
            case GATEWAY_TIMEOUT -> ResponseEntity.status(504).build();
            case CIRCUIT_OPEN -> ResponseEntity.status(503).build();
        };
    }

    // Status 
    @GetMapping(value = "/status", produces = "text/plain")
    public ResponseEntity<Object> status() {
        return ResponseEntity.status(200).body("OK");
    }
}
