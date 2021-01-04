package com.talentica.arangodb.service;

import com.talentica.arangodb.entity.Book;
import com.talentica.arangodb.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class BookLoaderService {
    @Autowired
    private BookRepository bookRepository;
    public void saveBookBypassingTransaction(Book book){
        book.setUuid(UUID.randomUUID().toString());
        bookRepository.save(book);
    }

    public void save(Book book){
        bookRepository.saveToTransaction(book);
    }
    public void saveBothBooks(Book b1, Book b2){
        bookRepository.saveToTransaction(b1);
        bookRepository.saveToTransaction(b2);
    }
}
