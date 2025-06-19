import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import Keys.*;
import Util.LogLevel;
import Util.Logger;

public class OnionKeys {
    // used for storing keys for onion connections where you're the middle man
    // simply keys for when you're some in-between node

    // acting like a dictionary of the decribed keys


    private static ConcurrentHashMap<String, SymmetricKey> onionKeys = new ConcurrentHashMap<>();
    // key == connection_id
    // value == symmetric key associated with that connection

    // basically when you receive a REQUEST ONION message with
    // this connection id, you decrypt it with the corresponding key
    // then once you receive the corresponding REPLY of the inner message
    // you encrypt it with this same key


    // Dictionary methods
    public static void add(String connection_id, SymmetricKey symKey){
        onionKeys.put(connection_id, symKey);
        Logger.log("Stored a symmetric key with connection id:" + connection_id, LogLevel.SUCCESS);
    }

    public static SymmetricKey get(String connection_id){
        return onionKeys.get(connection_id);
    }

    public static boolean remove(String connection_id){
//        Logger.log("Removing onion key with connection id == " + connection_id, LogLevel.DEBUG);
        return (onionKeys.remove(connection_id) == null);
    }

    // end Dictionary methods


}
