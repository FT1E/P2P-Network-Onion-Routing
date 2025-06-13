import java.util.ArrayList;
import java.util.HashMap;

public class OnionConnectionList {
    
    
    // todo - efficient synchronization
    
    private static ArrayList<OnionConnection> list = new ArrayList<>();
    
    private static HashMap<String, OnionConnection> onionRequests = new HashMap<>();
    //  once you send a message with Onion Connection, it is stored in the above as:
    //      key == message.getId
    //      value = OC which sent the msg
    //  - when the corresponding REPLY is received,
    //      it is removed from here and the message is put in corresponding OC's message queue




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


    public synchronized static void addRequest(String id, OnionConnection oc){
        onionRequests.put(id, oc);
    }


    // returns true if this message was sent by an OnionConnection, which is then added to OC's message queue
    // false if not - process it like any other message
    public synchronized static boolean checkReply(Message message){
        OnionConnection oc = onionRequests.remove(message.getId());
        if (oc == null){
            // you're a middle man, so take key from OnionKeys, encrypt and send it back
            return false;
        }
        // you're the original sender so pass on the processing to oc's message queue
        oc.addMessage(message);
        return true;
    }
}
