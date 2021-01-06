package com.talentica.arangodb2.dataloader;

import java.io.IOException;

public class DataParserException extends IOException {
    public DataParserException(String message){
        super(message);
    }
    public DataParserException(Exception source){
        super(source);
    }
}
