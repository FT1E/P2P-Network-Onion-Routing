import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyOnionConnectionList {
    
    
    // - efficient synchronization
    // already fine add O(1), get O(1), remove O(n)
    
    private static ArrayList<OnionConnection> ocList = new ArrayList<>();

    private static final Object ocListLock = new Object();

    private static ConcurrentHashMap<String, OnionConnection> myOnionRequests = new ConcurrentHashMap<>();
    //  once you send a message with Onion Connection, it is stored in the above as:
    //      key == message.getId
    //      value = OC which sent the msg
    //  - when the corresponding REPLY is received,
    //      it is removed from here and the message is put in corresponding OC's message queue


    private static ExecutorService thread_pool = Executors.newCachedThreadPool();


    public static void add(OnionConnection oc){
        synchronized (ocListLock) {
            ocList.add(oc);
            thread_pool.submit(oc);
        }
    }

    public static OnionConnection get(int i){
        synchronized (ocListLock) {
            return ocList.get(i);
        }
    }

    public static void remove(OnionConnection oc){
        synchronized (ocListLock) {
            ocList.remove(oc);
        }
    }

    public static int size(){
        synchronized (ocListLock) {
            return ocList.size();
        }
    }

    public static OnionConnection[] getArray(){
        // returning a copy so that the original doesn't get affected if the return result of this does
        synchronized (ocListLock) {
            return ocList.toArray(new OnionConnection[0]);
        }
    }

    // when you send out a REQUEST ONION message - when you receive a corresponding reply
    // you can easily know which OC needs to process it
    public static void addRequest(String id, OnionConnection oc){
        myOnionRequests.put(id, oc);
    }


    // returns true if this message was sent by an OnionConnection, which is then added to OC's message queue
    // false if not - process it like any other message
    public static boolean checkReply(Message message){
        OnionConnection oc = myOnionRequests.remove(message.getId());
        if (oc == null){
            // you're a middle man, so take key from OnionKeys, encrypt and send it back
            return false;
        }
        // you're the original sender so process the message with the oc
        oc.handleMessage(message);
        return true;
    }
}
