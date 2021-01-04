package com.talentica.arangodb.dataloader;


import com.talentica.arangodb.entity.Book;

public interface BookDataParser {
    public Iterable<Book> parseBook() throws DataParserException;
}
