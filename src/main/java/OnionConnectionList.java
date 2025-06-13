import java.util.ArrayList;
import java.util.HashMap;

public class OnionConnectionList {


    // todo - efficient synchronization

    private static ArrayList<OnionConnection> list = new ArrayList<>();

    private static HashMap<String, OnionConnection> keyRequests = new HashMap<>();
    // stores REQUEST KEY_EXCHANGE messages
    //  once you send a REQ KEY_EXCHANGE message, it is stored in the above as:
    //      key == message.getId
    //      value = OC which sent the msg
    //  - when the corresponding REPLY is received  

    public synchronized static void add(OnionConnection oc){
        list.add(oc);
    }

    public synchronized static OnionConnection get(int i){
        return list.get(i);
    }

    public synchronized static void remove(OnionConnection oc){
        list.remove(oc);
    }

    public synchronized static int size(){
        return list.size();
    }
}
