package main.models;

import java.util.List;
import java.util.Objects;

public class LessonPlan implements NotificationItem{
    private String moduleId;
    private String id;
    private String title;
    private List<Resource> resources;
    private String descriptionHtml;
    private String startDate;
    private String acutalStartDate;
    private boolean isDisplayed;

    public LessonPlan(String moduleId, String id, String title, String descriptionHtml, String startDate, String acutalStartDate,
                      boolean isDisplayed) {
        this.moduleId = moduleId;
        this.id = id;
        this.title = title;
        this.descriptionHtml = descriptionHtml;
        this.startDate = startDate;
        this.acutalStartDate = acutalStartDate;
        this.isDisplayed = isDisplayed;
    }

    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    public void setDescriptionHtml(String descriptionHtml) {
        this.descriptionHtml = descriptionHtml;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
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
        return this.getId().equals(((LessonPlan) obj).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public String getAcutalStartDate() {
        return acutalStartDate;
    }

    public void setAcutalStartDate(String acutalStartDate) {
        this.acutalStartDate = acutalStartDate;
    }
}
