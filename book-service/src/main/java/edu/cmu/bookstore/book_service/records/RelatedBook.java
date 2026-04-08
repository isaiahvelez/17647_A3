package edu.cmu.bookstore.book_service.records;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RelatedBook {

    @JsonProperty("ISBN")
    private final String isbn;
    private final String title;
    @JsonProperty("Author")
    private final String author;

    public RelatedBook(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
    }

    @JsonProperty("ISBN")
    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    @JsonProperty("Author")
    public String getAuthor() {
        return author;
    }
}
