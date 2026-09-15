package problem1;
import java.io.*;
import java.net.*;

public class Server{
    private static final int PORT = 5000;
    private static final String s_dir = "./server_files/";

    public static void main(String[] args){
        File dir = new File(s_dir);
        if(!dir.exists()){
            dir.mkdir();
        }

        try ( ServerSocket serversocket = new ServerSocket(PORT)){
            System.out.println("server is running onport " + PORT);
            System.out.println("sharing directory: " + s_dir);
            while(true){
                Socket socket = serversocket.accept();
                ClientHandle handler = new ClientHandle(socket, s_dir);
                Thread t = new Thread(handler);
                t.start();
            }
        }
        catch (IOException e){
            System.out.println("Error starting server: " + e.getMessage());
        }
    }
}


