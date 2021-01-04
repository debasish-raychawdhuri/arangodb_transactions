package com.talentica.arangodb.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import org.springframework.data.repository.NoRepositoryBean;

public interface ArangoTransactionalRepository<T,ID> {
    public <S extends T> S saveToTransaction(final S entity);
}
