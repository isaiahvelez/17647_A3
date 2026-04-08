package edu.cmu.bookstore.book_service;

import org.springframework.data.repository.CrudRepository;

import edu.cmu.bookstore.book_service.records.Book;

// Book Repository
public interface BookRepository extends CrudRepository<Book, String> {
}
