import Util.LogLevel;
import Util.Logger;

import java.io.IOException;
import java.util.Random;


public class Main {
    public static void main(String[] args) {

        //  - start a server
        new Thread(new Server(Global.getSERVER_PORT())).start();

        Global.setMyIp(Global.findMyIp());


        for (int i = 0; i < 10; i++) {
            Global.addVariable("var" + i, "val" + i);
        }

//        Global.setTrack(false);


        //  - connect to a booststrap address (env variable) if available
        //  - otherwise you can connect to someone with CONNECT command in QueryHandler
        //  - once you connect to someone you immediately send them a PEER_DISCOVERY request
        String bootstrap_address = System.getenv("BOOTSTRAP_ADDRESS");
        if(bootstrap_address != null) {
            do {
                try {
                    new Peer(bootstrap_address);
//                    peer.sendMessage(Message.createPEER_DISCOVERY_REQUEST());
                    break;
                } catch (IOException e) {
                    // logger in constructor
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Logger.log("Error in thread.sleep!", LogLevel.ERROR);
                }

            } while (PeerList.getPeer(bootstrap_address) == null);

            // wait a second in case the connection gets dropped
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.log("Error in thread.sleep!", LogLevel.ERROR);
            }

            // in a do while loop in case the connection gets dropped from both sides
            // explained in more detail how it happens in Peer.disconnect()
            // but simply just waiting until it gets resolved
            Peer peer;
            do{
                peer = PeerList.getPeer(bootstrap_address);
                // stops once peer != null and sendMessage returns true
            }while(peer == null || !peer.sendMessage(Message.createPEER_DISCOVERY_REQUEST()));

        }

        // - api for sending messages
        new Thread(new QueryHandler()).start();

    }
}
