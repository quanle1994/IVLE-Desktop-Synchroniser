package main.views;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import main.controllers.Controller;
import main.controllers.IvleDownloader;
import main.io.FavFile;
import main.models.*;
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
    public ScrollPane timeScrollPane;
    public ScrollPane moduleScrollPane;
    public ScrollPane favScrollPane;
    public AnchorPane contentScrollPane;
    public VBox contentVbox;
    public VBox rightVbox;
    public FontAwesomeIconView loader;
    public JFXSpinner spinner;
    public StackPane loaderWrapper;
    public JFXTextField searchTime;
    public JFXTextField searchModule;
    public JFXTextField lessonPlanWeekSearch;
    public ScrollPane lessonPlanWeekScroll;
    public JFXTextField lessonPlanModuleSearch;
    public ScrollPane lessonPlanModuleScroll;
    private Controller controller = Controller.getInstance();
    private HBox previous = null;
    private IvleDownloader ivleDownloader = IvleDownloader.getInstance();
    private List<String> timeList = new ArrayList<>();
    private List<ImmutablePair<String, String>> lessonPlanTimeList = new ArrayList<>();
    private Map<NotificationItem, HBox> timeItemMap = new HashMap<>();
    private Map<NotificationItem, HBox> moduleItemMap = new HashMap<>();
    private Map<NotificationItem, HBox> lessonWeekItemMap = new HashMap<>();
    private Map<NotificationItem, HBox> lessonModuleItemMap = new HashMap<>();

    private Map<NotificationItem, HBox> favItemMap = new HashMap<>();
    private Map<String, Boolean> favedItemMap = new HashMap<>();
    private Map<String, Module> modules = new HashMap<>();
    private Map<Object, Map<String, Object>> moduleDetails = new HashMap<>();
    private Map<String, Workbin> workbins = new HashMap<>();
    private Map<String, File> files = new HashMap<>();
    private Map<String, Announcement> announcementHashMap = new HashMap<>();
    private Map<String, LessonPlan> lessonPlanHashMap = new HashMap<>();
    private Map<String, ThreadNode> threadNodeHashMap = new HashMap<>();
    private Map<String, NotificationItem> notificationItemMap = new HashMap<>();

    private Map<String, Map<String, List<HBox>>> timeMap = new HashMap<>();
    private Map<String, Map<String, List<HBox>>> moduleMap = new HashMap<>();
    private Map<String, Map<String, List<HBox>>> lessonPlanWeekMap = new HashMap<>(); //week title - actual date - item
    private Map<String, Map<String, List<HBox>>> lessonPlanModuleMap = new HashMap<>(); //module name - actual date - item

    private Map<String, Boolean> moduleDisplay = new HashMap<>();
    private Map<String, Boolean> timeDisplay = new HashMap<>();
    private Set<String> addedHeaders = new TreeSet<>();
    private Set<String> addedLessonPlanHeaders = new TreeSet<>();
    private TreeSet<String> existingFilePaths = new TreeSet<>();
    private VBox moduleScrollContent = new VBox();
    private VBox timeScrollContent = new VBox();
    private VBox favScrollContent = new VBox();
    private VBox lessonPlanWeekScrollContent = new VBox();
    private VBox lessonPlanModuleScrollContent = new VBox();

    public void initialize() {
        timeScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        moduleScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        favScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lessonPlanModuleScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lessonPlanWeekScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        load();
        Service<Void> ser = new Service<Void>() {
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
            Task task = new Task<Void>() {
                @Override
                public Void call() throws Exception {
                    readFavedItemsFromFile();
                    while (true) {
                        Platform.runLater(() -> {
                            createNotiItems();
                            createLessonPlans();
                            setFavItems();
                            moduleScrollPane.setContent(moduleScrollContent);
                            timeScrollPane.setContent(timeScrollContent);
                            favScrollPane.setContent(favScrollContent);
                            lessonPlanWeekScroll.setContent(lessonPlanWeekScrollContent);
                            lessonPlanModuleScroll.setContent(lessonPlanModuleScrollContent);
                        });
                        Thread.sleep(3000);
                    }
                }
            };
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        });
        ser.start();
        loaderWrapper.setOnMouseClicked(event -> {
            if (loader.isVisible()) {
                load();
            }
        });
        searchModule.setOnKeyReleased(event -> {
            List<HBox> tags = moduleScrollContent.getChildren().stream().filter(c -> c instanceof HBox)
                    .map(c -> (HBox) c).collect(Collectors.toList());
            for (int i = 0; i < tags.size(); i++) {
                HBox hBox = tags.get(i);
                VBox vBox = (VBox) hBox.getChildren().get(1);
                if (vBox.getChildren().size() <= 2) continue;
                String text = ((Label) vBox.getChildren().get(3)).getText();
                Label searchLabel = (Label) vBox.getChildren().get(2);
                String searchText = searchLabel.getText();
                if (!isKeptModule(searchText) || !moduleDisplay.get(text)) {
                    hideItemTag(hBox);
                } else {
                    unhideItemTag(hBox);
                }
            }
        });

        searchTime.setOnKeyReleased(event -> {
            List<HBox> tags = timeScrollContent.getChildren().stream().filter(c -> c instanceof HBox)
                    .map(c -> (HBox) c).collect(Collectors.toList());
            for (int i = 0; i < tags.size(); i++) {
                HBox hBox = tags.get(i);
                VBox vBox = (VBox) hBox.getChildren().get(1);
                if (vBox.getChildren().size() <= 2) continue;
                String text = ((Label) vBox.getChildren().get(3)).getText();
                Label searchLabel = (Label) vBox.getChildren().get(2);
                String searchText = searchLabel.getText();
                if (!isKepTime(searchText) || !timeDisplay.get(text)) {
                    hideItemTag(hBox);
                } else {
                    unhideItemTag(hBox);
                }
            }
        });

        lessonPlanWeekSearch.setOnKeyReleased(event -> {
        });
        lessonPlanModuleSearch.setOnKeyReleased(event -> {
        });
    }

    private boolean isKeptModule(String searchText) {
        return searchText.contains(searchModule.getText().toLowerCase());
    }

    private boolean isKepTime(String searchText) {
        return searchText.contains(searchTime.getText().toLowerCase());
    }

    private void load() {
        loader.setVisible(false);
        spinner.setVisible(true);
        Thread backgroundService = new Thread(() -> {
            retrieveModules();
            loader.setVisible(true);
            spinner.setVisible(false);
            searchTime.setVisible(true);
            searchModule.setVisible(true);
            lessonPlanModuleSearch.setVisible(true);
            lessonPlanWeekSearch.setVisible(true);
        });
        backgroundService.setDaemon(true);
        backgroundService.start();
    }

    public void retrieveModules() {
        String modulesString = ivleDownloader.downloadModules();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> response
                    = objectMapper.readValue(modulesString, new TypeReference<Map<String, Object>>() {
            });
            ArrayList<Object> results = (ArrayList<Object>) response.get("Results");
            for (Object result : results) {
                Map<String, Object> moduleDetails = (Map<String, Object>) result;
                String moduleId = moduleDetails.get("ID").toString();
                Module newModule = new Module(moduleId, moduleDetails.get("CourseCode").toString()
                        .replace("/", "-"), moduleDetails.get("CourseName").toString()
                        .replace("/", "-"));
                String eReserveString = ivleDownloader.downloadEReserve(moduleId);
                newModule.seteReserveFiles(extractEreserveFiles(eReserveString));
                modules.put(moduleId, newModule);
                ArrayList<Object> workbinObjects = (ArrayList<Object>) moduleDetails.get("Workbins");
                this.moduleDetails.put(moduleId, moduleDetails);
                for (Object workbinObject : workbinObjects) {
                    Map<String, Object> worbinDetails = (Map<String, Object>) workbinObject;
                    String workbinId = worbinDetails.get("ID").toString();
                    workbins.put(workbinId, new Workbin(workbinId, moduleId));
                }
            }

            existingFilePaths = new TreeSet<>();
            System.out.println("GET DESKTOP DIRS");
            retrieveDesktopConents(controller.getSyncRootDirectory());
            downloadFiles();

            retrieveDescriptions();
            System.out.println("UPDATE DESCRIPTIONS");

            retrieveWorkbinContents();
            System.out.println("DOWNLOAD IVLE");
            downloadFiles();

            retrieveAnnouncements();
            System.out.println("UPDATE ANNOUNCEMENTS");

            retrieveForumThreads();
            System.out.println("DOWNLOAD FORUM POSTS");

            retrieveLessonPlans();
            System.out.println("DOWNLOAD LESSON PLANS");

//            Thread backgroundService = new Thread(() -> {
////                while (true) {
//                    Thread ivleThread = new Thread(() -> {
//                        retrieveWorkbinContents();
//                        proceed.add("Start IVLE Thread");
//                        if (proceed.size() == THREAD_NUMBER) {
//                            downloadFiles();
//                        }
//                    });
//                    ivleThread.setDaemon(true);
//                    Thread desktopThread = new Thread(() -> {
//                        existingFilePaths = new TreeSet<>();
//                        retrieveDesktopConents(controller.getSyncRootDirectory());
//                        proceed.add("Start Desktop Thread");
//                        if (proceed.size() == THREAD_NUMBER) {
//                            downloadFiles();
//                        }
//                    });
//                    desktopThread.setDaemon(true);
//                    Thread announcementThread = new Thread(() -> {
//                        retrieveAnnouncements();
//                        proceed.add("Start Announcement Thread");
//                        if (proceed.size() == THREAD_NUMBER) {
//                            downloadFiles();
//                        }
//                    });
//                    announcementThread.setDaemon(true);
//                    Thread forumThread = new Thread(() -> {
//                        retrieveForumThreads();
//                        proceed.add("Start Forum Thread");
//                        if (proceed.size() == THREAD_NUMBER) {
//                            downloadFiles();
//                        }
//                    });
//                    forumThread.setDaemon(true);
//                    if (proceed.size() == 0) {
//                        proceed.add("Start Background");
//                        ivleThread.start();
//                        desktopThread.start();
//                        announcementThread.start();
//                        forumThread.start();
//                    }
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
////                }
//            });
//            backgroundService.setDaemon(true);
//            backgroundService.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<EReserveFile> extractEreserveFiles(String eReserveString) throws IOException {
        List<EReserveFile> result = new ArrayList<>();
        Map<String, Object> eReserveMap = new ObjectMapper().readValue(eReserveString,
                new TypeReference<Map<String, Object>>() {
                });
        for (Map<String, Object> resultMap : (List<Map<String, Object>>) eReserveMap.get("Results")) {
            for (Map<String, Object> file : ((List<Map<String, Object>>) resultMap.get("Files"))) {
                EReserveFile eReserveFile = new EReserveFile(file.get("ID").toString(),
                        file.get("Description").toString(), file.get("Name").toString());
                result.add(eReserveFile);
            }
        }
        return result;
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
    }

    private synchronized boolean checkNewItem(Map<String, NotificationItem> map) {
        for (String itemId : map.keySet()) {
            NotificationItem notificationItem = map.get(itemId);
            if (!notificationItem.isDisplayed()) return true;
        }
        return false;
    }

    private synchronized boolean checkNewItemLessonPlan(Map<String, LessonPlan> map) {
        for (String itemId : map.keySet()) {
            NotificationItem notificationItem = map.get(itemId);
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
            if (isSet) continue;
            NotificationItem notificationItem = notificationItemMap.get(notificationItemId);
            if (notificationItem == null) {
                LessonPlan lessonPlan = lessonPlanHashMap.get(notificationItemId);
                if (lessonPlan == null) continue;
                HBox fileRow = lessonWeekItemMap.get(lessonPlan);
                selectFavItem((FontAwesomeIconView) ((HBox) fileRow.getChildren().get(0)).getChildren().get(0), fileRow,
                        lessonPlan, lessonWeekItemMap, lessonModuleItemMap);
                favedItemMap.put(notificationItemId, true);
                continue;
            }
            HBox fileRow = timeItemMap.get(notificationItem);
            selectFavItem((FontAwesomeIconView) ((HBox) fileRow.getChildren().get(0)).getChildren().get(0), fileRow,
                    notificationItem, timeItemMap, moduleItemMap);
            favedItemMap.put(notificationItemId, true);
        }
    }

    private synchronized void createNotiItems() {
        if (!checkNewItem(notificationItemMap)) return;
        try {
            addedHeaders = new TreeSet<>();
            timeScrollContent.getChildren().clear();
            moduleScrollContent.getChildren().clear();
            Set<String> ids = notificationItemMap.keySet();
            List<String> itemIds = new ArrayList<>(ids);
            for (int i = 0; i < itemIds.size(); i++) {
                String itemId = itemIds.get(i);
                NotificationItem notificationItem = notificationItemMap.get(itemId);
                if (notificationItem.isDisplayed()) continue;
                FileTag fileTag1 = new FileTag(notificationItem, timeItemMap, moduleItemMap).invoke(true);
                String date = fileTag1.getTimeStamp();
                fileTag1.group = DATE_FORMATTER_GROUP.format(DATE_PARSER_GROUP.parse(date));
                Label label = fileTag1.initLabel();

                String actualDate = fileTag1.getActualTimeStamp();
                String module = fileTag1.getModuleName();
                HBox fileTagTime = fileTag1.getFileRow();
                VBox vBox = (VBox) fileTagTime.getChildren().get(1);
                vBox.getChildren().remove(3);
                vBox.getChildren().add(label);

                fileTagTime.getStyleClass().add("item-tag");
                timeItemMap.put(notificationItem, fileTagTime);

                FileTag fileTag2 = new FileTag(notificationItem, timeItemMap, moduleItemMap);
                fileTag2.group = module;
                fileTag2 = fileTag2.invoke(false);
                HBox fileTagMod = fileTag2.getFileRow();
                fileTagMod.getStyleClass().add("item-tag");
                moduleItemMap.put(notificationItem, fileTagMod);

                if (!timeList.contains(date)) timeList.add(date);
                setMap(date, actualDate, timeMap, fileTagTime);
                setMap(module, actualDate, moduleMap, fileTagMod);
            }
            timeNotiItems();
            modNotiItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void createLessonPlans() {
        if (!checkNewItemLessonPlan(lessonPlanHashMap)) return;
        try {
            addedLessonPlanHeaders = new TreeSet<>();
            lessonPlanWeekScrollContent.getChildren().clear();
            lessonPlanModuleScrollContent.getChildren().clear();
            Set<String> ids = lessonPlanHashMap.keySet();
            List<String> itemIds = new ArrayList<>(ids);
            for (int i = 0; i < itemIds.size(); i++) {
                String itemId = itemIds.get(i);
                LessonPlan lessonPlanItem = lessonPlanHashMap.get(itemId);
                if (lessonPlanItem.isDisplayed()) continue;
                FileTag fileTag1 = new FileTag(lessonPlanItem, lessonWeekItemMap, lessonModuleItemMap).invoke(true);
                String weekTitle = lessonPlanItem.getTitle();
                fileTag1.group = weekTitle;
                Label label = fileTag1.initLabel();

                String actualDate = fileTag1.getActualTimeStamp();
                String module = fileTag1.getModuleName();
                HBox fileTagTime = fileTag1.getFileRow();
                VBox vBox = (VBox) fileTagTime.getChildren().get(1);
                vBox.getChildren().remove(3);
                vBox.getChildren().add(label);

                fileTagTime.getStyleClass().add("item-tag");
                lessonWeekItemMap.put(lessonPlanItem, fileTagTime);

                FileTag fileTag2 = new FileTag(lessonPlanItem, lessonWeekItemMap, lessonModuleItemMap);
                fileTag2.group = module;
                fileTag2 = fileTag2.invoke(false);
                HBox fileTagMod = fileTag2.getFileRow();
                fileTagMod.getStyleClass().add("item-tag");
                lessonModuleItemMap.put(lessonPlanItem, fileTagMod);

                if (!lessonPlanTimeList.contains(fileTag1.getTimeStamp()))
                    lessonPlanTimeList.add(new ImmutablePair<>(weekTitle, fileTag1.getTimeStamp()));
                setMap(weekTitle, actualDate, lessonPlanWeekMap, fileTagTime);
                setMap(module, actualDate, lessonPlanModuleMap, fileTagMod);
            }
            lessonPlanWeekItems();
            lessonPlanModuleItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        for (int j = timeList.size() - 1; j >= 0; j--) {
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
                addHeaders(_timeString, timeScrollContent, false);
                filterItems(hBoxes, _timeString, timeDisplay, timeScrollContent);
            }
        }
    }

    private void filterItems(List<HBox> hBoxes, String groupName, Map<String, Boolean> displayMap, VBox scrollContent) {
        for (int i = 0; i < hBoxes.size(); i++) {
            HBox hBox = hBoxes.get(i);
            Boolean value = displayMap.get(groupName);
            boolean val = value == null ? false : value;
            if (!val) {
                hideItemTag(hBox);
            }
            if (scrollContent.getChildren().contains(hBox)) continue;
            scrollContent.getChildren().add(hBox);
        }
    }

    private void hideItemTag(HBox hBox) {
        hBox.setVisible(false);
        hBox.setMinHeight(0);
        hBox.setPrefHeight(0);
    }

    private void unhideItemTag(HBox hBox) {
        hBox.setVisible(true);
        hBox.setPrefHeight(67);
    }

    private void modNotiItems() {
        ArrayList<String> names = new ArrayList<>(moduleMap.keySet());
        names.sort(String::compareTo);
        for (String moduleName : names) { //module name -> date -> item hbox
            Map<String, List<HBox>> moduleNames = moduleMap.get(moduleName);
            List<String> timestampList = new ArrayList<>(moduleNames.keySet());
            addHeaders(moduleName, moduleScrollContent, true);

            Collections.sort(timestampList, Comparator.reverseOrder());
            for (String timestamp : timestampList) {
                List<HBox> hBoxes = moduleNames.get(timestamp);
                filterItems(hBoxes, moduleName, moduleDisplay, moduleScrollContent);
            }
        }
    }

    private void lessonPlanWeekItems() {
        lessonPlanTimeList.sort(Comparator.comparing(ImmutablePair::getRight));
        for (int j = lessonPlanTimeList.size() - 1; j >= 0; j--) {
            ImmutablePair<String, String> pair = lessonPlanTimeList.get(j);
            Map<String, List<HBox>> timeStampMap = lessonPlanWeekMap.get(pair.getLeft());
            Set<String> timeStampSet = timeStampMap.keySet();
            List<String> timeStamps = new ArrayList<>(timeStampSet);
            Collections.sort(timeStamps, Comparator.reverseOrder());
            for (String timeStamp : timeStamps) {
                List<HBox> hBoxes = timeStampMap.get(timeStamp);
                addLessonPlanHeaders(pair.getLeft() + "_LP", lessonPlanWeekScrollContent, false);
                filterItems(hBoxes, pair.getLeft() + "_LP", timeDisplay, lessonPlanWeekScrollContent);
            }
        }
    }

    private void lessonPlanModuleItems() {
        ArrayList<String> names = new ArrayList<>(lessonPlanModuleMap.keySet());
        names.sort(String::compareTo);
        for (String moduleName : names) { //module name -> date -> item hbox
            Map<String, List<HBox>> moduleNames = lessonPlanModuleMap.get(moduleName);
            List<String> timestampList = new ArrayList<>(moduleNames.keySet());
            addLessonPlanHeaders(moduleName + "_LP", lessonPlanModuleScrollContent, true);

            Collections.sort(timestampList, Comparator.reverseOrder());
            for (String timestamp : timestampList) {
                List<HBox> hBoxes = moduleNames.get(timestamp);
                filterItems(hBoxes, moduleName + "_LP", moduleDisplay, lessonPlanModuleScrollContent);
            }
        }
    }

    private void addHeaders(String headerText, VBox listBox, boolean isModule) {
        if (!addedHeaders.contains(headerText)) {
            HBox headerBox = new HBox();
            VBox box = new VBox();
            Label headerContent = new Label(headerText);

            VBox icon = new VBox();
            FontAwesomeIconView imageView = new FontAwesomeIconView(FontAwesomeIcon.CHEVRON_DOWN);
            icon.getChildren().add(imageView);
            icon.getStyleClass().add("expand-button");

            box.getChildren().add(headerContent);
            headerBox.getChildren().addAll(icon, box);
            toggleGroupHeader(headerText, icon, headerContent, isModule ? moduleDisplay : timeDisplay);
            headerBox.getStyleClass().add("group-header");
            setDisplay(headerText, isModule);
            headerBox.setOnMouseClicked(event -> {
                if (isModule) {
                    toggleVisibilityModule(headerText, headerBox, headerText, moduleMap);
                } else {
                    try {
                        Boolean val = timeDisplay.get(headerText);
                        timeDisplay.put(headerText, !val);
                        Date parse = DATE_FORMATTER_GROUP.parse(headerText);
                        Map<String, List<HBox>> stringListMap = timeMap.get(DATE_PARSER_GROUP.format(parse));
                        ArrayList<String> timeStamps = new ArrayList<>(stringListMap.keySet());
                        toggleVisibilityTime(val, stringListMap, timeStamps);
                        VBox vBox = (VBox) headerBox.getChildren().get(0);
                        vBox.getChildren().clear();
                        toggleGroupHeader(headerText, vBox,
                                (Label) ((VBox) headerBox.getChildren().get(1)).getChildren().get(0), timeDisplay);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });
            listBox.getChildren().add(headerBox);
            addedHeaders.add(headerText);
        }
    }

    private void addLessonPlanHeaders(String headerText, VBox listBox, boolean isModule) { //header text = header + "_LP"
        String header = headerText.split("_")[0];
        if (!addedLessonPlanHeaders.contains(header)) {
            HBox headerBox = new HBox();
            VBox box = new VBox();
            Label headerContent = new Label(header);

            VBox icon = new VBox();
            FontAwesomeIconView imageView = new FontAwesomeIconView(FontAwesomeIcon.CHEVRON_DOWN);
            icon.getChildren().add(imageView);
            icon.getStyleClass().add("expand-button");

            box.getChildren().add(headerContent);
            headerBox.getChildren().addAll(icon, box);
            toggleGroupHeader(headerText, icon, headerContent, isModule ? moduleDisplay : timeDisplay);
            headerBox.getStyleClass().add("group-header");
            setDisplay(headerText, isModule);
            headerBox.setOnMouseClicked(event -> {
                if (isModule) {
                    toggleVisibilityModule(headerText, headerBox, header, lessonPlanModuleMap);
                } else {
                    Boolean val = timeDisplay.get(headerText);
                    timeDisplay.put(headerText, !val);
                    Map<String, List<HBox>> stringListMap = lessonPlanWeekMap.get(header);
                    ArrayList<String> timeStamps = new ArrayList<>(stringListMap.keySet());
                    toggleVisibilityTime(val, stringListMap, timeStamps);
                    VBox vBox = (VBox) headerBox.getChildren().get(0);
                    vBox.getChildren().clear();
                    toggleGroupHeader(headerText, vBox,
                            (Label) ((VBox) headerBox.getChildren().get(1)).getChildren().get(0), timeDisplay);
                }
            });
            listBox.getChildren().add(headerBox);
            addedLessonPlanHeaders.add(header);
        }
    }

    private void toggleVisibilityTime(Boolean val, Map<String, List<HBox>> stringListMap, ArrayList<String> timeStamps) {
        for (int i = 0; i < timeStamps.size(); i++) {
            List<HBox> hBoxes = stringListMap.get(timeStamps.get(i));
            hBoxes = hBoxes.stream().filter(Objects::nonNull).collect(Collectors.toList());
            hBoxes.forEach(hb -> {
                hb.setMinHeight(0);
                Label label = (Label) ((VBox) hb.getChildren().get(1)).getChildren().get(2);
                hb.setPrefHeight(val || !isKepTime(label.getText().toLowerCase()) ? 0 : 67);
                hb.setVisible(!val && isKepTime(label.getText().toLowerCase()));
            });
        }
    }

    private void toggleVisibilityModule(String headerText, HBox headerBox, String header, Map<String, Map<String, List<HBox>>> lessonPlanModuleMap) {
        Boolean val = moduleDisplay.get(headerText);
        moduleDisplay.put(headerText, !val);
        Map<String, List<HBox>> hboxes = lessonPlanModuleMap.get(header);
        ArrayList<String> dates = new ArrayList<>(hboxes.keySet());
        for (int i = 0; i < dates.size(); i++) {
            List<HBox> hBoxes = hboxes.get(dates.get(i));
            if (hBoxes == null) continue;
            hBoxes = hBoxes.stream().filter(Objects::nonNull).collect(Collectors.toList());
            hBoxes.forEach(hb -> {
                hb.setMinHeight(0);
                Label label = (Label) ((VBox) hb.getChildren().get(1)).getChildren().get(2);
                hb.setPrefHeight(val || !isKeptModule(label.getText().toLowerCase()) ? 0 : 67);
                hb.setVisible(!val && isKeptModule(label.getText().toLowerCase()));
            });
        }
        VBox vBox = (VBox) headerBox.getChildren().get(0);
        toggleGroupHeader(headerText, vBox,
                (Label) ((VBox) headerBox.getChildren().get(1)).getChildren().get(0), moduleDisplay);
    }

    private void setDisplay(String headerText, boolean isModule) {
        if (isModule) {
            Boolean value = moduleDisplay.get(headerText);
            moduleDisplay.put(headerText, value == null ? true : value);
        } else {
            Boolean value = timeDisplay.get(headerText);
            timeDisplay.put(headerText, value == null ? true : value);
        }
    }

    private void toggleGroupHeader(String headerText, VBox vBox, Label label, Map<String, Boolean> displayMap) {
        vBox.getChildren().clear();
        if (displayMap.get(headerText) == null || displayMap.get(headerText)) {
            vBox.getChildren().add(new FontAwesomeIconView(FontAwesomeIcon.CHEVRON_DOWN));
            label.getStyleClass().remove("dimmed");
        } else {
            FontAwesomeIconView chevUp = new FontAwesomeIconView(FontAwesomeIcon.CHEVRON_UP);
            chevUp.getStyleClass().add("dimmed");
            vBox.getChildren().add(chevUp);
            label.getStyleClass().add("dimmed");
        }
    }

    private String getSize(double size) {
        String hrSize = "";
        double k = size / 1024.0;
        double m = size / Math.pow(1024, 2);
        double g = size / Math.pow(1024, 3);
        double t = size / Math.pow(1024, 4);
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
                        = objectMapper.readValue(workbinString, new TypeReference<Map<String, Object>>() {
                });
                ArrayList<Object> results = (ArrayList<Object>) response.get("Results");
                for (Object result : results) {
                    Map<String, Object> workbinDetails = (Map<String, Object>) result;
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
//            String announcementListString = ivleDownloader.downloadAnnouncements(moduleId);
            ObjectMapper objectMapper = new ObjectMapper();
            try {
//                Map<String, Object> response = objectMapper.readValue(announcementListString,
//                        new TypeReference<Map<String, Object>>(){});
//                ArrayList<Object> results = (ArrayList<Object>) response.get("Results");
                List<Object> results = (List<Object>) moduleDetails.get(moduleId).get("Announcements");
                for (Object result : results) {
                    Map<String, Object> announcementDetails = (Map<String, Object>) result;
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("FINISH ANNOUNCEMENTS");
    }

    private void retrieveDescriptions() {
        for (String moduleId : modules.keySet()) {
            if (notificationItemMap.get(moduleId + "-DES") != null) continue;
            try {
                Map<String, Object> module = moduleDetails.get(moduleId);
                List<Object> descriptions = (List<Object>) module.get("Descriptions");
                String descriptionText = getDescriptionText(descriptions);
                String creatorName = ((Map<String, Object>) module.get("Creator")).get("Name").toString();
                String createDate = module.get("CourseOpenDate_js").toString();
                Description description = new Description(moduleId, "Module Description", descriptionText, creatorName,
                        createDate, false);
                notificationItemMap.put(moduleId + "-DES", description);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("FINISH DESCRIPTIONS");
    }

    private void retrieveLessonPlans() {
        for (String moduleId : modules.keySet()) {
            try {
                Map<String, Object> module = moduleDetails.get(moduleId);
                List<Map<String, Object>> lessonPlans = (List<Map<String, Object>>) module.get("LessonPlan");
                for (Map<String, Object> lessonPlanObject : lessonPlans) {
                    String id = lessonPlanObject.get("ID").toString();
                    if (lessonPlanHashMap.get(id) != null) continue;
                    String weekNumber = lessonPlanObject.get("Title").toString();
                    String startDate = lessonPlanObject.get("StartDate_js").toString();
                    String endDate = lessonPlanObject.get("EndDate_js").toString();
                    String descriptionHtml = lessonPlanObject.get("Description").toString();

                    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    SimpleDateFormat df2 = new SimpleDateFormat("dd-MM");
                    Date parsedStartDate = DATE_PARSER.parse(startDate);
                    Date parsedEndDate = DATE_PARSER.parse(endDate);
                    String title = "Week " + weekNumber + ": " + df2.format(parsedStartDate) + " to " + df2.format(parsedEndDate);
                    LessonPlan lessonPlan = new LessonPlan(moduleId, id, title, descriptionHtml,
                            df.format(parsedStartDate), startDate, false);
                    readResources(lessonPlan, moduleId);
                    lessonPlanHashMap.put(id, lessonPlan);
//                    notificationItemMap.put(id, lessonPlan);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("FINISH LESSON PLANS");
    }

    private void readResources(LessonPlan lessonPlan, String moduleId) throws Exception {
        String lessonPlanListString = ivleDownloader.downloadLessonPlan(lessonPlan.getStartDate(), moduleId);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> response = objectMapper.readValue(lessonPlanListString,
                new TypeReference<Map<String, Object>>() {
                });
        ArrayList<Map<String, Object>> results = (ArrayList<Map<String, Object>>) response.get("Results");
        if (results.isEmpty()) return;
        Map<String, Object> result = results.get(0);
        List<Resource> resources = new ArrayList<>();
        for (Map<String, Object> resource : (List<Map<String, Object>>) result.get("Resources")) {
            String type = resource.get("ToolType").toString();
            String title = resource.get("ToolTitle").toString();
            String link = resource.get("ToolLink").toString();
            String folderId = resource.get("FolderID").toString();
            String workbinId = resource.get("WorkbinID").toString();
            String toolId = resource.get("ToolID").toString();
            Resource res = new Resource(type, title, link, folderId, workbinId, toolId);
            resources.add(res);
        }
        lessonPlan.setResources(resources);
    }

    private String getDescriptionText(List<Object> descriptions) {
        String text = "";
        descriptions.sort((d1, d2) -> {
            Map<String, Object> map1 = (Map<String, Object>) d1;
            Map<String, Object> map2 = (Map<String, Object>) d2;
            int order1 = Integer.parseInt(map1.get("Order").toString());
            int order2 = Integer.parseInt(map2.get("Order").toString());
            return Integer.compare(order1, order2);
        });
        for (Object description : descriptions) {
            Map<String, Object> desObj = (Map<String, Object>) description;
            Object title = desObj.get("Title");
            Object des = desObj.get("Description");
            text += String.join("", "<h1>", title.toString(), "</h1>");
            text += des.toString().replace("src=\"/v1/bank/media", "src=\"https://ivle.nus.edu.sg/v1/bank/media");
            text += "<h4 />";
        }
        return text;
    }

    private void retrieveForumThreads() {
        for (String moduleId : modules.keySet()) {
            String announcementListString = ivleDownloader.downloadForumThreads(moduleId);
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, Object> response = objectMapper.readValue(announcementListString,
                        new TypeReference<Map<String, Object>>() {
                        });
                ArrayList<Object> results = (ArrayList<Object>) response.get("Results");
                for (Object result : results) {
                    Map<String, Object> threadNodeMap = (Map<String, Object>) result;
                    String title = threadNodeMap.get("Title").toString();
                    List<Object> headings = (List<Object>) threadNodeMap.get("Headings");
                    for (Object heading : headings) {
                        Map<String, Object> headingMap = (Map<String, Object>) heading;
                        String headerId = headingMap.get("ID").toString();
                        String headerTitle = headingMap.get("Title").toString();
                        List<Object> threads = (ArrayList<Object>) headingMap.get("Threads");
                        traverseForumThread(moduleId, headerId, title + ": " + headerTitle, threads, null);
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
            Map<String, Object> subThreadNodeMap = (Map<String, Object>) thread;
            String id = subThreadNodeMap.get("ID").toString();
            String postBody = subThreadNodeMap.get("PostBody").toString();
            String postDate = subThreadNodeMap.get("PostDate_js").toString();
            String postTitle = subThreadNodeMap.get("PostTitle").toString();
            Map<String, Object> poster = (Map<String, Object>) subThreadNodeMap.get("Poster");
            String posterName = poster.get("Name").toString();
            String posterEmail = poster.get("Email").toString();
            ThreadNode threadNode;
            if (threadNodeHashMap.get(id) == null)
                threadNode = new ThreadNode(moduleId, headerId, headerTitle, id, postTitle, postDate,
                        postBody, posterName, posterEmail, parentNodeId, false);
            else threadNode = new ThreadNode(moduleId, headerId, headerTitle, id, postTitle, postDate,
                    postBody, posterName, posterEmail, parentNodeId, true);
            threadNodeHashMap.put(id, threadNode);
            notificationItemMap.put(id, threadNode);
            List<Object> subThreads = (ArrayList<Object>) subThreadNodeMap.get("Threads");
            traverseForumThread(moduleId, headerId, headerTitle, subThreads, id);
        }
    }

    private void traverseDirectory(Workbin workbin, String directory, ArrayList<Object> folders) {
        for (Object folder : folders) {
            Map<String, Object> folderDetails = (Map<String, Object>) folder;
            String folderName = folderDetails.get("FolderName").toString();
            String id = folderDetails.get("ID").toString();
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
                newFile.setFolderId(id);
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
        fileRemarksRow.getChildren().addAll(remarksText, remarks);
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
//        description.getStyleClass().add("html-content");
        VBox.setVgrow(description, Priority.ALWAYS);
        HBox.setHgrow(description, Priority.ALWAYS);
        description.setZoom(1.2);
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
        VBox.setVgrow(notiDesRow, Priority.ALWAYS);
        contentVbox.getChildren().clear();
        contentVbox.getChildren().addAll(titleRow, notiTitleRow, notiDateRow, notiOwnerRow,
                notiDesRow);
        VBox.setVgrow(contentVbox, Priority.ALWAYS);
    }

    private void fillDesContent(String moduleName, String descriptionValue) {
        Label module = new Label("DESCRIPTION: " + moduleName);
        module.getStyleClass().add("title-row");
        WebView desWebView = new WebView();
        WebEngine engine = desWebView.getEngine();
        engine.loadContent(descriptionValue);
        desWebView.setZoom(1.2);
        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) ->
                redirect(engine, newValue));
        engine.setJavaScriptEnabled(true);
        HBox titleRow = new HBox();
        titleRow.getStyleClass().add("row-wrapper");
        HBox notiDesRow = new HBox();
        notiDesRow.getStyleClass().add("row-wrapper");

        titleRow.getChildren().add(module);
        notiDesRow.getChildren().addAll(desWebView);
        VBox.setVgrow(notiDesRow, Priority.ALWAYS);
        HBox.setHgrow(notiDesRow, Priority.ALWAYS);
        VBox.setVgrow(desWebView, Priority.ALWAYS);
        HBox.setHgrow(desWebView, Priority.ALWAYS);
        contentVbox.getChildren().clear();
        contentVbox.getChildren().addAll(titleRow, notiDesRow);
    }

    private void fillLessonPlanContent(String moduleName, LessonPlan lessonPlan) {
        Label module = new Label("LESSON PLAN: " + moduleName);
        module.getStyleClass().add("title-row");

        Label resourcesLabel = new Label("Resources: (" + lessonPlan.getResources().size() + ")");
        resourcesLabel.getStyleClass().add("row-field");
        VBox resourcesBox = new VBox();
        for (int i = 0; i < lessonPlan.getResources().size(); i++) {
            Resource resource = lessonPlan.getResources().get(i);
            String resType = resource.getType();
            String resTitle = resource.getTitle();
            if (resType.equals("LE")) {
                EReserveFile file = modules.get(lessonPlan.getModuleId()).geteReserveFiles().stream()
                        .filter(f -> f.getId().equals(resource.getFileId())).findFirst().orElse(null);
                resTitle = file == null ? "null" : file.getTitle();
            }
            String title = (i + 1) + " - (" + (resType.equals("WB")
                    ? "Work Bin" : resType.equals("LE") ? "E-Reserve" : resType.equals("QZ") ? "Quiz" : "Link") +
                    "): " + resTitle;
            Label link = new Label(title);
            link.getStyleClass().addAll("hyperlink-style-resource");
            link.setOnMouseClicked(event -> {
                String type = resType;
                try {
                    if (type.equals("WB")) {
                        Desktop.getDesktop().open(new java.io.File(files.get(resource.getFileId()).getDirectory()));
                    } else {
                        Desktop.getDesktop().browse(new URI(resource.getLink()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            resourcesBox.getChildren().add(link);
            HBox.setHgrow(link, Priority.ALWAYS);
        }

        Label descriptionLabel = new Label("Description:");
        descriptionLabel.getStyleClass().add("row-field");

        WebView desWebView = new WebView();
        WebEngine engine = desWebView.getEngine();
        engine.loadContent(lessonPlan.getDescriptionHtml());
        desWebView.setZoom(1.2);
        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) ->
                redirect(engine, newValue));
        engine.setJavaScriptEnabled(true);
        HBox titleRow = new HBox();
        titleRow.getStyleClass().add("row-wrapper");
        HBox resourcesRow = new HBox();
        resourcesRow.getStyleClass().add("row-wrapper");
        HBox descriptionRow = new HBox();
        descriptionRow.getStyleClass().add("row-wrapper");

        titleRow.getChildren().add(module);
        resourcesRow.getChildren().addAll(resourcesLabel, resourcesBox);
        descriptionRow.getChildren().addAll(descriptionLabel, desWebView);
        VBox.setVgrow(descriptionRow, Priority.ALWAYS);
        HBox.setHgrow(descriptionRow, Priority.ALWAYS);
        VBox.setVgrow(desWebView, Priority.ALWAYS);
        HBox.setHgrow(desWebView, Priority.ALWAYS);

        HBox.setHgrow(resourcesRow, Priority.ALWAYS);
        HBox.setHgrow(resourcesBox, Priority.ALWAYS);

        contentVbox.getChildren().clear();
        contentVbox.getChildren().addAll(titleRow, resourcesRow, descriptionRow);
    }

    private void fillThreadNodeContent(String moduleName, ThreadNode forumThread) {
        contentVbox.getChildren().clear();
        WebView forumViewer = new WebView();
        forumViewer.getEngine();
        forumViewer.getStyleClass().add("background-pane");
        VBox.setVgrow(forumViewer, Priority.ALWAYS);
        HBox.setHgrow(forumViewer, Priority.ALWAYS);
        WebEngine engine = forumViewer.getEngine();
        List<ThreadNode> threadNodes = getThreadNodes(forumThread);
        Collections.sort(threadNodes, Comparator.comparing(ThreadNode::getPostDate));
        engine.loadContent(ForumViewHtmlGenerator.compose(moduleName, threadNodes));
        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) ->
                redirect(engine, newValue));
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
//        contentVbox.heightProperty().addListener((ChangeListener) (observable, oldvalue, newValue) ->
//                contentScrollPane.setPrefHeight((Double) newValue));
    }

    private Label getModuleNameBox(String moduleName) {
        Label moduleNameLabel = new Label(moduleName);
        moduleNameLabel.setMinHeight(32 * (1 + moduleName.length() / 47));
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
            lineLabel.setMinHeight(21 * (1 + line.length() / 62));
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
            height += 21 * (1 + line.length() / 62);
        }
        return height;
    }

    private void redirect(WebEngine engine, Worker.State newValue) {
        if (newValue != Worker.State.SUCCEEDED) return;
        String url = engine.getLocation();
        if (url.isEmpty()) {
            NodeList nodeList = engine.getDocument().getElementsByTagName("a");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
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

    private void selectFavItem(FontAwesomeIconView imageView, HBox fileRow, NotificationItem notificationItem,
                               Map<NotificationItem, HBox> timeMap, Map<NotificationItem, HBox> modMap) {
        imageView.setVisible(!imageView.isVisible());
        ObservableList<javafx.scene.Node> children = favScrollContent.getChildren();
        ObservableList<javafx.scene.Node> childrenDup = FXCollections.observableArrayList(children);
        if (imageView.isVisible()) {
            fileRow.getStyleClass().add("fav-item");
            FileTag fileTag = new FileTag(notificationItem, timeMap, modMap);
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
            HBox timeItem = timeMap.get(notificationItem);
            HBox modItem = modMap.get(notificationItem);
            favedItemMap.put(notificationItem.getId(), true);
            FavFile.updateFile(new ArrayList<>(favedItemMap.keySet()));
            setFav(timeItem);
            setFav(modItem);
        } else {
            fileRow.getStyleClass().remove("fav-item");
            HBox _fileRow = favItemMap.get(notificationItem);
            children.remove(_fileRow);
            HBox timeItem = timeMap.get(notificationItem);
            HBox modItem = modMap.get(notificationItem);
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
        private String group;
        private HBox favourite = new HBox();
        private Map<NotificationItem, HBox> timeMap;
        private Map<NotificationItem, HBox> modMap;

        public HBox getFavourite() {
            return favourite;
        }

        public FileTag(NotificationItem notificationItem, Map<NotificationItem, HBox> timeMap, Map<NotificationItem, HBox> modMap) {
            this.notificationItem = notificationItem;
            this.timeMap = timeMap;
            this.modMap = modMap;
        }

        public HBox getFileRow() {
            return fileRow;
        }

        public FileTag invoke(boolean isTime) {
            FontAwesomeIconView imageView = new FontAwesomeIconView(FontAwesomeIcon.STAR);
            imageView.setSize("2em");
            imageView.getStyleClass().add("star-icon");
//            imageView.setImage(new Image("star.png"));
            imageView.setVisible(false);
            favourite.getChildren().add(imageView);
            favourite.getStyleClass().add("fav-button");
            fileRow.setMinHeight(0);
            fileRow.setPrefHeight(67);
            Label label = initLabel();
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
                Label searchString = new Label(String.join("||", fileName.getText(), file.getFileName(),
                        file.getOwner(), file.getDate(), file.getFileDescription(), file.getFileContent()).toLowerCase());
                searchString.setVisible(false);
                searchString.setMaxHeight(0);
                HBox fileDes = new HBox();
                fileDes.getChildren().addAll(fileDate, fileOwner, fileSize);
                fileDes.getStyleClass().add("file-details-holder");
                fileTag.getChildren().addAll(fileName, fileDes, searchString, label);
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
                Announcement notificationItem = (Announcement) this.notificationItem;
                Announcement announcement = notificationItem;
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
                Label searchString = new Label(String.join("||", title.getText(), notificationItem.getCreatorName(),
                        notificationItem.getCreateDate(), notificationItem.getDescription()).toLowerCase());
                searchString.setVisible(false);
                searchString.setMaxHeight(0);
                hBox.getStyleClass().add("noti-details-holder");
                vBox.getChildren().addAll(title, hBox, searchString, label);
                announcement.setDisplayed(true);
                fileRow.getChildren().addAll(favourite, vBox);
                fileRow.setOnMouseClicked(event -> {
                    fillNotiInContent(moduleName, announcement.getTitle(), announcement.getDescription(), _formattedDate,
                            announcement.getCreatorName());
                    if (previous != null) previous.getStyleClass().remove("selected-item");
                    fileRow.getStyleClass().add("selected-item");
                    previous = fileRow;
                });
            } else if (notificationItem instanceof Description) {
                Description notificationItem = (Description) this.notificationItem;
                Description description = notificationItem;
                Module module = modules.get(description.getModuleId());
                String courseCode = module.getCourseCode();
                moduleName = courseCode + " - " + module.getCourseName();
                String timeLabel = courseCode + " - Module Description: " + module.getCourseName();
                String moduleLabel = "Module Description: " + courseCode;
                Label title = (isTime) ? new Label(timeLabel) : new Label(moduleLabel);
                title.getStyleClass().add("file-name");
                String formattedDate = "";
                actualTimeStamp = description.getCreateDate();
                try {
                    Date timeStampDate = DATE_PARSER.parse(actualTimeStamp);
                    formattedDate = DATE_FORMATTER.format(timeStampDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                timeStamp = actualTimeStamp.split("T")[0];
                String _formattedDate = formattedDate;
                Label createDateLabel = new Label(_formattedDate);
                Label creatorName = new Label(description.getCreatorName());
                VBox vBox = new VBox();
                HBox hBox = new HBox();
                hBox.getChildren().addAll(createDateLabel, creatorName);
                Label searchString = new Label(String.join("||", title.getText(), notificationItem.getCreatorName(),
                        notificationItem.getCreateDate(), notificationItem.getDescription()).toLowerCase());
                searchString.setVisible(false);
                searchString.setMaxHeight(0);
                hBox.getStyleClass().add("des-details-holder");
                vBox.getChildren().addAll(title, hBox, searchString, label);
                description.setDisplayed(true);
                fileRow.getChildren().addAll(favourite, vBox);
                fileRow.setOnMouseClicked(event -> {
                    fillDesContent(moduleName, description.getDescription());
                    if (previous != null) previous.getStyleClass().remove("selected-item");
                    fileRow.getStyleClass().add("selected-item");
                    previous = fileRow;
                });
            } else if (notificationItem instanceof LessonPlan) {
                LessonPlan notificationItem = (LessonPlan) this.notificationItem;
                LessonPlan lessonPlan = notificationItem;
                Module module = modules.get(lessonPlan.getModuleId());
                String courseCode = module.getCourseCode();
                moduleName = courseCode + " - " + module.getCourseName();
                String timeLabel = courseCode + " - Lesson Plan: " + lessonPlan.getTitle();
                String moduleLabel = "Lesson Plan: " + lessonPlan.getTitle();
                Label title = (isTime) ? new Label(timeLabel) : new Label(moduleLabel);
                title.getStyleClass().add("file-name");
                String formattedDate = "";
                actualTimeStamp = lessonPlan.getAcutalStartDate();
                try {
                    Date timeStampDate = DATE_PARSER.parse(actualTimeStamp);
                    formattedDate = DATE_FORMATTER.format(timeStampDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                timeStamp = actualTimeStamp.split("T")[0];
                String _formattedDate = formattedDate;
                Label createDateLabel = new Label(_formattedDate);
                Label creatorName = new Label("Lesson Plan");
                VBox vBox = new VBox();
                HBox hBox = new HBox();
                hBox.getChildren().addAll(createDateLabel, creatorName);
                Label searchString = new Label(String.join("||", title.getText(),
                        notificationItem.getDescriptionHtml(), String.join("||",
                                notificationItem.getResources().stream().map(r -> r.getLink() + "||" + r.getTitle())
                                        .collect(Collectors.toList()))).toLowerCase());
                searchString.setVisible(false);
                searchString.setMaxHeight(0);
                hBox.getStyleClass().add("lessonPlan-details-holder");
                vBox.getChildren().addAll(title, hBox, searchString, label);
                lessonPlan.setDisplayed(true);
                fileRow.getChildren().addAll(favourite, vBox);
                fileRow.setOnMouseClicked(event -> {
                    fillLessonPlanContent(moduleName, lessonPlan);
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
                Label searchString = new Label(String.join("||", title.getText(), forumThread.getPostDate(),
                        forumThread.getPosterName(), forumThread.getPostBody(), forumThread.getPosterEmail()).toLowerCase());
                searchString.setVisible(false);
                searchString.setMaxHeight(0);
                searchString.setPrefHeight(0);
                hBox.getStyleClass().add("forum-details-holder");
                vBox.getChildren().addAll(title, hBox, searchString, label);
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
                selectFavItem(imageView, fileRow, notificationItem, timeMap, modMap);
                event.consume();
            });
            return this;
        }

        private Label initLabel() {
            Label label = new Label(group);
            label.setVisible(false);
            label.setMinHeight(0);
            label.setPrefHeight(0);
            return label;
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
