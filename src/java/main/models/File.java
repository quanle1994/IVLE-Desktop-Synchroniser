package main.models;

import java.util.Objects;

public class File implements NotificationItem{
    private String workbinId;
    private String moduleId;
    private String directory;
    private String id;
    private String fileName;
    private String fileDescription;
    private String fileRemarks;
    private String fileType;
    private String fileContent;
    private String folderId;
    private String owner;
    private String size;
    private String date;
    private boolean isDisplayed = false;

    public File() {}

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public String getWorkbinId() {
        return workbinId;
    }

    public void setWorkbinId(String workbinId) {
        this.workbinId = workbinId;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getFileDescription() {
        return fileDescription;
    }

    public void setFileDescription(String fileDescription) {
        this.fileDescription = fileDescription;
    }

    public String getFileRemarks() {
        return fileRemarks;
    }

    public void setFileRemarks(String fileRemarks) {
        this.fileRemarks = fileRemarks;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public boolean isDisplayed() {
        return isDisplayed;
    }

    public void setDisplayed(boolean displayed) {
        isDisplayed = displayed;
    }

    @Override
    public boolean equals(Object obj) {
        return this.getId().equals(((File) obj).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}
