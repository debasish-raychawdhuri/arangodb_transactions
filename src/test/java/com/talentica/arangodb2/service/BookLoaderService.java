package com.talentica.arangodb2.service;

import com.talentica.arangodb2.entity.Book;
import com.talentica.arangodb2.entity.User;
import com.talentica.arangodb2.repository.BookRepository;
import com.talentica.arangodb2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class BookLoaderService {
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private UserRepository userRepository;
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

    public void saveBookWithUser(Book book, User user){
        bookRepository.saveToTransaction(book);
        user.setFavoriteBook(book.getUuid());
        userRepository.saveToTransaction(user);
    }
}
