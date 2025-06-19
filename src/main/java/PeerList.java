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


    // don't make an address list if a new peer is being added
    private static final Object lock = new Object();


    // Dictionary methods

    // get by address
    public static Peer getPeer(String address){
        return peerMap.get(address);
    }

    // add
    public static boolean addPeer(Peer peer){

            if (peer.getAddress().equals("127.0.0.1")) {
                Logger.log("Connected to myself, closing connection ...", LogLevel.DEBUG);
                peer.disconnect();
                return false;
            }

        synchronized (lock) {
            if (peerMap.putIfAbsent(peer.getAddress(), peer) != null) {
//                String address = peer.getAddress();
                Logger.log("Duplicate connection with " + peer.getAddress() + ", closing connection ...", LogLevel.DEBUG);
                peer.disconnect();
//                Logger.log("After duplicate is the address [" + address + "] is peer still in PeerList:"  + (getPeer(address) != null), LogLevel.DEBUG);
                return false;
            }


            Logger.log("New peer added! Address: " + peer.getAddress());
            // - submit to thread pool - so it can read and process messages from the peer
            threadPool.submit(peer);
            return true;
        }
    }

    // remove
    public static boolean removePeer(Peer peer){
//        Logger.log("AddressList before removing peer:" + getAddressList("."));
        synchronized (lock) {
            return peerMap.remove(peer.getAddress(), peer);
        }
    }
    // end Dictionary methods



    // getAddresses
    // get a list of all addresses excluding one
    // used for PEER_DISCOVERY
    private static ArrayList<String> getAddressArrayList(String excludeAddr, int n, boolean random){
        synchronized (lock) {
            ArrayList<String> addresses = new ArrayList<>(Collections.list(peerMap.keys()));
            addresses.remove(excludeAddr);

            if (n < 0) {
                n = 0;
            } else if (n > addresses.size()) {
                n = addresses.size();
            }
            if(random) {
                Collections.shuffle(addresses);
            }
            return new ArrayList<>(addresses.subList(0, n));
        }
    }
    public static String[] getAddressArray(String excludeAddr, int n, boolean random){
        return getAddressArrayList(excludeAddr, n, random).toArray(new String[0]);
    }

    // returns one string joined by ";"
    public static String getAddressList(String excludeAddr, int n, boolean random){
        String[] addresses = getAddressArray(excludeAddr, n, random);
        return String.join(";", addresses);
    }

    // end - getAddresses

    // getSize
    public static int getSize(){
        return peerMap.size();
    }
}
