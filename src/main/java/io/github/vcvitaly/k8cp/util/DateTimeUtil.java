package io.github.vcvitaly.k8cp.util;

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateTimeUtil {

    private static final DateTimeFormatter LONG_ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static LocalDateTime toLocalDate(String fullDate, String fullTime) {
        return LocalDateTime.parse("%s %s".formatted(fullDate, fullTime.substring(0, 5)), LONG_ISO_DATE_FORMATTER);
    }

    public static String toString(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return "";
        }
        return LONG_ISO_DATE_FORMATTER.format(localDateTime);
    }

    public static LocalDateTime toLocalDateTime(FileTime fileTime) {
        return fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
