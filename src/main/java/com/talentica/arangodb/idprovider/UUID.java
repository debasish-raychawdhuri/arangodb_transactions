package com.talentica.arangodb.idprovider;

public class UUID implements IdProvider{
    @Override
    public Object newUniqueID(Class<?> fieldType, Object entity) {
        if(fieldType == String.class) {
            return java.util.UUID.randomUUID().toString();
        }else if(fieldType == java.util.UUID.class){
            return java.util.UUID.randomUUID();
        }else{
            throw new IllegalArgumentException("Unsupported field type for UUID: "+fieldType);
        }
    }
}
