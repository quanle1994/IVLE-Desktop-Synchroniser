package main.models;

import java.util.Objects;

public class ThreadNode implements NotificationItem{
    private String moduleId;
    private String headerId;
    private String headerTitle;
    private String postId;
    private String postTitle;
    private String postDate;
    private String postBody;
    private String posterName;
    private String posterEmail;
    private String parentNodeId;
    private boolean isDisplayed;

    public ThreadNode(String moduleId, String headerId, String headerTitle, String postId, String postTitle, String postDate,
                      String postBody, String posterName, String posterEmail, String parentNodeId, boolean isDisplayed) {
        this.moduleId = moduleId;
        this.headerId = headerId;
        this.headerTitle = headerTitle;
        this.postId = postId;
        this.postTitle = postTitle;
        this.postDate = postDate;
        this.postBody = postBody;
        this.posterName = posterName;
        this.posterEmail = posterEmail;
        this.isDisplayed = isDisplayed;
        this.parentNodeId = parentNodeId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostDate() {
        return postDate;
    }

    public void setPostDate(String postDate) {
        this.postDate = postDate;
    }

    public String getPostBody() {
        return postBody;
    }

    public void setPostBody(String postBody) {
        this.postBody = postBody;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public void setHeaderTitle(String headerTitle) {
        this.headerTitle = headerTitle;
    }

    public String getPosterName() {
        return posterName;
    }

    public void setPosterName(String posterName) {
        this.posterName = posterName;
    }

    public String getPosterEmail() {
        return posterEmail;
    }

    public void setPosterEmail(String posterEmail) {
        this.posterEmail = posterEmail;
    }

    @Override
    public boolean isDisplayed() {
        return isDisplayed;
    }

    public void setDisplayed(boolean isDisplayed) {
        this.isDisplayed = isDisplayed;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getHeaderId() {
        return headerId;
    }

    public void setHeaderId(String headerId) {
        this.headerId = headerId;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public boolean equals(Object obj) {
        return this.getPostId().equals(((ThreadNode) obj).getPostId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getPostId());
    }

    public String getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }
}
