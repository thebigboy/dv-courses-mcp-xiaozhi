package dev.danvega.courses.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalenderService {

    private static final String defaultCalenderName = "个人";

    public String getMacCalendarEventsByDate(String dateStr) throws IOException {
        // dateStr 格式要求为 yyyy-MM-dd，例如 "2025-06-06"
        String appleScript = """
        on run argv
            set targetDate to item 1 of argv
            set calName to item 2 of argv
            set output to ""

            set {year:y, month:m, day:d} to (current date)
            set {y, m, d} to {text 1 thru 4 of targetDate, text 6 thru 7 of targetDate, text 9 thru 10 of targetDate}
            set startDate to date (y & "-" & m & "-" & d & " 00:00:00")
            set endDate to date (y & "-" & m & "-" & d & " 23:59:59")

            tell application "Calendar"
                repeat with cal in calendars
                    if (name of cal) is calName then
                        set theEvents to every event of cal whose start date ≥ startDate and start date ≤ endDate
                        repeat with ev in theEvents
                            try
                                try
                                    set startDate to start date of ev
                                    set endDate to end date of ev
        
                                    set startHour to text -2 thru -1 of ("0" & (hours of startDate as string))
                                    set startMin to text -2 thru -1 of ("0" & (minutes of startDate as string))
                                    set startTime to startHour & ":" & startMin
        
                                    set endHour to text -2 thru -1 of ("0" & (hours of endDate as string))
                                    set endMin to text -2 thru -1 of ("0" & (minutes of endDate as string))
                                    set endTime to endHour & ":" & endMin
                                on error
                                    set startTime to "全天"
                                    set endTime to "全天"
                                end try

                                set locationText to location of ev
                                if locationText is missing value or locationText is "" then
                                    set locationText to "无"
                                end if

                                set titleText to summary of ev
                                set output to output & "（时间）" & startTime & " - " & endTime & " （地点）" & locationText & " （事项）：" & titleText & linefeed
                            end try
                        end repeat
                    end if
                end repeat
            end tell
            return output
        end run
        """;

        ProcessBuilder pb = new ProcessBuilder("osascript", "-", dateStr,defaultCalenderName);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            writer.write(appleScript);
            writer.flush();
            writer.close();

            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public Map<String, Object> addCalendarEvent(String title, String location, String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (title == null || title.isEmpty() || startTime == null || endTime == null) {
                result.put("success", false);
                result.put("error", "标题、开始时间和结束时间不能为空");
                return result;
            }

            // 判断时间格式：是否包含“年”
            String dateFormat = startTime.contains("年") ? "yyyy年MM月dd日 HH:mm:ss" : "yyyy-MM-dd HH:mm:ss";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
            LocalDateTime startDate, endDate;

            try {
                startDate = LocalDateTime.parse(startTime, formatter);
                endDate = LocalDateTime.parse(endTime, formatter);
            } catch (DateTimeParseException e) {
                result.put("success", false);
                result.put("error", "时间格式错误：" + e.getMessage());
                return result;
            }

            if (!startDate.isBefore(endDate)) {
                result.put("success", false);
                result.put("error", "开始时间必须早于结束时间");
                return result;
            }

            // 转换为 AppleScript 可识别的格式
            String startStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String endStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 处理转义字符
            title = title.replace("\"", "\\\"");
            location = location.replace("\"", "\\\"");



            String appleScript = String.format("""
            tell application "Calendar"
                try
                    tell calendar "%s"
                        make new event with properties {summary:"%s", location:"%s", start date:date "%s", end date:date "%s"}
                    end tell
                    return "success"
                on error errMsg
                    return "error: " & errMsg
                end try
            end tell
            """, defaultCalenderName, title, location, startStr, endStr);

            ProcessBuilder pb = new ProcessBuilder("osascript", "-e", appleScript);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.lines().collect(Collectors.joining("\n")).trim();
                if (output.startsWith("error:")) {
                    result.put("success", false);
                    result.put("error", output.substring(6).trim());
                } else if (output.equals("success")) {
                    result.put("success", true);
                    result.put("result", "事件添加成功");
                } else {
                    result.put("success", false);
                    result.put("error", "未知返回：" + output);
                }
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "发生错误: " + e.getMessage());
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        CalenderService calenderService = new CalenderService();
        long start = System.currentTimeMillis();
        String result = calenderService.getMacCalendarEventsByDate("2025-06-06");
        long end = System.currentTimeMillis();
        System.out.println((end - start) + "ms");
        System.out.println(result);

        String title = "做作业";
        String location = "教室";
        String startStr = "2025-06-06 12:00:00";
        String endStr = "2025-06-06 12:30:00";
        Map<String,Object> result2 = calenderService.addCalendarEvent(title, location, startStr, endStr);
        System.out.println(result2);
    }

}
