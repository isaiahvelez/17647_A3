package edu.cmu.bookstore.book_service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import edu.cmu.bookstore.book_service.records.Book;

@Component
public class Validation {

    // Book Validation
    public Boolean validBook(Book testBook) {

        // Are all the fields there?
        if (
            testBook.ISBN() == null        ||
            testBook.title() == null       ||
            testBook.Author() == null      ||
            testBook.description() == null ||
            testBook.genre() == null       ||
            testBook.price() == null       ||
            testBook.quantity() == null
        ) {

            // Not all the fields are there
            return false;
        }

        // Is the price negative?
        if (testBook.price() < 0) {
            return false;
        }

        BigDecimal priceDecimal = new BigDecimal(testBook.price().toString());

        // Is the price valid?
        if (priceDecimal.scale() > 2) {

            // Price is not valid
            return false;
        }

        return true;

    }
}
