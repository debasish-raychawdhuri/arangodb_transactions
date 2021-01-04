package com.talentica.arangodb.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.talentica.arangodb.entity.Book;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookRepository extends ArangoTransactionalRepository<Book,String>, ArangoRepository<Book,String>{
    public Book findByUuid(String uuid);
}
