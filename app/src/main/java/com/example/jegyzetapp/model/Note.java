package com.example.jegyzetapp.model;

import java.util.Date;

public class Note {
    private String id;
    private String uid;
    private String title;
    private String content;
    private Date creationDate;

    public Note() {}

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Note(String id, String uid, String title, String content, Date creationDate) {
        this.id = id;
        this.uid = uid;
        this.title = title;
        this.content = content;
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
