import Util.Logger;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerList {
    // static class acting like a dictionary of all peer objects - insert, remove, get
    // also has a thread pool for executing peers' runnables (reading messages)

    // functionalities:
    //  - addPeer
    //  - removePeer (can be done by the peer object itself)
    //  - getPeer - based on index or peer address


    // array list of peer objects
    private static ArrayList<Peer> peerList = new ArrayList<>();

    // thread pool for peer runnables - reading messages
    private static ExecutorService threadPool = Executors.newCachedThreadPool();


    // todo - think of how to more efficiently achieve synchronization


    // add 1
    public synchronized static void addPeer(Peer peer) {
        // - add to list

        for (int i = 0; i < peerList.size(); i++) {
            if(peerList.get(i).getAddress().equals(peer.getAddress())){
                Logger.log("Already connected to peer [" + peer.getAddress() + "], closing connection!");
                peer.disconnect();
                return;
            }
        }

        Logger.log("New peer!");
        peerList.add(peer);
        // - submit to thread pool
        threadPool.submit(peer);
    }

    // remove
    public synchronized static void removePeer(Peer peer){
        // so to close a connection with peer just call its disconnect method

        //  - close connection with peer - done in Peer.disconnect which calls this method
        //  - remove it from the list
        peerList.remove(peer);
    }

    // get a peer object
    // based on index
    public synchronized static Peer getPeer(int i){
        return peerList.get(i);
    }

    // based on address
    public synchronized static Peer getPeer(String address){
        for (int i = 0; i < peerList.size(); i++) {
            if (peerList.get(i).getAddress().equals(address)){
                return peerList.get(i);
            }
        }
        return null;
    }

    // end get a peer object


    // getAddresses
    // get a list of all addresses excluding one
    // used for PEER_DISCOVERY
    public static String[] getAddressArray(String excludeAddr){
        return getAddressArrayList(excludeAddr).toArray(new String[0]);
    }

    public synchronized static ArrayList<String> getAddressArrayList(String excludeAddr){
        ArrayList<String> addresses = new ArrayList<>(peerList.size() - 1);
        for (int i=0; i<peerList.size(); i++){
            if (peerList.get(i).getAddress().equals(excludeAddr)) continue;
            addresses.add(peerList.get(i).getAddress());
        }
        return addresses;
    }

    // returns one string joined by ";"
    public static String getAddressList(String excludeAddr){
        String[] addresses = getAddressArray(excludeAddr);
        return String.join(";", addresses);
    }

    // end - getAddresses

    // getSize
    public static int getSize(){
        return peerList.size();
    }


}
