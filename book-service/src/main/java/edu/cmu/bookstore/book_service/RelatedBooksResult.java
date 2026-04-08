package edu.cmu.bookstore.book_service;

import java.util.List;

import edu.cmu.bookstore.book_service.records.RelatedBook;

public record RelatedBooksResult(RelatedBooksStatus status, List<RelatedBook> books) {
}
