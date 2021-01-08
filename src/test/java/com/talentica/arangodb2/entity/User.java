package com.talentica.arangodb2.entity;

import com.arangodb.springframework.annotation.Document;
import com.talentica.arangodb.annotation.CustomId;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Document("Users")
public class User {
    @Id
    private String id;
    @CustomId
    private String uuid;
    private String name;
    private String favoriteBook;
}
