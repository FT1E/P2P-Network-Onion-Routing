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

        // since everyone is connecting to peer1
        // to make sure the node has started the server first
        // before they try to connect to him
        if(!"peer1".equals(System.getenv("PEER_ID"))) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Logger.log("Error in main at currentThread.wait()", LogLevel.ERROR);
            }
        }else{
            // also peer1 should make sure peer2 has started his server
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.log("Error in main at currentThread.wait()", LogLevel.ERROR);
            }
        }




        //  - connect to a booststrap address (env variable) if available
        //  - otherwise you can connect to someone with CONNECT command in QueryHandler
        //  - once you connect to someone you immediately send them a PEER_DISCOVERY request
        if(System.getenv("BOOTSTRAP_ADDRESS") != null) {
            while (true) {
                try {
                    Peer peer = new Peer(System.getenv("BOOTSTRAP_ADDRESS"));
                    Thread.sleep(1000);
                    peer.sendMessage(Message.createPEER_DISCOVERY_REQUEST());
                    break;
                } catch (IOException | InterruptedException e) {
                    // logger in constructor
                    Logger.log("Error in Thread.sleep!", LogLevel.DEBUG);
                    continue;
                }
            }
        }

        // - api for sending messages
        new Thread(new QueryHandler()).start();

    }
}
