import Util.LogLevel;
import Util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    // main purpose - allowing others to connect to you

    private final int port;
    private ServerSocket server;

    public Server(int port){
        this.port = port;
    }



    @Override
    public void run() {

        Thread.currentThread().setName("Server");

        // create a server socket
        try{
            server = new ServerSocket(port);
        }catch(IOException e){
            Logger.log("Couldn't start server!", LogLevel.ERROR);
            return;
        }
        Logger.log("Server listening for connections on port " + port);


        // accept incoming connections
        Socket socket = null;
        while(true){

            try {
                socket = server.accept();
            } catch (IOException e) {
                Logger.log("Error in accepting a connection!");
            }

            if(socket == null) {
                continue;
            }

            //  - create a new client - every client adds itself to peerlist in its constructor if no error occured
            new Peer(socket);


        }
    }
}
