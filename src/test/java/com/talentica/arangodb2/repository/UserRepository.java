package com.talentica.arangodb2.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.talentica.arangodb.repository.ArangoTransactionalRepository;
import com.talentica.arangodb2.entity.User;

public interface UserRepository extends ArangoRepository<User, String>, ArangoTransactionalRepository<User,String> {
    public User findByUuid(String uuid);
}
