package main.models;

public interface NotificationItem {
    boolean isDisplayed();
    boolean equals(Object obj);
    int hashCode();

    String getId();
}
