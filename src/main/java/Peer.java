import Util.LogLevel;
import Util.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Peer implements Runnable {
    // simply an interface for exchanging messages with a connected peer
    // plus a runnable for reading messages
    // also handling messages using methods from a static class

    // main functionalities
    //  - sendMessage
    //  - readMessage
    //  - reading messages



    // variables
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;


    // constructors
    // - add to PeerList, at the end of every constructor

    // 1 - from a socket
    // when someone connects to you
    public Peer(Socket socket){
        this.socket = socket;

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            Logger.log("Error in getting socket's input or output stream! Socket address:" + getAddress(), LogLevel.ERROR);
            return;
        }

        // peer adds itself to PeerList if no error happens
        PeerList.addPeer(this);
    }

    //  2 - from an address
    //  when you're trying to connect to someone
    public Peer(String address) throws IOException{
        // duplicate connection check
        address = Global.getNormalFormAddress(address);
        if(PeerList.getPeer(address) != null){
            throw new IOException();
        }
        if(Global.getMyIp().equals(address) || "127.0.0.1".equals(address)) {
//            Logger.log(address + " is your address!", LogLevel.WARN);
            throw new IOException();
        }

        // try to create socket
        try {
            this.socket = new Socket(address, Global.getSERVER_PORT());
        } catch (IOException e) {
            Logger.log("Error in trying to connect to: " + address, LogLevel.ERROR);
            throw e;
        }
        // socket successfully created at this point or crashed and returned

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            Logger.log("Error in getting socket's input or output stream! Socket address:" + getAddress(), LogLevel.ERROR);
            throw e;
        }

        // peer adds itself to PeerList if no error happens
        PeerList.addPeer(this);
    }
    // end  constructors


    // SEND message method
    public boolean sendMessage(Message message){
        try {
//            Logger.log("Sending message:" + message.toString(), LogLevel.DEBUG);
            writer.write(message.toString() + "\n");
            writer.flush();
            return true;
        } catch (IOException e) {
            Logger.log("Error in sending message to peer [" + getAddress() + "]", LogLevel.ERROR);
            return false;
        }
    }
    // end SEND message method

    // READ message method
    public String readMessage(){
        try {
            return reader.readLine();
        } catch (IOException e) {
            Logger.log("Error in readMessage:" + e.getMessage(), LogLevel.ERROR);
            Logger.log("Error in reading message from peer [" + getAddress() + "]", LogLevel.ERROR);
            return null;
        }
    }
    // end READ message method


    @Override
    public void run() {
        // just read messages and process them

        Thread.currentThread().setName("Peer[" + getAddress() + "]");

        String rawMessage;
        Message message;
        while(true){

            rawMessage = readMessage();
            if (rawMessage == null){
                Logger.log("rawMessage == null", LogLevel.DEBUG);
                break;
            }

            // - if track mode is on - print every message received (another query command TRACK ON/OFF + a variable in Global)
            if(Global.getTrack()){
                Logger.log("[" + getAddress() + "]:" + rawMessage, LogLevel.DEBUG);
            }

            try {
                message = new Message(rawMessage);
            } catch (IOException e) {
                continue;
            }

            // - process message
            MessageHandling.handle(message, this);

        }

        // - close connection without infinite recursion
        disconnect();
    }


    // disconnect
    // method for closing connection
    public boolean disconnect(){

        String address = getAddress();

        try {
            PeerList.removePeer(this);
            socket.close();
            writer.close();
            reader.close();
            Logger.log("Successfully closed connection with peer [" + address + "]", LogLevel.SUCCESS);

            // return here if it's a duplicate
            if(Global.getMyIp().equals(address) || "127.0.0.1".equals(address)){
                return true;
            }


            // try to connect again - in case the connection is closed from both sides
            // say P1 connects to P2 through connection A1 and adds it to PeerList
            // P2 connects to P1 through connection A2 and adds it to PeerList
            // P1 sees A2 as a duplicate and drops it
            // P2 sees A1 as a duplicate and drops it
            // connection is dropped but both nodes are still online
            // so the one with higher address tries to connect again
            // so the same thing doesn't repeat - when MAC sublayer you learned from Computer Networks comes in useful :D
            if(Global.getMyIp().compareTo(address) > 0){
                Logger.log("Bigger address, trying to reconnect ... ", LogLevel.DEBUG);
                new Peer(address);
            }

            return true;
        } catch (IOException e) {
            Logger.log("Error in trying to close connection with peer [" + address + "]", LogLevel.ERROR);
            return false;
        }
    }
    // end disconnect

    // getters

    // peer address
    public String getAddress(){
        InetAddress address = socket.getInetAddress();
        if(address != null){
            return address.getHostAddress();
        }
        return null;
    }

    // end getters
}
