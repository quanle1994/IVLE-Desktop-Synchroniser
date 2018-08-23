package main.views;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import main.controllers.Controller;
import main.controllers.IvleDownloader;
import main.io.FavFile;
import main.models.*;
import main.models.Module;
import main.utils.Cache;
import main.utils.ForumViewHtmlGenerator;
import main.utils.Workbin;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class IvleView {
    public static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("EEE dd-MM-yyy HH:mm");
    public static final SimpleDateFormat DATE_PARSER_GROUP = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat DATE_FORMATTER_GROUP = new SimpleDateFormat("EEE dd-MM-yyy");
    private static final int THREAD_NUMBER = 5;
    public ScrollPane timeScrollPane;
    public ScrollPane moduleScrollPane;
    public ScrollPane favScrollPane;
    public ScrollPane contentScrollPane;
    public VBox contentVbox;
    public VBox rightVbox;
    private Controller controller = Controller.getInstance();
    private HBox previous = null;
    private IvleDownloader ivleDownloader = IvleDownloader.getInstance();
    private List<String> timeList = new ArrayList<>();
    private List<String> proceed = new ArrayList<>();
    private Map<NotificationItem, HBox> timeItemMap = new HashMap<>();
    private Map<NotificationItem, HBox> moduleItemMap = new HashMap<>();
    private Map<NotificationItem, HBox> favItemMap = new HashMap<>();
    private Map<String, Boolean> favedItemMap = new HashMap<>();
    private Map<String, Module> modules = new HashMap<>();
    private Map<String, Workbin> workbins = new HashMap<>();
    private Map<String, File> files = new HashMap<>();
    private Map<String, Announcement> announcementHashMap = new HashMap<>();
    private Map<String, ThreadNode> threadNodeHashMap = new HashMap<>();
    private Map<String, NotificationItem> notificationItemMap = new HashMap<>();
    private Map<String, Map<String, List<HBox>>> timeMap = new HashMap<>();
    private Map<String, Map<String, List<HBox>>> moduleMap = new HashMap<>();
    private Set<String> addedHeaders = new TreeSet<>();
    private TreeSet<String> existingFilePaths = new TreeSet<>();
    private VBox moduleScrollContent = new VBox();
    private VBox timeScrollContent = new VBox();
    private VBox favScrollContent = new VBox();

    public void initialize() {
        contentScrollPane.prefHeightProperty().bind(rightVbox.heightProperty().subtract(80));
        contentVbox.prefWidthProperty().bind(rightVbox.widthProperty().subtract(24));
        contentVbox.prefHeightProperty().bind(contentScrollPane.heightProperty().subtract(21));
        timeScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        moduleScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        favScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        Service<Void> ser = new Service<>() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() {
                        return null;
                    }
                };
            }
        };
        ser.setOnSucceeded((WorkerStateEvent event) -> {
            retrieveModules();
            Task task = new Task<Void>() {
                @Override
                public Void call() throws Exception {
                    readFavedItemsFromFile();
                    while (true) {
                        Platform.runLater(() -> {
                            createNotiItems();
                            setFavItems();
                            moduleScrollPane.setContent(moduleScrollContent);
                            timeScrollPane.setContent(timeScrollContent);
                            favScrollPane.setContent(favScrollContent);
                        });
                        Thread.sleep(1000);
                    }
                }
            };
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        });
        ser.start();
    }

    public synchronized void retrieveModules() {
        String modulesString = ivleDownloader.downloadModules();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> response
                    = objectMapper.readValue(modulesString, new TypeReference<Map<String,Object>>(){});
            ArrayList<Object> results = (ArrayList<Object>) response.get("Results");
            for (Object result : results) {
                Map<String, Object> moduleDetails = (Map<String, Object>) result;
                String moduleId = moduleDetails.get("ID").toString();
                modules.put(moduleId, new Module(moduleId, moduleDetails.get("CourseCode").toString()
                        .replace("/", "-"), moduleDetails.get("CourseName").toString()
                        .replace("/", "-")));
                ArrayList<Object> workbinObjects = (ArrayList<Object>) moduleDetails.get("Workbins");
                for (Object workbinObject : workbinObjects) {
                    Map<String, Object> worbinDetails = (Map<String, Object>) workbinObject;
                    String workbinId = worbinDetails.get("ID").toString();
                    workbins.put(workbinId, new Workbin(workbinId, moduleId));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread backgroundService = new Thread(() -> {
            while (true) {
                Thread ivleThread = new Thread(() -> {
                    retrieveWorkbinContents();
                    proceed.add("Start IVLE Thread");
                    if (proceed.size() == THREAD_NUMBER) {
                        downloadFiles();
                    }
                });
                ivleThread.setDaemon(true);
                Thread desktopThread = new Thread(() -> {
                    retrieveDesktopConents(controller.getSyncRootDirectory());
                    proceed.add("Start Desktop Thread");
                    if (proceed.size() == THREAD_NUMBER) {
                        downloadFiles();
                    }
                });
                desktopThread.setDaemon(true);
                Thread announcementThread = new Thread(() -> {
                    retrieveAnnouncements();
                    proceed.add("Start Announcement Thread");
                    if (proceed.size() == THREAD_NUMBER) {
                        downloadFiles();
                    }
                });
                announcementThread.setDaemon(true);
                Thread forumThread = new Thread(() -> {
                    retrieveForumThreads();
                    proceed.add("Start Forum Thread");
                    if (proceed.size() == THREAD_NUMBER) {
                        downloadFiles();
                    }
                });
                forumThread.setDaemon(true);
                if (proceed.size() == 0) {
                    proceed.add("Start Background");
                    ivleThread.start();
                    desktopThread.start();
                    announcementThread.start();
                    forumThread.start();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        backgroundService.setDaemon(true);
        backgroundService.start();
    }

    private synchronized void downloadFiles() {
        Set<ImmutablePair> filePairs = files.entrySet().stream().map(
                x -> new ImmutablePair(x.getValue().getDirectory(), x.getValue().getId())).collect(Collectors.toSet());
        List<ImmutablePair> strings = new ArrayList<>(filePairs);
        Collections.sort(strings, Comparator.comparing(pair -> pair.getLeft().toString()));
        List<String> fileIds = strings.stream().map(x -> x.getRight().toString()).collect(Collectors.toList());

        for (String fileId : fileIds) {
            File file = files.get(fileId);
            if (existingFilePaths.contains(file.getDirectory())) continue;
            Cache.getInstance().downloadFile(ivleDownloader, fileId, file.getDirectory());
        }
        System.out.println("FINISHED DOWNLOADING");
        proceed = new ArrayList<>();
    }

    private synchronized boolean checkNewItem() {
        for (String itemId : notificationItemMap.keySet()) {
            NotificationItem notificationItem = notificationItemMap.get(itemId);
            if (!notificationItem.isDisplayed()) return true;
        }
        return false;
    }

    private void readFavedItemsFromFile() {
        while (FavFile.hasNext()) {
            String notificationItemId = FavFile.nextLine();
            favedItemMap.put(notificationItemId, false);
        }
    }

    private void setFavItems() {
        for (String notificationItemId : favedItemMap.keySet()) {
            boolean isSet = favedItemMap.get(notificationItemId);
            if (isSet)  continue;
            NotificationItem notificationItem = notificationItemMap.get(notificationItemId);
            if (notificationItem == null) continue;
            HBox fileRow = timeItemMap.get(notificationItem);
            selectFavItem((ImageView) ((HBox) fileRow.getChildren().get(0)).getChildren().get(0), fileRow, notificationItem);
            favedItemMap.put(notificationItemId, true);
        }
    }

    private synchronized void createNotiItems() {
        if (!checkNewItem()) return;
        addedHeaders = new TreeSet<>();
        timeScrollContent.getChildren().clear();
        moduleScrollContent.getChildren().clear();
        for (String itemId : notificationItemMap.keySet()) {
            NotificationItem notificationItem = notificationItemMap.get(itemId);
            if (notificationItem.isDisplayed()) continue;
            FileTag fileTag1 = new FileTag(notificationItem).invoke(true);
            String date = fileTag1.getTimeStamp();
            String actualDate = fileTag1.getActualTimeStamp();
            String module = fileTag1.getModuleName();
            HBox fileTagTime = fileTag1.getFileRow();
            fileTagTime.getStyleClass().add("item-tag");
            timeItemMap.put(notificationItem, fileTagTime);

            FileTag fileTag2 = new FileTag(notificationItem).invoke(false);
            HBox fileTagMod = fileTag2.getFileRow();
            fileTagMod.getStyleClass().add("item-tag");
            moduleItemMap.put(notificationItem, fileTagMod);

            if (!timeList.contains(date)) timeList.add(date);
            setMap(date, actualDate, timeMap, fileTagTime);
            setMap(module, actualDate, moduleMap, fileTagMod);
        }
        timeNotiItems();
        modNotiItems();
    }

    private void setMap(String key1, String key2, Map<String, Map<String, List<HBox>>> map, HBox fileTagTime) {
        Map<String, List<HBox>> contents = map.get(key1);
        if (contents == null) contents = new HashMap<>();
        List<HBox> vBoxes = contents.get(key2);
        if (vBoxes == null) vBoxes = new ArrayList<>();
        vBoxes.add(fileTagTime);
        contents.put(key2, vBoxes);
        map.put(key1, contents);
    }

    private void timeNotiItems() {
        Collections.sort(timeList);
        for (int j = timeList.size() - 1; j>=0; j--) {
            String timeString = timeList.get(j);
            Map<String, List<HBox>> timeStampMap = timeMap.get(timeString);
            Set<String> timeStampSet = timeStampMap.keySet();
            List<String> timeStamps = new ArrayList<>(timeStampSet);
            Collections.sort(timeStamps, Comparator.reverseOrder());
            for (String timeStamp : timeStamps) {
                List<HBox> hBoxes = timeStampMap.get(timeStamp);
                String _timeString = "";
                try {
                    Date _date = DATE_PARSER_GROUP.parse(timeString);
                    _timeString = DATE_FORMATTER_GROUP.format(_date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                addHeaders(_timeString, timeScrollContent);
                for (int i = 0; i < hBoxes.size(); i++) {
                    HBox hBox = hBoxes.get(i);
                    if (timeScrollContent.getChildren().contains(hBox)) continue;
                    timeScrollContent.getChildren().add(hBox);
                }
            }
        }
    }

    private void modNotiItems() {
        for (String moduleName : moduleMap.keySet()) {
            Map<String, List<HBox>> moduleNames = moduleMap.get(moduleName);
            Set<String> timestamps = moduleNames.keySet();
            List<String> timestampList = new ArrayList<>(timestamps);
            addHeaders(moduleName, moduleScrollContent);

            Collections.sort(timestampList, Comparator.reverseOrder());
            for (String timestamp : timestampList) {
                List<HBox> hBoxes = moduleNames.get(timestamp);
                for (int i = 0; i < hBoxes.size(); i++) {
                    HBox hBox = hBoxes.get(i);
                    if (moduleScrollContent.getChildren().contains(hBox)) continue;
                    moduleScrollContent.getChildren().add(hBox);
                }
            }
        }
    }

    private void addHeaders(String moduleName, VBox listBox) {
        if (!addedHeaders.contains(moduleName)) {
            VBox headerBox = new VBox();
            Label headerContent = new Label(moduleName);
            headerBox.getChildren().add(headerContent);
            headerBox.getStyleClass().add("group-header");
            listBox.getChildren().add(headerBox);
            addedHeaders.add(moduleName);
        }
    }

    private String getSize(double size){
        String hrSize = "";
        double k = size/1024.0;
        double m = size/Math.pow(1024, 2);
        double g = size/Math.pow(1024, 3);
        double t = size/Math.pow(1024, 4);
        DecimalFormat dec = new DecimalFormat("0.00");

        if (t > 1) {
            hrSize = dec.format(t).concat("TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat("GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat("MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat("KB");
        } else {
            hrSize = dec.format(k).concat("KB");
        }

        return hrSize;
    }

    private void retrieveDesktopConents(String directory) {
        java.io.File file = new java.io.File(directory);
        if (!file.isDirectory()) existingFilePaths.add(file.getAbsolutePath().replace("\\", "/"));
        else {
            String[] subNodes = file.list();
            for (String subNode : subNodes) {
                retrieveDesktopConents(file.getAbsolutePath() + "/" + subNode);
            }
        }
    }

    private void retrieveWorkbinContents() {
        for (Workbin workbin : workbins.values()) {
            Module module = modules.get(workbin.getModuleId());
            String moduleFolder = module.getCourseCode() + " - " + module.getCourseName();
            String directory = controller.getSyncRootDirectory() + "/" + moduleFolder;
            String workbinString = ivleDownloader.downloadWorkbin(workbin.getId());
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, Object> response
                        = objectMapper.readValue(workbinString, new TypeReference<Map<String,Object>>(){});
                ArrayList<Object> results = (ArrayList<Object>) response.get("Results");
                for (Object result : results) {
                    Map<String, Object> workbinDetails = (Map<String,Object>) result;
                    ArrayList<Object> folders = (ArrayList<Object>) workbinDetails.get("Folders");
                    traverseDirectory(workbin, directory, folders);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void retrieveAnnouncements() {
        for (String moduleId : modules.keySet()) {
            String announcementListString = ivleDownloader.downloadAnnouncements(moduleId);
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, Object> response = objectMapper.readValue(announcementListString,
                        new TypeReference<Map<String, Object>>(){});
                ArrayList<Object> results = (ArrayList<Object>) response.get("Results");
                for (Object result : results) {
                    Map<String, Object> announcementDetails = (Map<String,Object>) result;
                    String id = announcementDetails.get("ID").toString();
                    if (announcementHashMap.get(id) != null) continue;
                    String title = announcementDetails.get("Title").toString();
                    String description = announcementDetails.get("Description").toString();
                    String creatorName = ((Map<String, Object>) announcementDetails.get("Creator")).get("Name").toString();
                    String createDate = announcementDetails.get("CreatedDate_js").toString();
                    Announcement announcement = new Announcement(moduleId, id, title, description, creatorName,
                            createDate, false);
                    announcementHashMap.put(id, announcement);
                    notificationItemMap.put(id, announcement);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("FINISH ANNOUNCEMENTS");
    }

    private void retrieveForumThreads() {
        for (String moduleId : modules.keySet()) {
            String announcementListString = ivleDownloader.downloadForumThreads(moduleId);
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, Object> response = objectMapper.readValue(announcementListString,
                        new TypeReference<Map<String, Object>>(){});
                ArrayList<Object> results = (ArrayList<Object>) response.get("Results");
                for (Object result : results) {
                    Map<String, Object> threadNodeMap = (Map<String,Object>) result;
                    List<Object> headings = (List<Object>) threadNodeMap.get("Headings");
                    for (Object heading : headings) {
                        Map<String, Object> headingMap = (Map<String,Object>) heading;
                        String headerId = headingMap.get("ID").toString();
                        String headerTitle = headingMap.get("Title").toString();
                        List<Object> threads = (ArrayList<Object>) headingMap.get("Threads");
                        traverseForumThread(moduleId, headerId, headerTitle, threads, null);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("FINISH FORUM THREADS");
    }

    private void traverseForumThread(String moduleId, String headerId, String headerTitle, List<Object> threads,
                                     String parentNodeId) {
        for (Object thread : threads) {
            Map<String, Object> subThreadNodeMap = (Map<String,Object>) thread;
            String id = subThreadNodeMap.get("ID").toString();
            if (threadNodeHashMap.get(id) != null) continue;
//            String postBody = convertToPlainText(subThreadNodeMap.get("PostBody").toString());
            String postBody = subThreadNodeMap.get("PostBody").toString();
            String postDate = subThreadNodeMap.get("PostDate_js").toString();
            String postTitle = subThreadNodeMap.get("PostTitle").toString();
            Map<String, Object> poster = (Map<String, Object>) subThreadNodeMap.get("Poster");
            String posterName = poster.get("Name").toString();
            String posterEmail = poster.get("Email").toString();
            ThreadNode threadNode = new ThreadNode(moduleId, headerId, headerTitle, id, postTitle, postDate,
                    postBody, posterName, posterEmail, parentNodeId, false);
            threadNodeHashMap.put(id, threadNode);
            notificationItemMap.put(id, threadNode);
            List<Object> subThreads = (ArrayList<Object>) subThreadNodeMap.get("Threads");
            traverseForumThread(moduleId, headerId, headerTitle, subThreads, id);
        }
    }

    private String convertToPlainText(String richText) {
        return richText.replace("&nbsp;", " ").replace("<br>", "")
                .replace("&#39;", "'").replace("&gt;", ">")
                .replace("&quot;", "\"");
    }

    private void traverseDirectory(Workbin workbin, String directory, ArrayList<Object> folders) {
        for (Object folder : folders) {
            Map<String, Object> folderDetails = (Map<String, Object>) folder;
            String folderName = folderDetails.get("FolderName").toString();
            String filePath = directory + ("/" + folderName);
            ArrayList<Object> subFiles = (ArrayList<Object>) folderDetails.get("Files");
            for (Object o : subFiles) {
                Map<String, Object> fileDetails = (Map<String, Object>) o;
                String fileName = fileDetails.get("FileName").toString();
                String fileId = fileDetails.get("ID").toString();

                if (files.get(fileId) != null) continue;
                File newFile = new File();
                newFile.setWorkbinId(workbin.getId());
                newFile.setModuleId(workbin.getModuleId());
                newFile.setDirectory(filePath + "/" + fileName);
                newFile.setId(fileId);
                newFile.setFileName(fileName);
                newFile.setFileDescription(fileDetails.get("FileDescription").toString());
                newFile.setFileRemarks(fileDetails.get("FileRemarks").toString());
                newFile.setFileType(fileDetails.get("FileType").toString());
                newFile.setOwner(((Map<String, Object>) fileDetails.get("Creator")).get("Name").toString());
                newFile.setSize(fileDetails.get("FileSize").toString());
                newFile.setDate(fileDetails.get("UploadTime_js").toString());
                files.put(fileId, newFile);
                notificationItemMap.put(fileId, newFile);
            }
            ArrayList<Object> subFolders = (ArrayList<Object>) folderDetails.get("Folders");
            traverseDirectory(workbin, filePath, subFolders);
        }
    }

    private void fillFileInContent(String moduleName, String fileTitle, String directoryValue, String sizeValue,
                                   String descriptionValue, String dateValue, String ownerValue, String remarksValue) {
        Label module = new Label("WORKBIN: " + moduleName);
        module.getStyleClass().add("title-row");
        Label fileNameText = new Label("File Name:");
        fileNameText.getStyleClass().add("row-field");
        Pane fileType = new Pane();
        fileType.getStyleClass().add("file-type-pane");
        Label fileName = new Label(fileTitle);
        fileName.getStyleClass().addAll("row-value", "title-value");
        fileName.getStyleClass().add("hyperlink-style");
        java.io.File file = new java.io.File(directoryValue);
        fileName.setOnMouseClicked(event -> {
            if (file.exists()) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        Label sizeText = new Label("Size:");
        sizeText.getStyleClass().add("row-field");
        Label size = new Label(sizeValue);
        size.getStyleClass().add("row-value");
        Label directoryText = new Label("Directory:");
        directoryText.getStyleClass().add("row-field");
        Label directory = new Label(directoryValue);
        directory.getStyleClass().add("row-value");
        directory.getStyleClass().add("hyperlink-style");
        directory.setOnMouseClicked(event -> {
            try {
                String command = (System.getProperty("os.name").toLowerCase().contains("mac")) ?
                        "open " + file.getParentFile().toURI() : "explorer.exe /select," + file.toURI();
                Runtime.getRuntime().exec(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Label dateText = new Label("Uploaded on:");
        dateText.getStyleClass().add("row-field");
        Label date = new Label(dateValue);
        date.getStyleClass().add("row-value");
        Label ownerText = new Label("Author:");
        ownerText.getStyleClass().add("row-field");
        Label owner = new Label(ownerValue);
        owner.getStyleClass().add("row-value");
        Label descriptionText = new Label("Description:");
        descriptionText.getStyleClass().add("row-field");
        Label description = new Label(descriptionValue);
        description.getStyleClass().add("row-value");
        Label remarksText = new Label("Remarks:");
        remarksText.getStyleClass().add("row-field");
        Label remarks = new Label(remarksValue);
        remarks.getStyleClass().add("row-value");

        HBox titleRow = new HBox();
        titleRow.getStyleClass().add("row-wrapper");
        HBox fileNameRow = new HBox();
        fileNameRow.getStyleClass().add("row-wrapper");
        HBox fileSizeRow = new HBox();
        fileSizeRow.getStyleClass().add("row-wrapper");
        HBox fileDirectoryRow = new HBox();
        fileDirectoryRow.getStyleClass().add("row-wrapper");
        HBox fileDateRow = new HBox();
        fileDateRow.getStyleClass().add("row-wrapper");
        HBox fileOwnerRow = new HBox();
        fileOwnerRow.getStyleClass().add("row-wrapper");
        HBox fileDesRow = new HBox();
        fileDesRow.getStyleClass().add("row-wrapper");
        HBox fileRemarksRow = new HBox();
        fileRemarksRow.getStyleClass().add("row-wrapper");

        titleRow.getChildren().add(module);
        fileNameRow.getChildren().addAll(fileNameText, fileName);
        fileSizeRow.getChildren().addAll(sizeText, size);
        fileDirectoryRow.getChildren().addAll(directoryText, directory);
        fileDateRow.getChildren().addAll(dateText, date);
        fileOwnerRow.getChildren().addAll(ownerText, owner);
        fileDesRow.getChildren().addAll(descriptionText, description);
        fileRemarksRow.getChildren().addAll(remarksText,remarks);
        contentVbox.getChildren().clear();
        contentVbox.getChildren().addAll(titleRow, fileNameRow, fileSizeRow, fileDirectoryRow, fileDateRow, fileOwnerRow,
                fileDesRow, fileRemarksRow);
    }

    private void fillNotiInContent(String moduleName, String notiTitleValue, String descriptionValue, String dateValue,
                                   String ownerValue) {
        Label module = new Label("ANNOUNCEMENT: " + moduleName);
        module.getStyleClass().add("title-row");
        Label notiTitleText = new Label("Title:");
        notiTitleText.getStyleClass().add("row-field");
        Label notiTitle = new Label(notiTitleValue);
        notiTitle.getStyleClass().addAll("row-value", "title-value");
        Label dateText = new Label("Time:");
        dateText.getStyleClass().add("row-field");
        Label date = new Label(dateValue);
        date.getStyleClass().add("row-value");
        Label ownerText = new Label("Author:");
        ownerText.getStyleClass().add("row-field");
        Label owner = new Label(ownerValue);
        owner.getStyleClass().add("row-value");
        Label descriptionText = new Label("Description:");
        descriptionText.getStyleClass().add("row-field");
        WebView description = new WebView();
        WebEngine engine = description.getEngine();
        engine.loadContent(descriptionValue);
        description.getStyleClass().add("html-content");
        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) ->
                redirect(engine, newValue));
        engine.setJavaScriptEnabled(true);
        HBox titleRow = new HBox();
        titleRow.getStyleClass().add("row-wrapper");
        HBox notiTitleRow = new HBox();
        notiTitleRow.getStyleClass().add("row-wrapper");
        HBox notiDateRow = new HBox();
        notiDateRow.getStyleClass().add("row-wrapper");
        HBox notiOwnerRow = new HBox();
        notiOwnerRow.getStyleClass().add("row-wrapper");
        HBox notiDesRow = new HBox();
        notiDesRow.getStyleClass().add("row-wrapper");

        titleRow.getChildren().add(module);
        notiTitleRow.getChildren().addAll(notiTitleText, notiTitle);
        notiDateRow.getChildren().addAll(dateText, date);
        notiOwnerRow.getChildren().addAll(ownerText, owner);
        notiDesRow.getChildren().addAll(descriptionText, description);
        contentVbox.getChildren().clear();
        contentVbox.getChildren().addAll(titleRow, notiTitleRow, notiDateRow, notiOwnerRow,
                notiDesRow);
    }

    private void fillThreadNodeContent(String moduleName, ThreadNode forumThread) {
        contentVbox.getChildren().clear();
        WebView forumViewer = new WebView();
        forumViewer.getStyleClass().add("background-pane");
        forumViewer.setMinWidth(600);
        forumViewer.setMinHeight(400);
        WebEngine engine = forumViewer.getEngine();
        List<ThreadNode> threadNodes = getThreadNodes(forumThread);
        Collections.sort(threadNodes, Comparator.comparing(ThreadNode::getPostDate));
        engine.loadContent(ForumViewHtmlGenerator.compose(moduleName, threadNodes));
        engine.setJavaScriptEnabled(true);
        contentVbox.getChildren().add(forumViewer);
//        contentVbox.getChildren().clear();
//        Label module = getModuleNameBox(moduleName);
//        module.getStyleClass().add("title-row");
//        String headerTitle = forumThread.getHeaderTitle();
//        String postId = forumThread.getPostId();
//        List<ThreadNode> threadNodes = getThreadNodes(forumThread);
//        Collections.sort(threadNodes, Comparator.comparing(ThreadNode::getPostDate));
//        Label headerTitleLabel = new Label("Heading: " + headerTitle);
//        headerTitleLabel.getStyleClass().add("header-title");
//        contentVbox.getChildren().addAll(module, headerTitleLabel);
//        for (ThreadNode threadNode : threadNodes) {
//            String posterName = forumThread.getPosterName();
//            String posterEmail = forumThread.getPosterEmail();
//            String postTitle = "Title: " + forumThread.getPostTitle();
//            String postBody = forumThread.getPostBody();
//            String postDate = formattedDate;
//
//            VBox post = new VBox();
//
//            VBox posterBox = new VBox();
//            Label posterNameLabel = new Label(posterName);
//            posterNameLabel.getStyleClass().add("poster-box");
//            Label posterEmailLabel = new Label(posterEmail);
//            posterEmailLabel.getStyleClass().add("poster-box");
//            Label posterTitleLabel = new Label(postTitle);
//            posterTitleLabel.getStyleClass().add("poster-box");
//            posterBox.getChildren().addAll(posterNameLabel, posterEmailLabel, posterTitleLabel);
//            posterBox.getStyleClass().add("poster-box-wrapper");
//
//            VBox postBodyBox = new VBox();
//            WebView postBodyLabel = new WebView();
//            WebEngine engine = postBodyLabel.getEngine();
//            AtomicBoolean hasChanged = new AtomicBoolean(false);
//            engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
//                if( newValue != Worker.State.SUCCEEDED || !hasChanged.get()) {
//                    return;
//                }
//                double height = Double.parseDouble(engine.executeScript("document.getElementById('text-body')" +
//                        ".clientHeight").toString());
//                if (height > 0) {
//                    hasChanged.set(true);
//                    postBodyLabel.setMinHeight(height);
//                }
//            });
//            engine.loadContent("<body style='background-color: white; font-family: Verdana'><div id='text-body'>" +
//                    postBody + "</div></body>");
////            postBodyLabel.setMinHeight(getHeight(postBody));
//            postBodyLabel.setMinWidth(400);
//            postBodyLabel.setBlendMode(BlendMode.MULTIPLY);
//
////            VBox postBodyLabel = createBodyBox(postBody);
////            postBodyLabel.getStyleClass().add("post-body");
//            Label postDateLabel = new Label(postDate);
//            postBodyBox.getChildren().addAll(postBodyLabel, postDateLabel);
//            postBodyBox.getStyleClass().add("post-body-box");
//
//            post.getChildren().addAll(posterBox, postBodyBox);
//            post.getStyleClass().add("post");
//            contentVbox.getChildren().addAll(post);
//            if (threadNode.getPostId().equals(postId)) {
//                postBodyBox.getStyleClass().add("new-post");
//                break;
//            }
//            postBodyBox.getStyleClass().add("old-post");
//        }
        contentVbox.heightProperty().addListener((ChangeListener) (observable, oldvalue, newValue) ->
                contentScrollPane.setVvalue((Double)newValue));
    }

    private Label getModuleNameBox(String moduleName) {
        Label moduleNameLabel = new Label(moduleName);
        moduleNameLabel.setMinHeight(32 * (1 + moduleName.length()/47));
        return moduleNameLabel;
    }

    private List<ThreadNode> getThreadNodes(ThreadNode currentNode) {
        List<ThreadNode> threadNodes = new ArrayList<>();
        threadNodes.add(currentNode);
        String parentNodeId = currentNode.getParentNodeId();
        if (parentNodeId == null) return threadNodes;
        ThreadNode parentNode = threadNodeHashMap.get(parentNodeId);
        threadNodes.addAll(getThreadNodes(parentNode));
        return threadNodes;
    }

    private VBox createBodyBox(String postBody) {
        VBox bodyBox = new VBox();
        bodyBox.getStyleClass().add("text-wrapper");
        String[] lines = postBody.split("\r\n");
        for (String line : lines) {
            Label lineLabel = new Label(line);
            lineLabel.setMinHeight(21*(1 + line.length()/62));
            bodyBox.getChildren().add(lineLabel);
            lineLabel.getStyleClass().add("wrap-text");
        }
        return bodyBox;
    }

    private double getHeight(String postBody) {
        VBox bodyBox = new VBox();
        String[] lines = postBody.split("\r\n");
        double height = 0;
        for (String line : lines) {
            height += 21 * (1 + line.length()/62);
        }
        return height;
    }

    private void redirect(WebEngine engine, Worker.State newValue) {
        if (newValue != Worker.State.SUCCEEDED) return;
        String url = engine.getLocation();
        if (url.isEmpty()) {
            NodeList nodeList = engine.getDocument().getElementsByTagName("a");
            for (int i = 0; i < nodeList.getLength(); i++)
            {
                Node node= nodeList.item(i);
                EventTarget eventTarget = (EventTarget) node;
                eventTarget.addEventListener("click", evt -> {
                    String href = node.getAttributes().getNamedItem("href").getTextContent();
                    try {
                        Desktop.getDesktop().browse(new URI(href));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    evt.preventDefault();
                }, false);
            }
        }
    }

    private void selectFavItem(ImageView imageView, HBox fileRow, NotificationItem notificationItem) {
        imageView.setVisible(!imageView.isVisible());
        ObservableList<javafx.scene.Node> children = favScrollContent.getChildren();
        ObservableList<javafx.scene.Node> childrenDup = FXCollections.observableArrayList(children);
        if (imageView.isVisible()) {
            fileRow.getStyleClass().add("fav-item");
            FileTag fileTag = new FileTag(notificationItem);
            HBox _fileRow = fileTag.invoke(true).getFileRow();
            _fileRow.getStyleClass().add("item-tag");
            fileTag.getFavourite().setVisible(true);
            favItemMap.put(notificationItem, _fileRow);
            ((HBox) _fileRow.getChildren().get(0)).getChildren().get(0).setVisible(true);
            childrenDup.add(_fileRow);
            childrenDup.sort((o1, o2) -> {
                String o1Text = ((Label) ((HBox) ((VBox) ((HBox) o1).getChildren().get(1)).getChildren().get(1))
                        .getChildren().get(0)).getText().split(" ", 2)[1];
                String o2Text = ((Label) ((HBox) ((VBox) ((HBox) o2).getChildren().get(1)).getChildren().get(1))
                        .getChildren().get(0)).getText().split(" ", 2)[1];
                return o2Text.compareTo(o1Text);
            });
            children.clear();
            children.addAll(childrenDup);
            HBox timeItem = timeItemMap.get(notificationItem);
            HBox modItem = moduleItemMap.get(notificationItem);
            favedItemMap.put(notificationItem.getId(), true);
            FavFile.updateFile(new ArrayList<>(favedItemMap.keySet()));
            setFav(timeItem);
            setFav(modItem);
        }
        else {
            fileRow.getStyleClass().remove("fav-item");
            HBox _fileRow = favItemMap.get(notificationItem);
            children.remove(_fileRow);
            HBox timeItem = timeItemMap.get(notificationItem);
            HBox modItem = moduleItemMap.get(notificationItem);
            favedItemMap.remove(notificationItem.getId());
            FavFile.updateFile(new ArrayList<>(favedItemMap.keySet()));
            unsetFav(timeItem);
            unsetFav(modItem);
        }
    }

    private void setFav(HBox targetItem) {
        ObservableList<String> styleClass = targetItem.getStyleClass();
        if (!styleClass.contains("fav-item")) styleClass.add("fav-item");
        ((HBox) targetItem.getChildren().get(0)).getChildren().get(0).setVisible(true);
    }

    private void unsetFav(HBox targetItem) {
        ObservableList<String> styleClass = targetItem.getStyleClass();
        if (styleClass.contains("fav-item")) styleClass.remove("fav-item");
        ((HBox) targetItem.getChildren().get(0)).getChildren().get(0).setVisible(false);
    }

    private class FileTag {
        private NotificationItem notificationItem;
        private HBox fileRow = new HBox();
        private String timeStamp;
        private String actualTimeStamp;
        private String moduleName;
        private HBox favourite = new HBox();

        public HBox getFavourite() {
            return favourite;
        }

        public FileTag(NotificationItem notificationItem) {
            this.notificationItem = notificationItem;
        }

        public HBox getFileRow() {
            return fileRow;
        }

        public FileTag invoke(boolean isTime) {
            ImageView imageView = new ImageView();
            imageView.setImage(new Image("star.png"));
            imageView.setVisible(false);
            favourite.getChildren().add(imageView);
            favourite.getStyleClass().add("fav-button");
            if (notificationItem instanceof File) {
                File file = (File) notificationItem;
                String moduleId = file.getModuleId();
                Module module = modules.get(moduleId);
                moduleName = module.getCourseCode() + " - " + module.getCourseName();
                VBox fileTag = new VBox();
                String[] split = file.getDirectory().split("/");
                String timeLabel = modules.get(file.getModuleId()).getCourseCode() + " - Workbin: " + file.getFileName();
                String moduleLabel = "Workbin: " + split[split.length - 2] + "/" + file.getFileName();
                Label fileName = (isTime) ? new Label(timeLabel) : new Label(moduleLabel);
                fileName.getStyleClass().add("file-name");
                String size = getSize(Double.parseDouble(file.getSize()));
                Label fileSize = new Label(size);
                actualTimeStamp = file.getDate();
                String formattedDate = "";
                try {
                    Date timeStampDate = DATE_PARSER.parse(actualTimeStamp);
                    formattedDate = DATE_FORMATTER.format(timeStampDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                timeStamp = actualTimeStamp.split("T")[0];
                String _formattedDate = formattedDate;
                Label fileDate = new Label(formattedDate);
                Label fileOwner = new Label(file.getOwner());
                HBox fileDes = new HBox();
                fileDes.getChildren().addAll(fileDate, fileOwner, fileSize);
                fileDes.getStyleClass().add("file-details-holder");
                fileTag.getChildren().addAll(fileName, fileDes);
                file.setDisplayed(true);
                fileRow.getChildren().addAll(favourite, fileTag);
                fileRow.setOnMouseClicked(event -> {
                    fillFileInContent(moduleName, file.getFileName(), file.getDirectory(), size,
                            file.getFileDescription(), _formattedDate, file.getOwner(), file.getFileRemarks());
                    if (previous != null) previous.getStyleClass().remove("selected-item");
                    fileRow.getStyleClass().add("selected-item");
                    previous = fileRow;
                });
            } else if (notificationItem instanceof Announcement) {
                Announcement announcement = (Announcement) notificationItem;
                Module module = modules.get(announcement.getModuleId());
                String courseCode = module.getCourseCode();
                moduleName = courseCode + " - " + module.getCourseName();
                String timeLabel = courseCode + " - Announcement: " + announcement.getTitle();
                String moduleLabel = "Announcement: " + announcement.getTitle();
                Label title = (isTime) ? new Label(timeLabel) : new Label(moduleLabel);
                title.getStyleClass().add("file-name");
                String formattedDate = "";
                actualTimeStamp = announcement.getCreateDate();
                try {
                    Date timeStampDate = DATE_PARSER.parse(actualTimeStamp);
                    formattedDate = DATE_FORMATTER.format(timeStampDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                timeStamp = actualTimeStamp.split("T")[0];
                String _formattedDate = formattedDate;
                Label createDateLabel = new Label(_formattedDate);
                Label creatorName = new Label(announcement.getCreatorName());
                VBox vBox = new VBox();
                HBox hBox = new HBox();
                hBox.getChildren().addAll(createDateLabel, creatorName);
                hBox.getStyleClass().add("noti-details-holder");
                vBox.getChildren().addAll(title, hBox);
                announcement.setDisplayed(true);
                fileRow.getChildren().addAll(favourite, vBox);
                fileRow.setOnMouseClicked(event -> {
                    fillNotiInContent(moduleName, announcement.getTitle(), announcement.getDescription(), _formattedDate,
                            announcement.getCreatorName());
                    if (previous != null) previous.getStyleClass().remove("selected-item");
                    fileRow.getStyleClass().add("selected-item");
                    previous = fileRow;
                });
            } else {
                ThreadNode forumThread = (ThreadNode) notificationItem;
                Module module = modules.get(forumThread.getModuleId());
                String courseCode = module.getCourseCode();
                moduleName = courseCode + " - " + module.getCourseName();
                String timeLabel = courseCode + " - Forum: " + forumThread.getHeaderTitle() + " - " + forumThread
                        .getPostTitle();
                String moduleLabel = "Forum: " + forumThread.getHeaderTitle() + " - " + forumThread.getPostTitle();
                Label title = (isTime) ? new Label(timeLabel) : new Label(moduleLabel);
                title.getStyleClass().add("file-name");
                String formattedDate = "";
                actualTimeStamp = forumThread.getPostDate();
                try {
                    Date timeStampDate = DATE_PARSER.parse(actualTimeStamp);
                    formattedDate = DATE_FORMATTER.format(timeStampDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                timeStamp = actualTimeStamp.split("T")[0];
                String _formattedDate = formattedDate;
                Label createDateLabel = new Label(_formattedDate);
                Label creatorName = new Label(forumThread.getPosterName());
                VBox vBox = new VBox();
                HBox hBox = new HBox();
                hBox.getChildren().addAll(createDateLabel, creatorName);
                hBox.getStyleClass().add("forum-details-holder");
                vBox.getChildren().addAll(title, hBox);
                forumThread.setDisplayed(true);
                fileRow.getChildren().addAll(favourite, vBox);
                fileRow.setOnMouseClicked(event -> {
                    fillThreadNodeContent(moduleName, forumThread);
                    if (previous != null) previous.getStyleClass().remove("selected-item");
                    fileRow.getStyleClass().add("selected-item");
                    previous = fileRow;
                });
            }
            fileRow.setMinWidth(600);
            favourite.setOnMouseClicked(event -> {
                selectFavItem(imageView, fileRow, notificationItem);
                event.consume();
            });
            return this;
        }

        public String getTimeStamp() {
            return timeStamp;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getActualTimeStamp() {
            return actualTimeStamp;
        }
    }
}
