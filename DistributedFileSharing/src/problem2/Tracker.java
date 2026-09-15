package problem2;
import java.io.*;
import java.net.*;
import java.util.*;

public class Tracker {
    private static final int PORT = 6000;
    static ArrayList<FileEntry> catalogue = new ArrayList<FileEntry>();
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[TRACKER] Tracker runnning on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                TrackerHandler h = new TrackerHandler(socket);
                Thread t = new Thread(h);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("[TRACKER] Error starting tracker: " + e.getMessage());
        }
    } 

}
