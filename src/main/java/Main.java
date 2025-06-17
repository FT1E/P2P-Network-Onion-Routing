import Util.LogLevel;
import Util.Logger;

import java.io.IOException;
import java.util.Random;


// TODO: 16/06/2025
//      - code clean up
//      - maybe also add a command to have an onion connection with custom number of in-between nodes (positive and 1 less than the number of peers you have, so you don't have repeating in-between nodes and the final node isn't used as an in-between node)
//      - also allow testing with real computers not just with docker containers
//          - just add a bootstrap_address variable in Global
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
        try {
            Peer peer = new Peer(System.getenv("BOOTSTRAP_ADDRESS"));
            peer.sendMessage(Message.createPEER_DISCOVERY_REQUEST());
        } catch (IOException e) {
            // logger in constructor
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

        if(System.getenv("MANUAL") != null){
            new Thread(new QueryHandler()).start();
//            return;
        }


        Logger.log("Current address list:" + PeerList.getAddressList("", PeerList.getSize()), LogLevel.DEBUG);

//        if (!System.getenv("PEER_ID").equals("peer1")) {
//            return;
//        }

        // testing for debug
//        Logger.log("Creating OC object in main", LogLevel.DEBUG);
        String randomAddress = PeerList.getAddressList("", 1);
        OnionConnection oc = null;
        try {
            oc = new OnionConnection(randomAddress);
        } catch (IOException e) {
            // logger in constructor
            return;
        }
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
