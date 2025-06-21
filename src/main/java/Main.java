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
        String bootstrap_address = System.getenv("BOOTSTRAP_ADDRESS");
        if(bootstrap_address != null) {
            do {
                try {
                    Peer peer = new Peer(bootstrap_address);
                    peer.sendMessage(Message.createPEER_DISCOVERY_REQUEST());
                    break;
                } catch (IOException e) {
                    // logger in constructor
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Logger.log("Error in thread.sleep!", LogLevel.ERROR);
                }

            } while (PeerList.getPeer(bootstrap_address) != null);
        }
        // - api for sending messages
        new Thread(new QueryHandler()).start();

    }
}
