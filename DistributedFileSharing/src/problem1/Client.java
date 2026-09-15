package problem1;
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static final String host = "localhost";
    private static final int PORT = 5000;
    private static final String downloadDir = "./downloads/";

    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        System.out.print("name: ");
        String n = scanner.nextLine().trim();
        File d = new File(downloadDir);
        if (!d.exists()){
            d.mkdirs();
        } 

        try(
            Socket socket = new Socket(host, PORT);
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        ){
            out.writeUTF(n);
            out.flush();
            System.out.println("[CLIENT] Connected to server as " + n);

            while(true){
                String l = scanner.nextLine().trim();
                if(l.isEmpty()) continue;
                String[] parts = l.split(" ", 2);
                String c = parts[0].toLowerCase();
                
                switch(c){
                    case "list":
                        out.writeUTF("list");
                        out.flush();
                        String response = in.readUTF();
                        if(response.equals("OK")){
                            System.out.println("[SERVER] Available files:");
                            int fileCount = in.readInt();
                            System.out.println("[CLIENT] Files on server:");
                            for(int i = 0; i < fileCount; i++){
                                String filename = in.readUTF();
                                long s = in.readLong(); 
                                System.out.println(" - " + filename + " (" + s + " bytes)");
                            }
                        } else {
                            String errormsg = in.readUTF();
                            System.out.println("[CLIENT] Error: " + errormsg);
                        }
                        break;
                    
                    case "get":
                        if(parts.length < 2 ){
                            System.out.println("[CLIENT] Error: No filename provided.");
                            break;
                        }
                        else{
                            String filename = parts[1].trim();
                            out.writeUTF("get " + filename);
                            out.flush();
                            String getResponse = in.readUTF();
                            if(getResponse.equals("OK")){
                                long fileSize = in.readLong();
                                System.out.println("[SERVER] Sending " + filename + " (" + fileSize + " bytes)");
                                File file = new File(downloadDir + filename);
                                try(FileOutputStream fos = new FileOutputStream(file)){
                                    byte[] buffer = new byte[4096];
                                    long bytesReadTotal = 0;
                                    while(bytesReadTotal < fileSize){
                                        int bytesToRead = (int)Math.min(buffer.length, fileSize - bytesReadTotal);
                                        int bytesRead = in.read(buffer, 0, bytesToRead);
                                        if(bytesRead == -1) break;
                                        fos.write(buffer, 0, bytesRead);
                                        bytesReadTotal += bytesRead;
                                    }
                                    System.out.println("[CLIENT] Downloaded " + filename + " (" + fileSize + " bytes)");
                                } catch(IOException e){
                                    System.out.println("[CLIENT] Error saving file: " + e.getMessage());
                                }
                            } else {
                                String errormsg = in.readUTF();
                                System.out.println("[CLIENT] Error: " + errormsg);
                            }

                        }
                        break;
                    case "quit":
                        out.writeUTF("quit");
                        out.flush();
                        System.out.println("[CLIENT] You are successfully disconnected");
                        return;
                    default:
                        System.out.println("[CLIENT] Unknown command: " + c);
                }

            }
        }
        catch (IOException e){
            System.out.println("[CLIENT] Error: " + e.getMessage());
        }


    }

}
