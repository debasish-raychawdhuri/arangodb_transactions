package com.talentica.arangodb.idprovider;

public interface IdProvider {
    public Object newUniqueID(Class<?> fieldType, Object entity);
}
