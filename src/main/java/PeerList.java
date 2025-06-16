import Util.LogLevel;
import Util.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerList {
    // static class acting like a dictionary of all peer objects - insert, remove, get
    // also has a thread pool for executing peers' runnables (reading messages)

    // functionalities:
    //  - addPeer
    //  - removePeer (done by the peer object itself)
    //  - getPeer - based on peer address


    // HashMap of peers
    // key   == peer address
    // value == peer object
    private static ConcurrentHashMap<String, Peer> peerMap = new ConcurrentHashMap<>();

    // thread pool for peer runnables - reading messages
    private static ExecutorService threadPool = Executors.newCachedThreadPool();





    // Dictionary methods

    // get by address
    public static Peer getPeer(String address){
        return peerMap.get(address);
    }

    // add
    public synchronized static boolean addPeer(Peer peer){
        if(peer.getAddress().equals("127.0.0.1")){
            Logger.log("Connected to myself, closing connection ...", LogLevel.DEBUG);
            peer.disconnect(true);
            return false;
        }

        if(peerMap.putIfAbsent(peer.getAddress(), peer) != null){
            Logger.log("Duplicate connection with " + peer.getAddress(), LogLevel.DEBUG);
            peer.disconnect(true);
            return false;
        }


        Logger.log("New peer added! Address: " + peer.getAddress());
        // - submit to thread pool - so it can read and process messages from the peer
        threadPool.submit(peer);
        return true;
    }

    // remove
    public static boolean removePeer(Peer peer){
//        Logger.log("AddressList before removing peer:" + getAddressList("."));
        return (peerMap.remove(peer.getAddress()) != null);
    }
    // end Dictionary methods



    // getAddresses
    // get a list of all addresses excluding one
    // used for PEER_DISCOVERY
    public synchronized static ArrayList<String> getAddressArrayList(String excludeAddr){
        ArrayList<String> addresses = new ArrayList<>(Collections.list(peerMap.keys()));
        addresses.remove(excludeAddr);
        return addresses;
    }
    public static String[] getAddressArray(String excludeAddr){
        return getAddressArrayList(excludeAddr).toArray(new String[0]);
    }

    // returns one string joined by ";"
    public static String getAddressList(String excludeAddr){
        String[] addresses = getAddressArray(excludeAddr);
        return String.join(";", addresses);
    }

    // end - getAddresses

    // getSize
    public static int getSize(){
        return peerMap.size();
    }



}
