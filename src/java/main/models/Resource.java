package main.models;

public class Resource {
    private String type;
    private String title;
    private String link;
    private String folderId;
    private String workbinId;
    private String fileId;

    public Resource(String type, String title, String link, String folderId, String workbinId, String fileId) {
        this.type = type;
        this.title = title;
        this.link = link;
        this.folderId = folderId;
        this.workbinId = workbinId;
        this.fileId = fileId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getWorkbinId() {
        return workbinId;
    }

    public void setWorkbinId(String workbinId) {
        this.workbinId = workbinId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
