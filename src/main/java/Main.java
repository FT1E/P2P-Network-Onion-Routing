import Util.LogLevel;
import Util.Logger;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        //  - start a server
        new Thread(new Server(Constants.getSERVER_PORT())).start();


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


        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Logger.log("Error in main at currentThread.wait()", LogLevel.ERROR);
        }

        // todo
        //      - start sending messages
        //      - api for it

        Logger.log("Current address list:" + PeerList.getAddressList(""), LogLevel.DEBUG);
    }
}
