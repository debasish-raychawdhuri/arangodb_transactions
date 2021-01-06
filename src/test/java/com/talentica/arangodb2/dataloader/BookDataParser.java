package com.talentica.arangodb2.dataloader;


import com.talentica.arangodb2.entity.Book;

public interface BookDataParser {
    public Iterable<Book> parseBook() throws DataParserException;
}
