public class Constants {

    private static final int SERVER_PORT = 8001;

    private static boolean TRACK = true;
    public static int getSERVER_PORT() {
        return SERVER_PORT;
    }

    public static void setTrack(boolean status){
        TRACK = status;
    }

    public static boolean getTrack(){
        return TRACK;
    }
}
