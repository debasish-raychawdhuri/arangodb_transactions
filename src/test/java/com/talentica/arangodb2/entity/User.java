package com.talentica.arangodb2.entity;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Rev;
import com.talentica.arangodb.annotation.CustomId;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Data
@Document("Users")
public class User {
    @Id
    private String id;
    @CustomId
    private String uuid;
    private String name;
    private String favoriteBook;
    @Rev
    private String version;
}
