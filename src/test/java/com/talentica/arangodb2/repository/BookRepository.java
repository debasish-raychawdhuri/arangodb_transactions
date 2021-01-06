package com.talentica.arangodb2.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.talentica.arangodb2.entity.Book;
import com.talentica.arangodb.repository.ArangoTransactionalRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends ArangoTransactionalRepository<Book,String>, ArangoRepository<Book,String>{
    public Book findByUuid(String uuid);
}
