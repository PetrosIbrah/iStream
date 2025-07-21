package com.app.isocketclientfx;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.*;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class ClientController implements Initializable {

    private final static Logger log = LogManager.getLogger(ClientController.class);

    private Stage stage;
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private String ServerIP = "localhost";
    private final int ServerPort = 8000;

    protected List<String> VideoList;
    protected List<String> VideoNameList;
    protected SSLSocket socket; //
    protected BufferedReader in;
    protected File Vid;
    protected String SelectedVideo;
    protected String SelectedFormat;
    protected String SelectedDisplay;
    protected String SelectedProtocol;
    protected double Speed = 0;
    protected String AdaptiveOnOff = "false";
    protected String FinalAdaptiveOnOff = "false";
    protected String Streaming = "false";
    protected int Seconds = 0;
    protected int RecCount = 0;
    protected String protocol = "";
    protected String StartRec;
    protected String Time;
    protected Thread CounterThread;
    protected String StreamingPort;

    public void HandleExit () {
        if (FinalAdaptiveOnOff.equals("false")) {
            try {
                OutputStream os = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(os, true);
                writer.println("Exit");
                log.info("Successfully Sent exit server speedtest results");
            } catch (IOException e) {
                log.error("Couldn't Send exit Speedtest results");
            }
        }
    }

    @Override
    public void initialize (URL url, ResourceBundle rb){

        SpeedTest();

        try {Thread.sleep(5000);
        }  catch (InterruptedException e) {
            log.error("Application couldn't pause for 5 seconds");
        }

        VideoList = new ArrayList<>();
        VideoNameList = new ArrayList<>();
        ChoiceList.getItems().clear();
        text3.setText("");

        formatChoice.getItems().clear();
        formatChoice.getItems().add("mkv");
        formatChoice.getItems().add("mp4");
        formatChoice.getItems().add("avi");

        DisplayChoice.getItems().clear();
        DisplayChoice.getItems().add("1080p");
        DisplayChoice.getItems().add("720p");
        DisplayChoice.getItems().add("480p");
        DisplayChoice.getItems().add("360p");
        DisplayChoice.getItems().add("240p");

        protocolChoice.getItems().clear();
        protocolChoice.getItems().add("None");
        protocolChoice.getItems().add("TCP");
        protocolChoice.getItems().add("UDP");
        protocolChoice.getItems().add("RTP/UDP");

        Connect();

        SendSpeedResults();

        ReceiveVideoList();

        ReceiveStreamingPort();
    }

    public void SpeedTest() {
        CountDownLatch CD = new CountDownLatch(1);
        SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(SpeedTestReport report) {
                Speed = report.getTransferRateBit().doubleValue() / 1000000;
                text.setText("Net Speed: " + String.format("%.2f", Speed) + "Mbs");
                String msg = "Successful completion of speedtest " + "Net Speed: " + String.format("%.2f", Speed) + "Mbs";
                log.info(msg);
                CD.countDown();
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                log.error("Couldn't complete speedtest");
                CD.countDown();
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {}
        });

        String downloadUrl = "http://speedtest.tele2.net/1MB.zip";
        speedTestSocket.startFixedDownload(downloadUrl, 5000, 1000);

        try{
            CD.await();
        } catch (InterruptedException e) {
            log.error("Couldn't await for speedtest");
        }
    }

    public void Connect() {
        try {
            System.setProperty("javax.net.ssl.trustStore", "TrustedStore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            System.setProperty("javax.net.ssl.keyStore", "KeyStore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "123456");
            System.setProperty("jdk.tls.client.protocols", "TLSv1.3");
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) sslSocketFactory.createSocket(ServerIP, ServerPort);
            socket.startHandshake();

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            textArea.setText("Connected to server.");
            log.info("Successful Connection to Server");
        } catch (Exception e) {
            log.fatal("Connection Problem");
        }

    }

    public void SendSpeedResults() {
        try {
            OutputStream os = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(os, true);
            String Msg = Double.toString(Speed);
            writer.println(Msg);
            log.info("Successfully sent server speedtest results");
        } catch (IOException e) {
            log.error("Couldn't Send Speedtest results");
        }
    }

    public void ReceiveVideoList() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            int VideoListSize = Integer.parseInt(in.readLine());
            VideoList.clear();
            for (int i = 0; i < VideoListSize; i++) {
                String video = in.readLine();
                VideoList.add(video);
            }

            for (String vid: VideoList) {
                String[] name = vid.split("-");
                if (!VideoNameList.contains(name[0])) {
                    VideoNameList.add(name[0]);
                }
            }

            ChoiceList.getItems().addAll(VideoNameList);

            log.info("Successfully Received video list from server");
        } catch (IOException e) {
            log.error("Couldn't Receive video list from server");
        }
    }

    public void ReceiveStreamingPort () {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StreamingPort = in.readLine();

            log.info("Successfully Received Streaming Port from server");
        } catch (IOException e) {
            log.error("Couldn't Receive Streaming Port from server");
        }

    }

    @FXML
    private Text text;

    @FXML
    private Text text1;

    @FXML
    private Text text3;

    @FXML
    private TextArea textArea;

    @FXML
    private ChoiceBox<String> ChoiceList;

    @FXML
    private ChoiceBox<String> formatChoice;

    @FXML
    private ChoiceBox<String> DisplayChoice;

    @FXML
    private ChoiceBox<String> protocolChoice;

    private PrintWriter writer;
    private Process process;

    @FXML
    private Text countText;

    @FXML
    private void StartRecording () {
        if (Streaming.equals("true")) {
            RecCount++;
            StartRec = "true";
            text3.setText("Recording");
            try {
                ProcessBuilder Command = new ProcessBuilder("cmd.exe");
                Command.redirectErrorStream(true);
                process = Command.start();

                writer = new PrintWriter(process.getOutputStream());

                String ffmpeg = System.getenv("ffmpeg");
                if (ffmpeg == null || ffmpeg.isEmpty()) {
                    ffmpeg = "ffmpeg";
                }
                // In case of incompatibility please replace "Stereo Mix (Realtek(R) Audio)" with the user's actual  audio device
                writer.println(ffmpeg + " -y -f gdigrab -i title=" + protocol +
                        " -f dshow -i audio=\"Stereo Mix (Realtek(R) Audio)\"" + // In case you dont want audio delete this line
                        " -c:v libx264 " +
                        "Recordings"
                        + File.separator + SelectedVideo + "-" + SelectedDisplay + "-" + RecCount + "." + SelectedFormat);
                writer.flush();

                CounterThread = new Thread(() ->{
                    Seconds = 0;
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(1000);
                            Seconds++;
                            SecondsToTime();
                            countText.setText(Time);
                        } catch (InterruptedException e) {
                            log.warn("Counter stopped");
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
                CounterThread.start();


                Thread outputThread = new Thread(() -> {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("Recorder: " + line);
                        }
                    } catch (IOException e) {
                        log.warn("Couldn't print recorder");
                    }
                });
                outputThread.setDaemon(true);
                outputThread.start();
                log.info("Recording started successfully");
            } catch (Exception e) {
                log.warn("Couldn't Start Recording");
            }
        }
    }

    @FXML
    private void StopRecording () {
        if (Streaming.equals("true")  && StartRec.equals("true")) {
            text3.setText("Recorded");
            writer.println("q");
            writer.flush();
            countText.setText("00:00:00");
            Seconds = 0;
            CounterThread.interrupt();
            log.info("Stopped Recording Successfully");
        }
    }

    @FXML
    private void AdaptiveStream(){
        if (!AdaptiveOnOff.equals("true")){
            text1.setText("Adaptive Streaming: ON");
            AdaptiveOnOff = "true";
        } else {
            text1.setText("Adaptive Streaming: OFF");
            AdaptiveOnOff = "false";
        }
    }

    @FXML
    private void StartPlayMedia() {
        SelectedVideo = ChoiceList.getValue();
        SelectedFormat = formatChoice.getValue();
        SelectedDisplay = DisplayChoice.getValue();
        SelectedProtocol = protocolChoice.getValue();
        int check = SendChoice();
        if (check==0){
            return;
        }
        SendAdaptiveOrNot();

        Streaming = "true";

        FinalAdaptiveOnOff = AdaptiveOnOff;
        if (FinalAdaptiveOnOff.equals("true")) {
            protocol = "Adaptive";
            SpeedtestLoop();
            AdaptiveUDPStream();
        }

        if (FinalAdaptiveOnOff.equals("false")) {
            if (SelectedProtocol.equals("None")) {
                if (SelectedDisplay.equals("240p")) {
                    protocol = "TCP";
                    TCPStream();
                } else if (SelectedDisplay.equals("360p") || SelectedDisplay.equals("480p")) {
                    protocol = "UDP";
                    UDPStream();

                } else if (SelectedDisplay.equals("720p") || SelectedDisplay.equals("1080p")) {
                    protocol = "RTP";
                    RTPStream();
                }
            } else {
                if (SelectedProtocol.equals("TCP")) {
                    protocol = "TCP";
                    TCPStream();
                } else if (SelectedProtocol.equals("UDP")) {
                    protocol = "UDP";
                    UDPStream();
                } else if (SelectedProtocol.equals("RTP/UDP")) {
                    protocol = "RTP";
                    RTPStream();
                }
            }
        }
    }

    public void TCPStream(){
        try {
            String ffplay = System.getenv("ffplay");
            if (ffplay == null || ffplay.isEmpty()) {
                ffplay = "ffplay";
            }
            ServerIP = socket.getInetAddress().getHostAddress();
            ProcessBuilder Command = new ProcessBuilder(
                    ffplay,
                    "-window_title", "TCP",
                    "tcp://" + ServerIP + ":4444"
            );
            Command.inheritIO();
            Command.redirectErrorStream(true);
            Process process = Command.start();

            log.info("Successful TCP Streaming | Client-side ");
        } catch (Exception e){
            e.printStackTrace();
            log.error("Unsuccessful TCP Streaming | Client-side ");
        }
    }

    public void UDPStream(){
        try {
            String ffplay = System.getenv("ffplay");
            if (ffplay == null || ffplay.isEmpty()) {
                ffplay = "ffplay";
            }
            ServerIP = socket.getInetAddress().getHostAddress();
            ProcessBuilder Command = new ProcessBuilder(
                    ffplay,
                    "-window_title", "UDP",
                    "udp://" + ServerIP + ":" + StreamingPort
            );
            Command.inheritIO();
            Command.redirectErrorStream(true);
            Process process = Command.start();
            log.info("Successful UDP Streaming | Client-side ");
        } catch (Exception e){
            log.error("Unsuccessful UDP Streaming | Client-side ");
        }
    }

    public void RTPStream () {
        try {
            try {
                Thread.sleep(1000);
                String filename = "SDPS" + File.separator + SelectedVideo + "-" + SelectedDisplay + "-" + SelectedFormat + ".sdp";
                InputStream input = socket.getInputStream();
                Vid = new File(filename);

                FileOutputStream fos = new FileOutputStream(Vid);
                byte[] buffer = new byte[4024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.close();
                textArea.setText("SDP Video received successfully.");
                log.info("Successful Receiving SDP file | Client-side RTP/UDP");
            } catch (Exception e){
                log.error("Unsuccessful Receiving SDP file | Client-side RTP/UDP");
            }

            try {
                String ffplay = System.getenv("ffplay");
                if (ffplay == null || ffplay.isEmpty()) {
                    ffplay = "ffplay";
                }
                ServerIP = socket.getInetAddress().getHostAddress();
                ProcessBuilder Command = new ProcessBuilder(
                        ffplay,
                        "-window_title", protocol,
                        "-protocol_whitelist", "file,rtp,udp",
                        "-i", Vid.getAbsolutePath()
                );
                Command.inheritIO();
                Command.redirectErrorStream(true);
                Process process = Command.start();

                log.info("Successful Stream SDP file | Client-side RTP/UDP");
            } catch (Exception e){
                log.error("Unsuccessful Stream SDP file | Client-side RTP/UDP");
            }
        } catch (Exception e){
            log.error("Unsuccessful RTP/UDP Streaming | Client-side ");
        }

    }

    public void AdaptiveUDPStream () {
        try {
            ServerIP = socket.getInetAddress().getHostAddress();
            String ffplay = System.getenv("ffplay");
            if (ffplay == null || ffplay.isEmpty()) {
                ffplay = "ffplay";
            }
            ProcessBuilder Command = new ProcessBuilder(
                    ffplay,
                    "-x", "1600", "-y", "900",
                    "-window_title", "Adaptive",
                    "-fflags", "nobuffer",
                    "udp://" + ServerIP + ":" + StreamingPort
            );
            Command.inheritIO();
            Command.redirectErrorStream(true);
            Process process = Command.start();
            log.info("Successful Adaptive UDP Streaming | Client-side ");
        } catch (Exception e){
            log.error("Unsuccessful Adaptive UDP Streaming | Client-side ");
        }
    }

    public int SendChoice() {
        if (ChoiceList.getValue() == null || formatChoice.getValue() == null || DisplayChoice.getValue() == null || protocolChoice.getValue() == null){
            textArea.setText("Choose a Video First");
            return 0;
        }
        try {
            OutputStream os = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(os, true);
            writer.println(SelectedVideo + "-" + SelectedDisplay + "." + SelectedFormat);
            writer.println(SelectedProtocol);
            log.info("Successfully sent Server Video choice");
        } catch (IOException e) {
            log.fatal("Couldn't Send server Video Choice");
        }
        return 1;
    }

    public void SendAdaptiveOrNot(){
        try {
            OutputStream os = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(os, true);
            writer.println(AdaptiveOnOff);
            log.info("Successfully sent Server Adaptive choice");
        } catch (IOException e) {
            log.fatal("Couldn't Send server Adaptive Choice");
        }
    }

    public void SpeedtestLoop(){
        Thread loop = new Thread(() -> {
            try {
                OutputStream os = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(os, true);
                while (true) {
                    try {
                        Thread.sleep(9500);
                    } catch (InterruptedException e) {
                        log.error("Couldn't Execute speedtest in loop");
                    }
                    SpeedTest();
                    String Msg = Double.toString(Speed);
                    writer.println(Msg);
                    log.info("Successfully sent server speedtest results loop");
                }
            } catch (IOException e) {
                log.error("Couldn't complete Speedtest Loop");
            }
        });
        loop.setDaemon(true);
        loop.start();
    }

    public void SecondsToTime () {
        LocalTime time = LocalTime.ofSecondOfDay(Seconds);
        Time = time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}