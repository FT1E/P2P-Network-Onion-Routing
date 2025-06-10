package Util;

import java.text.SimpleDateFormat;

public class Logger {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_BRIGHT_RED = "\u001B[31;1m";
    private static final String ANSI_ORANGE = "\u001B[38;5;208;1m";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void log(String content, LogLevel logLevel){

        String color = ANSI_WHITE;
        switch (logLevel){
            case INFO -> color = ANSI_BLUE;
            case STATUS -> color = ANSI_PURPLE;
            case SUCCESS -> color = ANSI_GREEN;
            case ERROR -> color = ANSI_BRIGHT_RED;
            case WARN -> color = ANSI_ORANGE;
            case DEBUG -> color = ANSI_YELLOW;
        }

        String prefix = color + "["  + Thread.currentThread().getName() + "][" + dateFormat.format(System.currentTimeMillis()) + "]:" + ANSI_RESET;
        System.out.println(prefix + content);
    }

    public static void log(String content){
        log(content, LogLevel.INFO);
    }

    public static void chat(String source, String content){
        System.out.println(ANSI_CYAN + "[" +source + "]:" + content + ANSI_RESET);
    }

    public static void chat(String content){
        chat("[CHAT]", content);
    }
}
