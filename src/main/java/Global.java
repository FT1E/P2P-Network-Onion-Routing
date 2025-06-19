import java.util.concurrent.ConcurrentHashMap;

public class Global {

    private static final int SERVER_PORT = 8001;

    private static boolean TRACK = true;

    private static ConcurrentHashMap<String, String> variables = new ConcurrentHashMap<>();
    // key == variable name
    // value == variable value


    private static final String OCDropPhrase = "Red Rising";

    public static int getSERVER_PORT() {
        return SERVER_PORT;
    }

    public static void setTrack(boolean status){
        TRACK = status;
    }

    public static boolean getTrack(){
        return TRACK;
    }

    public static void addVariable(String name, String value){
        variables.put(name, value);
        // don't care if it's overwriting or not
        // just have a storage of key-value pairs
    }

    public static String getVariable(String name){
        return variables.get(name);
    }

    public static String getOCDropPhrase(){
        return OCDropPhrase;
    }
}
