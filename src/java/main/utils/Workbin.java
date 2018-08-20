package main.utils;

public class Workbin {
    private String id;
    private String moduleId;

    public Workbin(String id, String moduleId) {
        this.id = id;
        this.moduleId = moduleId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }
}
