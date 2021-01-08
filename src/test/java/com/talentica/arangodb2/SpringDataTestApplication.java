package com.talentica.arangodb2;

import com.arangodb.springframework.config.ArangoConfiguration;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.ArangoEntityWriter;
import com.talentica.arangodb.ArangoTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ComponentScan(basePackages = {"com.talentica.arangodb2"})
@EnableTransactionManagement
public class SpringDataTestApplication {

    @Bean
    public PlatformTransactionManager getTransactionManager(){
        return new ArangoTransactionManager();
    }

}
