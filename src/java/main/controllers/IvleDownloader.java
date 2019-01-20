package main.controllers;

import main.utils.Constants;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IvleDownloader {
    private static final IvleDownloader IVLE_DOWNLOADER = new IvleDownloader();
    private Controller controller = Controller.getInstance();

    public static IvleDownloader getInstance() {
        return IVLE_DOWNLOADER;
    }

    public String downloadModules() {
        Map<String, String> data = new HashMap<>();
        data.put("Duration", "0");
        data.put("IncludeAllInfo", "true");
        String spec = assembleUrl(Constants.BASE_URL + "Modules", data);
        return sendHttpGetRequest(spec);
    }

    public String downloadWorkbin(String workbinId) {
        Map<String, String> data = new HashMap<>();
        data.put("Duration", "0");
        data.put("WorkbinID", workbinId);
        data.put("TitleOnly", "false");
        String spec = assembleUrl(Constants.BASE_URL + "Workbins", data);
        return sendHttpGetRequest(spec);
    }

//    public String downloadAnnouncements(String courseId) {
//        Map<String, String> data = new HashMap<>();
//        data.put("Duration", "0");
//        data.put("CourseID", courseId);
//        data.put("TitleOnly", "false");
//        String spec = assembleUrl(Constants.BASE_URL + "Announcements", data);
//        return sendHttpGetRequest(spec);
//    }

    public void downloadFile(String fileId, String fileDirectory) {
        Map<String, String> data = new HashMap<>();
        data.put("target", "workbin");
        data.put("ID", fileId);
        String spec = assembleUrl(Constants.DOWNLOAD_URL, data);
        saveFile(spec, fileDirectory);
    }

    public String downloadForumThreads(String courseId) {
        Map<String, String> data = new HashMap<>();
        data.put("Duration", "0");
        data.put("CourseID", courseId);
        data.put("TitleOnly", "false");
        data.put("IncludeThreads", "true");
        String spec = assembleUrl(Constants.BASE_URL + "Forums", data);
        return sendHttpGetRequest(spec);
    }

    public String downloadLessonPlan(String startDate, String courseId) {
        Map<String, String> data = new HashMap<>();
        data.put("CourseID", courseId);
        data.put("EventDate", startDate);
        String spec = assembleUrl(Constants.BASE_URL + "LessonPlan_Events", data);
        return sendHttpGetRequest(spec);
    }

    public String downloadEReserve(String courseId) {
        Map<String, String> data = new HashMap<>();
        data.put("CourseID", courseId);
        String spec = assembleUrl(Constants.BASE_URL + "LibEreserves", data);
        return sendHttpGetRequest(spec);
    }

    private void saveFile(String spec, String fileDirectory) {
        try {
            URL url = new URL(spec);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            System.out.println(spec);
            int status = con.getResponseCode();
            if (status != 200) return;
            InputStream is = con.getInputStream();

            File savedFile = new File(fileDirectory);
            File parentFolder = savedFile.getParentFile();
            parentFolder.mkdirs();
            savedFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(savedFile);
            int count;
            byte[] buff = new byte[16 * 1024];
            while ((count = is.read(buff)) != -1) {
                fos.write(buff, 0, count);
            }
            fos.close();
            is.close();
            System.out.println("Finished " + fileDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String sendHttpGetRequest(String spec) {
        try {
        URL url = new URL(spec);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        System.out.println(spec);
        int status = con.getResponseCode();
        if (status != 200) return "";
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String assembleUrl(String restService, Map<String, String> data) {
        data.put("APIKey", controller.getApiKey());
        data.put("AuthToken", controller.getAuthToken());
        List<String> attributes = new ArrayList<>();
        for (String attribute : data.keySet()) attributes.add(attribute + "=" + data.get(attribute));
        return restService + "?" + String.join("&", attributes);
    }
}
