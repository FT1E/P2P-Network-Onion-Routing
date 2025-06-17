import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyOnionConnectionList {
    
    
    // - efficient synchronization
    // already fine add O(1), get O(1), remove O(n)
    
    private static ArrayList<OnionConnection> list = new ArrayList<>();
    
    private static ConcurrentHashMap<String, OnionConnection> onionRequests = new ConcurrentHashMap<>();
    //  once you send a message with Onion Connection, it is stored in the above as:
    //      key == message.getId
    //      value = OC which sent the msg
    //  - when the corresponding REPLY is received,
    //      it is removed from here and the message is put in corresponding OC's message queue


    private static ExecutorService thread_pool = Executors.newCachedThreadPool();


    public synchronized static void add(OnionConnection oc){
        list.add(oc);
        thread_pool.submit(oc);
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

    public synchronized static OnionConnection[] getArray(){
        // returning a copy so that the original doesn't get affected if the return result of this does
        return list.toArray(new OnionConnection[0]);
    }

    // when you send out a REQUEST ONION message - when you receive a corresponding reply
    // you can easily know which OC needs to process it
    public static void addRequest(String id, OnionConnection oc){
        onionRequests.put(id, oc);
    }


    // returns true if this message was sent by an OnionConnection, which is then added to OC's message queue
    // false if not - process it like any other message
    public static boolean checkReply(Message message){
        OnionConnection oc = onionRequests.remove(message.getId());
        if (oc == null){
            // you're a middle man, so take key from OnionKeys, encrypt and send it back
            return false;
        }
        // you're the original sender so pass on the processing to oc's message queue
        oc.addMessageToQueue(message);
        return true;
    }
}
