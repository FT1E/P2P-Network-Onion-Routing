import java.util.HashMap;

public class OnionKeys {
    // used for storing keys for onion connections where you're the middle man
    // simply keys for when you're some in-between node

    // acting like a dictionary of the decribed keys


    // todo - efficient way to synchronize
    private static HashMap<String, SymmetricKey> onionKeys = new HashMap<>();
    // key == connection_id
    // value == symmetric key associated with that connection

    // basically when you receive a REQUEST ONION message with
    // this connection id, you decrypt it with the corresponding key
    // then once you receive the corresponding REPLY of the inner message
    // you encrypt it with this same key


    // Dictionary methods
    public synchronized static void add(String connection_id, SymmetricKey symKey){
        onionKeys.put(connection_id, symKey);
    }

    public synchronized static void get(String connection_id){
        onionKeys.get(connection_id);
    }

    public synchronized static boolean remove(String connection_id){
        return (onionKeys.remove(connection_id) == null);
    }

    // end Dictionary methods


}
