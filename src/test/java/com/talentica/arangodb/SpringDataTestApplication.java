package com.talentica.arangodb;

import com.arangodb.springframework.config.ArangoConfiguration;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.ArangoEntityWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
