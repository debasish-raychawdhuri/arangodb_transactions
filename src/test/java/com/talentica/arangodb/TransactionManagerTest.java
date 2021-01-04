package com.talentica.arangodb;


import com.arangodb.ArangoDBException;
import com.arangodb.springframework.core.convert.ArangoEntityWriter;
import com.talentica.arangodb.dataloader.BookRdfParser;
import com.talentica.arangodb.repository.BookRepository;
import com.talentica.arangodb.service.BookLoaderService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.AssertionErrors;

import java.io.File;

public class TransactionManagerTest {

    @Test
    public void testOptimisticLockingSuccess() throws Exception {
        var stream = TransactionManagerTest.class.getResourceAsStream("/pg25519.rdf");
        BookRdfParser parser = new BookRdfParser(new File(""));
        var book = parser.parseBook(stream);
        assert (book.getTitle().equals("Little Wizard Stories of Oz"));
        var appContext = new AnnotationConfigApplicationContext(SpringDataTestApplication.class);
        String[] beanNames = appContext.getBeanDefinitionNames();
        var bookService = appContext.getBean(BookLoaderService.class);
        bookService.save(book);
        var bookRepo = appContext.getBean(BookRepository.class);
       // book = bookRepo.findByUuid(book.getUuid());
        book.setDownloads(1000);
        bookService.saveBookBypassingTransaction(book);

    }

    @Test
    public void testOptimisticLocking() throws Exception {
        var stream = TransactionManagerTest.class.getResourceAsStream("/pg25519.rdf");
        BookRdfParser  parser = new BookRdfParser(new File(""));
        var book = parser.parseBook(stream);
        assert (book.getTitle().equals("Little Wizard Stories of Oz"));
        var appContext = new AnnotationConfigApplicationContext(SpringDataTestApplication.class);
        String[] beanNames = appContext.getBeanDefinitionNames();
        var bookService = appContext.getBean(BookLoaderService.class);
        bookService.saveBookBypassingTransaction(book);
        book.setVersion("_bmNZa4C---");
        book.setDownloads(1000);
        try {
            bookService.save(book);
            AssertionErrors.fail("Must throw exception here");
        }catch (ArangoDBException ex){

        }catch (Exception ex){
            AssertionErrors.fail("Wrong exception "+ex);
        }
        System.out.println(book);
    }

    @Test
    public void testTransactionFailure() throws Exception{
        var appContext = new AnnotationConfigApplicationContext(SpringDataTestApplication.class);
        ArangoEntityWriter entityWriter = appContext.getBean(ArangoEntityWriter.class);
        AssertionErrors.assertTrue("could not create entity writer",entityWriter!=null);

        var stream = TransactionManagerTest.class.getResourceAsStream("/pg25519.rdf");
        var stream2 = TransactionManagerTest.class.getResourceAsStream("/pg15.rdf");
        BookRdfParser parser = new BookRdfParser(new File(""));
        var book = parser.parseBook(stream);
        var book2 = parser.parseBook(stream2);

        var bookService = appContext.getBean(BookLoaderService.class);
        var bookRepo = appContext.getBean(BookRepository.class);
        bookService.save(book);
        book = bookRepo.findByUuid(book.getUuid());
        book.setDownloads(1);
        book.setVersion("_bmNZa4C---");
        try {
            bookService.saveBothBooks(book,book2);
            AssertionErrors.assertFalse("This should not be reached", true);
        }catch(ArangoDBException ex){

        }
        var book3 = bookRepo.findByUuid(book.getUuid());
        var book4 = bookRepo.findByUuid(book2.getUuid());
        AssertionErrors.assertNull("Book2 must not have been commited", book4);
        AssertionErrors.assertFalse("Book1 must not have been updated", book3.getDownloads()==1);
    }
    @Test
    public void testTransactionSuccess() throws Exception{
        var appContext = new AnnotationConfigApplicationContext(SpringDataTestApplication.class);
        ArangoEntityWriter entityWriter = appContext.getBean(ArangoEntityWriter.class);
        AssertionErrors.assertTrue("could not create entity writer",entityWriter!=null);

        var stream = TransactionManagerTest.class.getResourceAsStream("/pg25519.rdf");
        var stream2 = TransactionManagerTest.class.getResourceAsStream("/pg15.rdf");
        BookRdfParser parser = new BookRdfParser(new File(""));
        var book = parser.parseBook(stream);
        var book2 = parser.parseBook(stream2);

        var bookService = appContext.getBean(BookLoaderService.class);
        var bookRepo = appContext.getBean(BookRepository.class);
        bookService.save(book);
        book = bookRepo.findByUuid(book.getUuid());

        book.setDownloads(1);
        //book.set_rev("_bmNZa4C---");
        try {
            bookService.saveBothBooks(book,book2);

        }catch(Exception ex){
            AssertionErrors.assertFalse("This should not be reached", true);
        }
        var book3 = bookRepo.findByUuid(book.getUuid());
        var book4 = bookRepo.findByUuid(book2.getUuid());
        AssertionErrors.assertNotNull("Book2 must have been commited", book4);
        AssertionErrors.assertTrue("Book1 must have been updated", book3.getDownloads()==1);
    }
}
