package main.utils;

import main.models.ThreadNode;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static main.views.IvleView.DATE_FORMATTER;
import static main.views.IvleView.DATE_PARSER;

public class ForumViewHtmlGenerator {
    private static final String style = "<style>\n" +
            "    #page-wrapper {\n" +
            "        padding: 10px 10px;\n" +
            "        font-size: 15px;\n" +
            "        overflow-y: auto;\n" +
            "        width: 100%;\n" +
            "        height: 100%;\n" +
            "        border-radius: 5px;\n" +
            "        background-color: rgba(255,255,255,0.5);\n" +
            "    }\n" +
            "\n" +
            "    #page-body {\n" +
            "        border-radius: 5px;\n" +
            "        padding: 10px 10px;\n" +
            "    }\n" +
            "\n" +
            "    #module-name {\n" +
            "        font-size: 23px;\n" +
            "        font-weight: bold;\n" +
            "    }\n" +
            "\n" +
            "    #forum-header {\n" +
            "        font-size: 20px;\n" +
            "    }\n" +
            "\n" +
            "    .reply-template {\n" +
            "        margin: 10px;\n" +
            "    }\n" +
            "\n" +
            "    .name-tag-template {\n" +
            "        padding: 10px;\n" +
            "        background-color: rgba(112,112,112, 0.7);\n" +
            "        border-top-right-radius: 10px;\n" +
            "        border-top-left-radius: 10px;\n" +
            "        float: left;\n" +
            "    }\n" +
            "\n" +
            "    .reply-body {\n" +
            "        padding: 10px;\n" +
            "        border-bottom-right-radius: 10px;\n" +
            "        border-bottom-left-radius: 10px;\n" +
            "    }\n" +
            "\n" +
            "    .old-post {\n" +
            "        background-color: rgba(91,134,229, 0.6);\n" +
            "    }\n" +
            "\n" +
            "    .new-post {\n" +
            "        background-color: rgba(137,247,254, 0.6);\n" +
            "    }\n" +
            "</style>\n" +
            "<script>\n" +
            "    window.onload = function() {\n" +
            "        var objDiv = document.getElementById(\"page-wrapper\");\n" +
            "        objDiv.scrollTop = objDiv.scrollHeight;\n" +
            "    }\n" +
            "</script>";
    private static final String bodyTag = "<body style=\"width: 96vw; height: 93vh; background: url('REPLACE_URL'); " +
            "font-family: Verdana; overflow: hidden; background-size: 140% 140%; border-radius: 10px\">\n";

    private static final String headerPortion =
            "    <div id=\"page-wrapper\">\n" +
            "    <div id=\"module-name\" style=\"width: 100%; text-align: center; margin-bottom: 20px\">REPLACE_MODULE</div>\n" +
            "    <div id=\"forum-header\" style=\"width: 100%; text-align: center; margin-bottom: 5px\">" +
                    "Heading: REPLACE_HEADING</div>\n" +
            "    <div id=\"page-body\">";

    private static final String closingPortion = "    </div>\n" +
            "</div>\n" +
            "</body>";

    public static String compose(String moduleName, List<ThreadNode> threadNodes) {
        String result = style + bodyTag + headerPortion;
        File backgroundImage = new File("./IVLeSync/summer.jpg");
        result = result.replace("REPLACE_URL", backgroundImage.toURI().toString());
        result = result.replace("REPLACE_MODULE", moduleName);
        result = result.replace("REPLACE_HEADING", threadNodes.get(0).getHeaderTitle());
        for (int i = 0; i < threadNodes.size(); i++) {
            ThreadNode threadNode = threadNodes.get(i);
            String actualTimeStamp = threadNode.getPostDate();
            String formattedDate = "";
            try {
                Date timeStampDate = DATE_PARSER.parse(actualTimeStamp);
                formattedDate = DATE_FORMATTER.format(timeStampDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String thread =
                    "        <div class=\"reply-template\">\n" +
                    "            <div style=\"width: 100%; overflow: hidden;\">\n" +
                    "                <div class=\"name-tag-template\" style=\"max-width: 50%; word-wrap: break-word\">\n" +
                    "                    <div class=\"poster-name\">" + threadNode.getPosterName() + "</div>\n" +
                    "                    <div class=\"poster-email\">" + threadNode.getPosterEmail() + "</div>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "            <div class=\"reply-body REPLACE_POST\">\n" +
                    "                <div class=\"post-title\" style='margin-bottom: 5px'><b>" + threadNode.getPostTitle() + "</b></div>\n" +
                    "                <div class=\"post-body\"  style='margin-bottom: 5px'>" + threadNode.getPostBody() + "</div>\n" +
                    "                <div class=\"post-time\" style='text-align: right; margin-bottom: 5px'>" + formattedDate +"</div>\n" +
                    "            </div>\n" +
                    "        </div>";
            result += thread;
            result = (i == threadNodes.size() - 1) ? result.replace("REPLACE_POST", "new-post") :
                    result.replace("REPLACE_POST", "old-post");
        }
        return result + closingPortion;
    }
}
