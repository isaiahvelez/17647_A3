package edu.cmu.bookstore.book_service.records;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "Book")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Book {

    // Book Fields 
    @Id
    @JsonProperty("ISBN")
    private String ISBN;
    private String title;
    @JsonProperty("Author")
    private String Author;
    private String description;
    private String genre;
    private Float price;
    private Integer quantity;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    // Required by JPA
    protected Book() {
    }

    public Book(String ISBN, String title, String Author, String description, String genre, Float price,
            Integer quantity, String summary) {
        this.ISBN = ISBN;
        this.title = title;
        this.Author = Author;
        this.description = description;
        this.genre = genre;
        this.price = price;
        this.quantity = quantity;
        this.summary = summary;
    }

    public String ISBN() {
        return ISBN;
    }

    public String getISBN() {
        return ISBN;
    }

    @JsonProperty("ISBN")
    public void setISBN(String ISBN) {
        this.ISBN = ISBN;
    }

    public String title() {
        return title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String Author() {
        return Author;
    }

    public String getAuthor() {
        return Author;
    }

    @JsonProperty("Author")
    public void setAuthor(String Author) {
        this.Author = Author;
    }

    public String description() {
        return description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String genre() {
        return genre;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Float price() {
        return price;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Integer quantity() {
        return quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String summary() {
        return summary;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
