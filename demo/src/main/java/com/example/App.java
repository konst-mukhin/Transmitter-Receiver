package com.example;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application
{
    private Ports ports = new Ports();
    @Override public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("com/example/MyInterface.fxml"));
        Scene scene = new Scene(root);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                event.consume(); 
            }
        });
        stage.setResizable(false);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            ports.closePorts();
            System.out.println("Ports closed on window close.");
            System.exit(0);
        });
        stage.show();
    }
    
    public static void main(String[] args) throws Exception {
        Application.launch(args);
    }
}