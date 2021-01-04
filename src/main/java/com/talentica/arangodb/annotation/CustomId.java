package com.talentica.arangodb.annotation;

import com.talentica.arangodb.idprovider.IdProvider;
import com.talentica.arangodb.idprovider.UUID;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CustomId {
    public Class<? extends IdProvider> provider() default UUID.class;
}
