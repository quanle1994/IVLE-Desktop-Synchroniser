package main.utils;

import main.controllers.IvleDownloader;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Cache {
    private static final Cache CACHE = new Cache();
    private Queue<List<String>> cacheQueue = new LinkedList<>();
    public static Cache getInstance() {return CACHE;}
    public List<String> count = new ArrayList<>();

    public void downloadFile(IvleDownloader ivleDownloader, String fileId, String fileDirectory) {
        Thread thread = new Thread(() -> {
            ivleDownloader.downloadFile(fileId, fileDirectory);
            count.remove(0);
            if (cacheQueue.size() > 0) {
                List<String> cacheFile = cacheQueue.poll();
                downloadFile(ivleDownloader, cacheFile.get(0), cacheFile.get(1));
            }
        });
        if (count.size() <5) {
            count.add("Taken");
            thread.run();
        } else {
            List<String> fileAttributes = new ArrayList<>();
            fileAttributes.add(fileId);
            fileAttributes.add(fileDirectory);
            cacheQueue.add(fileAttributes);
        }
    }
}
