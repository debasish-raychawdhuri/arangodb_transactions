package com.talentica.arangodb.entity;

import com.arangodb.springframework.annotation.Document;
import com.talentica.arangodb.annotation.CustomId;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.UUID;

@Data
@Document("Books")
public class Book {
    @Data
    public static class Format{
        private String url;
        private String format;
        private long extent;
        private Date modified;

    }
    @Data
    public static class Creator{
        private String name;
        private int birthDate;
        private int deathDate;
        private String alias;
        private String webpage;
    }
    @Id
    private String id;
    @CustomId
    private String uuid;
    private String title;
    private String [] subjects;
    private String [] bookshelves;
    private Format [] formats;
    private Creator[] creators;
    private long downloads;

    private String _rev;

}
