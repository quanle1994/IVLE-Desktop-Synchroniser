package main.models;

import java.util.Objects;

public class Description implements NotificationItem{
    private String moduleId;
    private String title;
    private String description;
    private String createDate;
    private String creatorName;
    private boolean isDisplayed;

    public Description(String moduleId, String title, String description, String creatorName, String createDate,
                       boolean isDisplayed) {
        this.moduleId = moduleId;
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
    public boolean equals(Object obj) {
        return this.getId().equals(((Description) obj).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.moduleId + "-DES");
    }

    @Override
    public String getId() {
        return this.moduleId + "-DES";
    }
}
