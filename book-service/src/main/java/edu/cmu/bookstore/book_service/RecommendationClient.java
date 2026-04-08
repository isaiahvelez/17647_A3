package edu.cmu.bookstore.book_service;

import java.util.List;

import edu.cmu.bookstore.book_service.records.RelatedBook;

public interface RecommendationClient {

    List<RelatedBook> fetchRelatedBooks(String isbn);
}
