package main.models;

import java.util.List;

public class Module {
    private String courseId;
    private String courseCode;
    private String courseName;
    private List<EReserveFile> eReserveFiles;

    public Module(String courseId, String courseCode, String courseName) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public List<EReserveFile> geteReserveFiles() {
        return eReserveFiles;
    }

    public void seteReserveFiles(List<EReserveFile> eReserveFiles) {
        this.eReserveFiles = eReserveFiles;
    }
}
