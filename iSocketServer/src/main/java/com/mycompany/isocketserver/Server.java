package com.mycompany.isocketserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server implements Runnable {
    private SSLSocket socket;
    private static List<String> VideoList = new ArrayList<>();
    private static List<String> ClientVideoList = new ArrayList<>();
    private String ClientChoice;
    private String ClientProtocol;
    private double ClientSpeed;
    private String Protocol;
    private static File Directory;
    private static String ClientIp;
    private static int ClientCount = 0;
    private static int Port;
    private static String VideoTitle;
    private static String Resolution;
    private static String Extension;
    private static Process cProcess;
    private static int Seconds = 0;
    private static String AdaptOrNot;
    private String Streamingport = "";
    private static String ffmpegloc;
    private static String ffprobeloc;

    private static final Logger log = LogManager.getLogger(Server.class);

    public Server(SSLSocket socket) {
        this.socket = socket;
    }

    public static void VideoPopulation() {
        try {
            FFmpegExecutor executor;
            FFmpeg ffmpeg = new FFmpeg(ffmpegloc);
            FFprobe ffprobe = new FFprobe(ffprobeloc);

            String[] Formats = {"mkv", "mp4", "avi"};
            String[] FormatNames = {"matroska", "mp4", "avi"};
            String[] Codecs = {"libx264", "libx264", "mpeg4"};
            String[] heights = {"240", "360", "480", "720", "1080"};
            String[] widths = {"426", "640", "854", "1280", "1920"};

            for (String Video : VideoList) {

                String Input = "AvailableVideos/" + Video;
                if (!new File(Input).exists()) {
                    continue;
                }

                String[] videoname = Video.split("-");
                String Name = videoname[0];
                for (int j = 0; j < Formats.length; j++) {
                    String Codec = Codecs[j];
                    String FormatName = FormatNames[j];
                    String format = Formats[j];
                    for (int i = 0; i < heights.length; i++) {
                        String height = heights[i];
                        String width = widths[i];
                        String Output = "AvailableVideos/" + Name + "-" + height + "p." + format;

                        File checkfile = new File(Output);
                        if (checkfile.exists()) {
                            continue;
                        }

                        FFmpegBuilder builder = new FFmpegBuilder()
                                .setInput(Input)
                                .overrideOutputFiles(true)
                                .addOutput(Output)
                                .setFormat(FormatName)
                                .setVideoResolution(Integer.parseInt(width), Integer.parseInt(height))
                                .setVideoCodec(Codec)
                                .done();

                        executor = new FFmpegExecutor(ffmpeg, ffprobe);
                        executor.createJob(builder).run();
                        log.info("Converted [" + Output + "] successfully.");
                    }
                }

            }
            log.info("Checked all videos and made new if needed.");
        } catch (Exception e) {
            log.error("Couldn't check or create videos.");
        }
    }

    public static void main(String[] args) {
        ffmpegloc = System.getenv("ffmpeg");
        if (ffmpegloc == null || ffmpegloc.isEmpty()) {
            ffmpegloc = "ffmpeg";
        }
        ffprobeloc = System.getenv("ffprobe");
        if (ffprobeloc == null || ffprobeloc.isEmpty()) {
            ffprobeloc = "ffprobe";
        }
        
        // Create list of Available videos
        Directory = new File("AvailableVideos");
        File[] Vids = Directory.listFiles();
        if (Vids != null) {
            for (File vid : Vids) {
                VideoList.add(vid.getName());
            }
        }

        VideoPopulation();

        // Delete All Previous Sdp files
        File SDPfolder = new File("SDPS");
        for (File sdp : SDPfolder.listFiles()) {
            if (sdp.getName().endsWith(".sdp") && sdp.delete()) {
                log.info("Deleted an unnesessery sdp file.");
            }
        }

        // Recreate the vide list after Population
        Vids = Directory.listFiles();
        VideoList.clear();
        if (Vids != null) {
            for (File vid : Vids) {
                VideoList.add(vid.getName());
            }
        }

        // Start Server
        try {
            Port = Integer.parseInt(args[0]);
            System.setProperty("javax.net.ssl.keyStore", "KeyStore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "123456");
            SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(Port);
            log.info("Server Initiated with port " + Port);
            log.info("===============================");

            // Server Execution while loop
            while (true) {
                ClientCount++;
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                log.info("A Client has connected to Server (Port = " + Port + ")");
                new Thread(new Server(socket)).start();
            }

        } catch (Exception e) {
            int Port = Integer.parseInt(args[0]);
            log.fatal("Couldn't properly initiate Server (Port = " + Port + ")");
        }
    }

    @Override
    public void run() {
        Seconds = 0;
        ClientIp = socket.getInetAddress().getHostAddress();
        try {
            ReceiveSpeed();
            
            FilterList();

            // Send Video List To client
            SendtoClient();

            SendStreamingPort();

            // Receive video and protocol Choice
            ReceiveChoice();

            //Receive Adapt Chioce
            ReceiveAdaptChoice();
            
            ChooseProtocol();

            BufferVideo();

        } catch (Exception e) {
            log.error("Lost connection with client (" + ClientIp + ")");
        }
    }

    public boolean ReceiveSpeed() {
        try {
            String tmp;
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            tmp = reader.readLine();
            if (tmp.equals("Exit")) {
                cProcess.destroy();
                try {
                    socket.close();
                } catch (IOException e2) {
                    log.error("Couldn't shut down socket");
                }
            }
            ClientSpeed = Double.parseDouble(tmp);
            log.info("Client (" + ClientIp + ") has internet speed of " + String.format("%.2f", ClientSpeed));
        } catch (Exception e) {
            log.warn("Couldn't retrieve Speed from Client (" + ClientIp + ")");
            return false;
        }
        return true;
    }

    public void FilterList() {
        double kb = ClientSpeed * 1000;
        if (kb >= 300) {
            for (String vid : VideoList) {
                if (vid.contains("240p")) {
                    ClientVideoList.add(vid);
                }
            }
        }

        if (kb >= 400) {
            for (String vid : VideoList) {
                if (vid.contains("360p")) {
                    ClientVideoList.add(vid);
                }
            }
        }

        if (kb >= 500) {
            for (String vid : VideoList) {
                if (vid.contains("480p")) {
                    ClientVideoList.add(vid);
                }
            }
        }

        if (kb >= 1500) {
            for (String vid : VideoList) {
                if (vid.contains("720p")) {
                    ClientVideoList.add(vid);
                }
            }
        }

        if (kb >= 3000) {
            for (String vid : VideoList) {
                if (vid.contains("1080p")) {
                    ClientVideoList.add(vid);
                }
            }
        }
    }

    public void SendtoClient() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(VideoList.size());
            for (String vids : VideoList) {
                out.println(vids);
            }
            log.info("Sent the proper video list to Client (" + ClientIp + ")");
        } catch (Exception e) {
            log.error("Couldn't send video list to Client (" + ClientIp + ")");
        }
    }

    public void SendStreamingPort() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            long currentTime = System.currentTimeMillis();
            int timeOffset = (int)(currentTime % 1000);
            Streamingport = String.valueOf(7000 + (100 * (Port - 8000)) + timeOffset + ClientCount);
            out.println(Streamingport);
            log.info("Sent the proper Streaming Port to Client (" + ClientIp + ")");
        } catch (Exception e) {
            log.error("Couldn't send Streaming Port to Client (" + ClientIp + ")");
        }
    }

    public void ReceiveChoice() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ClientChoice = reader.readLine();
            ClientProtocol = reader.readLine();
            log.info("Client (" + ClientIp + ") chooses: " + ClientChoice);
        } catch (Exception e) {
            log.info("Couldn't retrieve video choice from Client (" + ClientIp + ")");
        }

    }

    public void ReceiveAdaptChoice() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            AdaptOrNot = reader.readLine();
            if (AdaptOrNot.equals("true")) {
                log.info("Client (" + ClientIp + ") chooses Adaptive Streaming");
            } else {
                log.info("Client (" + ClientIp + ") chooses Normal Streaming ");
            }
        } catch (Exception e) {
            log.info("Couldn't retrieve adapt choice from Client (" + ClientIp + ")");
        }
    }

    public void ChooseProtocol() {
        if (ClientProtocol.equals("None")) {
            if (ClientChoice.contains("240p")) {
                Protocol = "TCP";
            } else if (ClientChoice.contains("360p") || ClientChoice.contains("480p")) {
                Protocol = "UDP";
            } else if (ClientChoice.contains("720p") || ClientChoice.contains("1080p")) {
                Protocol = "RTP/UDP";
            } else {
                Protocol = "UNKNOWN";
                return;
            }
        } else {
            Protocol = ClientProtocol;
        }
    }

    public void BufferVideo() {
        // Crop Name and title to edit to use incase of adaptive updates
        int dot = ClientChoice.lastIndexOf('.');
        String NameAndResolution = ClientChoice.substring(0, dot);
        Extension = ClientChoice.substring(dot + 1);

        int minus = NameAndResolution.lastIndexOf('-');
        VideoTitle = NameAndResolution.substring(0, minus);
        Resolution = NameAndResolution.substring(minus + 1);
        // =================================================

        File videoFile = new File("AvailableVideos" + File.separator + ClientChoice);
        if (!videoFile.exists()) {
            log.warn("Couldn't find Client's (" + ClientIp + ") video of choice in out repository");
            try {
                socket.close();
            } catch (IOException e2) {
                log.error("Couldn't shut down socket");
            }
            return;
        }

        if (AdaptOrNot.equals("true")) {
            ReceiveSpeedLoop();
            UDPStream(videoFile);
        } else if (Protocol.equals("TCP")) {
            TCPStream(videoFile);
        } else if (Protocol.equals("UDP")) {
            UDPStream(videoFile);
        } else if (Protocol.equals("RTP/UDP")) {
            RTPStream(videoFile);
        }
        if (AdaptOrNot.equals("false")) {
            SafeExit();
        }
    }

    public void ReceiveSpeedLoop() {
        Thread loop = new Thread(() -> {
            boolean Check;
            while (true) {
                Check = ReceiveSpeed();
                Seconds = Seconds + 10;
                if (!Check) {
                    break;
                }
                Adapt();
            }
        });
        loop.setDaemon(true);
        loop.start();
    }

    public void StopPlayer() {
        if (cProcess != null && cProcess.isAlive()) {
            cProcess.destroy();
            try {
                cProcess.waitFor();
            } catch (InterruptedException e) {
                log.error("Interrupted");
            }
        }
        cProcess = null;
    }

    public void TCPStream(File Streamed) {
        try {
            ProcessBuilder Command = new ProcessBuilder(
                    ffmpegloc,
                    "-i", Streamed.getAbsolutePath(),
                    "-f", "mpegts",
                    "tcp://" + ClientIp + ":4444?listen"
            );
            Command.inheritIO();
            Command.redirectErrorStream(true);
            cProcess = Command.start();
            log.info("Successful TCP Server-end streaming");
        } catch (Exception e) {
            log.error("Unsuccessful TCP Server-end streaming");
        }
    }

    public void UDPStream(File Streamed) {
        try {
            ProcessBuilder Command = new ProcessBuilder(
                    ffmpegloc,
                    "-re",
                    "-ss", String.valueOf(Seconds),
                    "-i", Streamed.getAbsolutePath(),
                    "-f", "mpegts",
                    "udp://" + ClientIp + ":" + Streamingport
            );
            Command.inheritIO();
            Command.redirectErrorStream(true);
            cProcess = Command.start();
            log.info("Successful UDP Server-end streaming");
        } catch (Exception e) {
            log.error("Unsuccessful UDP Server-end streaming");
        }
    }

    public void RTPStream(File Streamed) {
        String sdpname = "video-" + ClientCount + "-" + Port + ".sdp";
        try {
            try {
                ProcessBuilder Command = new ProcessBuilder(
                        ffmpegloc,
                        "-re",
                        "-i", Streamed.getAbsolutePath(),
                        "-an",
                        "-c:v", "copy",
                        "-f", "rtp",
                        "-sdp_file", "SDPS" + File.separator + sdpname,
                        "rtp://" + ClientIp + ":" + Streamingport
                );

                Command.inheritIO();
                Command.redirectErrorStream(true);
                cProcess = Command.start();
                log.info("Successful production of SDP file Server-end | RTP/UDP");
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("Unsuccessful production of SDP file Server-end | RTP/UDP");
            }

            try {
                byte[] buffer = new byte[4096];
                File sdp = new File("SDPS" + File.separator + sdpname);
                FileInputStream fis = new FileInputStream(sdp);
                OutputStream os = socket.getOutputStream();

                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                fis.close();
                os.flush();
                socket.shutdownOutput();
            } catch (Exception e) {
                log.error("Unsuccessful sending SDP file Server-end | RTP/UDP");
            }

            log.info("Successful RTP/UDP Server-end streaming");
        } catch (Exception e) {
            log.error("Unsuccessful RTP/UDP Server-end streaming");
        }
    }

    public void Adapt() {
        double kb = ClientSpeed * 1000;
        String OldResolution = Resolution;
        if (Resolution.equals("240p")) {
            if (kb > 700) {
                Resolution = "360p";
            }

        } else if (Resolution.equals("360p")) {
            if (kb > 1000) {
                Resolution = "480p";
            } else if (kb < 750) {
                Resolution = "240p";
            }

        } else if (Resolution.equals("480p")) {
            if (kb > 2000) {
                Resolution = "720p";
            } else if (kb < 1000) {
                Resolution = "360p";
            }

        } else if (Resolution.equals("720p")) {
            if (kb > 4000) {
                Resolution = "1080p";
            } else if (kb < 2500) {
                Resolution = "480p";
            }

        } else if (Resolution.equals("1080p")) {
            if (kb < 4500) {
                Resolution = "480p";
            }

        }

        if (!OldResolution.equals(Resolution)) {
            StopPlayer();

            try {
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("later");
            }

            String NewVideo = VideoTitle + "-" + Resolution + "." + Extension;
            File videoFile = new File("AvailableVideos" + File.separator + NewVideo);
            UDPStream(videoFile);
        }
    }

    public void SafeExit() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String Exit = reader.readLine();

            if (Exit.equals("Exit")) {
                StopPlayer();
                log.warn("Client Exited");
            }
        } catch (Exception e) {
            log.info("Couldn't retrieve video choice from Client (" + ClientIp + ")");
        }
    }
}
