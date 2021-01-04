package com.talentica.arangodb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;

@ComponentScan(basePackages = "com.talentica.arangodb")
@EnableTransactionManagement
public class SpringDataTestApplication {


    @Bean
    public PlatformTransactionManager getTransactionManager(){
        return new ArangoTransactionManager();
    }

}
