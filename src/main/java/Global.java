import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.net.*;

public class Global {

    private static final int SERVER_PORT = 8001;

    private static boolean TRACK = true;

    private static ConcurrentHashMap<String, String> variables = new ConcurrentHashMap<>();
    // key == variable name
    // value == variable value


    private static final String OCDropPhrase = "Red Rising";


    private static String myIp = "not set";

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


    public static String findMyIp(){
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual() || iface.isPointToPoint())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    final String ip = addr.getHostAddress();
                    if(Inet4Address.class == addr.getClass()) return ip;
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void setMyIp(String ip){
        myIp = ip;
    }

    public static String getMyIp(){
        return myIp;
    }


    public static String getNormalFormAddress(String address){
        if(address == null){
            return "";
        }
        InetAddress inetAddress = new InetSocketAddress(address, Global.getSERVER_PORT()).getAddress();
        if(inetAddress == null){
            return "";
        }else{
            return inetAddress.getHostAddress();
        }
    }

}
