package main.models;

import java.util.Objects;

public class Announcement implements NotificationItem{
    private String moduleId;
    private String id;
    private String title;
    private String description;
    private String createDate;
    private String creatorName;
    private boolean isDisplayed;

    public Announcement(String moduleId, String id, String title, String description, String creatorName, String createDate,
                        boolean isDisplayed) {
        this.moduleId = moduleId;
        this.id = id;
        this.title = title;
        this.description = description;
        this.creatorName = creatorName;
        this.createDate = createDate;
        this.isDisplayed = isDisplayed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    @Override
    public boolean isDisplayed() {
        return isDisplayed;
    }

    public void setDisplayed(boolean displayed) {
        isDisplayed = displayed;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        return this.getId().equals(((Announcement) obj).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
