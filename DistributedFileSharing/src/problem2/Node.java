package problem2;
import java.io.*;
import java.net.*;
import java.util.*;

public class Node {
        private static final String host = "127.0.0.1";
        private static final int tport = 6000;
        public static void main(String[] args){
            int port = Integer.parseInt(args[0]);
            String label = args[1];
            String sdir = args[2];
            if(!sdir.endsWith("/")){
                sdir += "/";
            }
  
            File dir = new File(sdir);
            if(!dir.exists()){
                dir.mkdir();
            }
            registerWithTracker(label, port, sdir);
            try{
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("[NODE] Node " + label + " running on port " + port);
                while(true){
                    Socket socket = serverSocket.accept();
                    NodeHandler h = new NodeHandler(socket, sdir, label, port);
                    Thread t = new Thread(h);
                    t.start();
                }
            } catch(IOException e){
                System.err.println("[NODE] Error starting node: " + e.getMessage());
            }
        }

        static void registerWithTracker(String label, int port, String sdir){
            File dir = new File(sdir);
            File[] item = dir.listFiles();
            List<File> flist = new ArrayList<File>();
            if(item != null){
                for (int i = 0; i< item.length; i++){
                    if(item[i].isFile()){
                        flist.add(item[i]);
                    }
                }
            }
            try{
                Socket socket = new Socket(host, tport);
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                out.writeUTF("NODE");
                out.writeUTF(label);
                out.writeUTF(host);
                out.writeInt(port);
                out.writeInt(flist.size());
                for (int i = 0; i < flist.size(); i++){
                    File f = flist.get(i);
                    out.writeUTF(f.getName());
                    out.writeLong(f.length());
                }
                out.flush();
                in.close();
                out.close();
                socket.close();
            }
            catch(IOException e){
                System.err.println("[" + label + "] + could not reach tracker: " + e.getMessage());

            }


        }

}
