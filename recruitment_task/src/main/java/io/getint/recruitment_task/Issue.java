package io.getint.recruitment_task;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Issue {
    public String id;
    public Fields fields;

}

class Fields {
    public String self;
    public String key;
    public String summary;
    public String description;
    public Priority priority;
    public CommentList comment;
}

class CommentList {
    public Comment[] comments;
}

class Author {
    public String self;
    public String accountId;
    public String emailAddress;
    public String displayName;
    public boolean active;
    public String timeZone;
    public String accountType;

    public AvatarUrls avatarUrls;
}

class AvatarUrls {
    @JsonProperty("48x48")
    public String _48x48;

    @JsonProperty("24x24")
    public String _24x24;

    @JsonProperty("32x32")
    public String _32x32;

    @JsonProperty("16x16")
    public String _16x16;
}


