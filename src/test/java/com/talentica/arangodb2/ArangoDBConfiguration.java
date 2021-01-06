package com.talentica.arangodb2;

import com.arangodb.ArangoDB;
import com.arangodb.springframework.annotation.EnableArangoRepositories;
import com.arangodb.springframework.config.ArangoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableArangoRepositories({"com.talentica.arangodb2.repository","com.talentica.arangodb.repository"})
public class ArangoDBConfiguration implements ArangoConfiguration {
    @Override
    public ArangoDB.Builder arango() {
        return new ArangoDB.Builder().host("localhost", 8529).user("root").password("password");
    }

    @Override
    public String database() {
        return "test";
    }
}
