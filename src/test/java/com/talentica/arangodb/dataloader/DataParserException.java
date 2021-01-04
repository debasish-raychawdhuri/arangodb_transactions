package com.talentica.arangodb.dataloader;

import java.io.IOException;

public class DataParserException extends IOException {
    public DataParserException(String message){
        super(message);
    }
    public DataParserException(Exception source){
        super(source);
    }
}
