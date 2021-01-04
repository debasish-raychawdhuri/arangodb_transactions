package com.talentica.arangodb;

import com.arangodb.springframework.config.ArangoConfiguration;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.ArangoEntityWriter;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;

@ComponentScan(basePackages = "com.talentica.arangodb")
@EnableTransactionManagement
public class SpringDataTestApplication {

    @Bean
    public PlatformTransactionManager getTransactionManager(
            ArangoOperations arangoOperations,
            ArangoEntityWriter writer,
            ArangoConverter converter,
            ArangoConfiguration arangoConfiguration){
        return new ArangoTransactionManager(arangoOperations,writer,converter,arangoConfiguration);
    }

}
