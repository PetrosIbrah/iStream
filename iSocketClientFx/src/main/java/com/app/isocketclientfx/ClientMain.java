package com.app.isocketclientfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientMain extends Application {

    private final static Logger log = LogManager.getLogger(ClientMain.class);

    @Override
    public void start(Stage PrimaryStage) {
        DelSDP();
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(ClientMain.class.getResource("client.fxml"));
            Parent root = fxmlLoader.load();
            ClientController controller = fxmlLoader.getController();
            controller.setStage(PrimaryStage);


            Scene scene = new Scene(root, 600, 222);
            PrimaryStage.setTitle("iStream (Client)");
            PrimaryStage.setScene(scene);
            PrimaryStage.show();
            PrimaryStage.setOnCloseRequest((WindowEvent event) -> {
                controller.HandleExit();
            });
            log.info("Launched Application Successfully");
        } catch (IOException e){
            log.fatal("Couldn't launch Application");
        }

    }

    @Override
    public void stop() {
        DelSDP();
    }

    public void DelSDP(){
        File SDPfolder = new File("SDPS");
        for (File sdp : SDPfolder.listFiles()) {
            if (sdp.getName().endsWith(".sdp") && sdp.delete()) {
                log.info("Deleted an unnecessary sdp file.");
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}