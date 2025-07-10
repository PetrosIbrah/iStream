package com.mycompany.isocketserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoadBalancer {

    private static final int[] SERVER_PORTS = {8001, 8002, 8003, 8004};
    private static final String SERVER_HOST = "localhost";
    private static int PortNum = 2;
    private static int ServerID = 0;

    private static final Logger log = LogManager.getLogger(LoadBalancer.class);

    public static void main(String[] args) {
        try {
            for (int port : SERVER_PORTS) {
                StartServer(port);
                Thread.sleep(1000);
                ServerID++;
            }
        } catch (Exception e) {
            log.fatal("Thread Exception - Couldn't lunch Server");
        }

        try {
            System.setProperty("javax.net.ssl.trustStore", "TrustedStore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            System.setProperty("javax.net.ssl.keyStore", "KeyStore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "123456");
            System.setProperty("jdk.tls.client.protocols", "TLSv1.3");
            SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket LoadBalanecrSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(8000);

            log.info("Initiated Load Balanecr at socket 8000");
            while (true) {
                try {
                    SSLSocket ClientSocket = (SSLSocket) LoadBalanecrSocket.accept();
                    PortNum = (PortNum + 1) % SERVER_PORTS.length;
                    log.info("Sent a client to Server with port " + (8000 + PortNum + 1));
                    new Thread(() -> ConnectionHandler(ClientSocket)).start();
                } catch (Exception e) {
                    log.error("Couldnt accept client | Load Balanncer");
                }
            }
        } catch (Exception e) {
            log.fatal("Couldnt bind socket with Load Balanncer");
        }

    }

    public static void ConnectionHandler(Socket ClientSocket) {

        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket ServerSocket = (SSLSocket) factory.createSocket(SERVER_HOST, SERVER_PORTS[PortNum]);
            ServerSocket.startHandshake();

            BufferedReader ReadFromClient = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));
            PrintWriter WriteToClient = new PrintWriter(ClientSocket.getOutputStream(), true);

            BufferedReader ReadFromServer = new BufferedReader(new InputStreamReader(ServerSocket.getInputStream()));
            PrintWriter WriteToServer = new PrintWriter(ServerSocket.getOutputStream(), true);
            // =========================================================================
            // Speed
            String Request = ReadFromClient.readLine();
            WriteToServer.println(Request);

            // Video List Size
            int VideoListSize = Integer.parseInt(ReadFromServer.readLine());
            WriteToClient.println(VideoListSize);
            
            // Video list
            for (int i = 0; i < VideoListSize; i++) {
                String response = ReadFromServer.readLine();
                WriteToClient.println(response);
            }
            
            // Streming port
            String Streamport;
            Streamport = ReadFromServer.readLine();
            WriteToClient.println(Streamport);
            
            // Receive choice
            Request = ReadFromClient.readLine();
            WriteToServer.println(Request);
            
            // Protocol
            String protocol = ReadFromClient.readLine();
            WriteToServer.println(protocol);
            
            // Receive Adaptive Choice
            String Adaptive = ReadFromClient.readLine();
            WriteToServer.println(Adaptive);

            // Stream everything the client sends to the server if needed
            if (Adaptive.equals("true")) {
                // Speedtest loop
                new Thread(() -> {
                    String msg;
                    try {
                        while ((msg = ReadFromClient.readLine()) != null) {
                            WriteToServer.println(msg);
                        }
                    } catch (IOException e) {
                        // εξοδος adaptive
                        WriteToServer.println("Exit");
                        log.warn("Couldn't retrieve Speed from Client | Load Balancer");
                    }
                }).start();
            } else if (Request.contains("720p") || Request.contains("1080p") || protocol.equals("RTP/UDP")) {
                // in case of rtp, extra communication
                // SDP
                InputStream is = ServerSocket.getInputStream();
                OutputStream os = ClientSocket.getOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                ClientSocket.shutdownOutput();
            }

            if (Adaptive.equals("false")) {
                // εξοδος σε περιπτωση adaptive
                Request = ReadFromClient.readLine();
                WriteToServer.println(Request);
            }

            log.info("Successful communication completion");
        } catch (IOException e) {
            log.error("Unsuccessful communication completion");
            return;
        }

    }

    public static void StartServer(int port) {
        String classpath = "target" + File.separator + "iSocketServer-1.0-SNAPSHOT.jar"; 
        try {
            ProcessBuilder RunServer = new ProcessBuilder(
                    "java",
                    "-jar", classpath,
                    Integer.toString(port)
            );
            RunServer.inheritIO();
            RunServer.start();
            log.info("Server Port " + port + "  initiated | Load Balancer");
        } catch (Exception e) {
            log.fatal("Couldn't lunch Server Port " + port);
        }
    }
}
