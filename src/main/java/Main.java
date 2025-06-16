import Util.LogLevel;
import Util.Logger;

import java.util.Random;


// TODO: 16/06/2025
//      - code clean up
public class Main {
    public static void main(String[] args) {

        //  - start a server
        new Thread(new Server(Global.getSERVER_PORT())).start();

        for (int i = 0; i < 10; i++) {
            Global.addVariable("var" + i, "val" + i);
        }

        Global.setTrack(false);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Logger.log("Error in main at currentThread.wait()", LogLevel.ERROR);
        }

        //  todo - connect to others based on env variables
        if(!System.getenv("BOOTSTRAP_ADDRESS").equals("null")) {
            Peer peer = new Peer(System.getenv("BOOTSTRAP_ADDRESS"));
            peer.sendMessage(Message.createPEER_DISCOVERY_REQUEST());
        }


        while(PeerList.getSize() < 4) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.log("Error in main at currentThread.wait()", LogLevel.ERROR);
            }
        }

        // todo
        //      - start sending messages
        //      - api for it
        //      - modify main a bit so that a server is only started once - ex. if someone runs Main again only the interface for sending messages is available

        if(System.getenv("MANUAL") != null){
            new Thread(new QueryHandler()).start();
//            return;
        }


        Logger.log("Current address list:" + PeerList.getAddressList(""), LogLevel.DEBUG);

//        if (!System.getenv("PEER_ID").equals("peer1")) {
//            return;
//        }

        // testing for debug
//        Logger.log("Creating OC object in main", LogLevel.DEBUG);
        String randomAddress = PeerList.getAddressArrayList("").get(new Random().nextInt(4));
        OnionConnection oc = new OnionConnection(randomAddress);
//        new Thread(oc).start();

        while(!oc.isConnection_established()){
//            Logger.log("Waiting for OC to establish connection ...", LogLevel.DEBUG);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.log("Error in main at currentThread.wait()", LogLevel.ERROR);
            }
        }

        oc.sendMessage(Message.createCHAT_REQUEST("Secret message from " + System.getenv("PEER_ID") + " on the other side!"));



    }
}
