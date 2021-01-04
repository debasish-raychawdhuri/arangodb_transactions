package com.talentica.arangodb.repository;

import com.talentica.arangodb.ArangoTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

public class ArangoTransactionalRepositoryImpl<T, ID> implements ArangoTransactionalRepository<T, ID> {
    @Autowired
    private PlatformTransactionManager transactionManager;


    public <S extends T> S saveToTransaction(final S entity) {
        S result;
        if (transactionManager instanceof ArangoTransactionManager) {
            var tm = (ArangoTransactionManager) transactionManager;
            tm.saveToTransaction(entity);
            result = entity;
        } else {
            throw new IllegalStateException("The transaction manager must be a "
                    + ArangoTransactionManager.class);
        }
        return result;
    }

}
