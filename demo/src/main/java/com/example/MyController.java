package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;
import jssc.SerialPortList;

public class MyController implements Ports.DataListener, Ports.ErrorListener, Ports.CountSendedBytes  {

    private Ports port;

    private int transmittedBytesCount = 0;

    private String portstr = "";

    private Timeline timeline;

    @FXML
    private Label inputLabel;

    @FXML
    private Label debugLabel;

    @FXML
    private AnchorPane errorPane;

    @FXML
    private TextArea errorArea;

    @FXML
    private Label outputLabel;

    @FXML
    private Label parametersLabel;

    @FXML
    private Label chooseLabel;

    @FXML
    private Label bytesLabel;

    @FXML
    private Label errorLabel;

    @FXML
    private TextArea inputArea;

    @FXML
    private TextArea outputArea;

    @FXML
    private ComboBox<String> inputBox;

    @FXML
    private ComboBox<String> outputBox;

    @FXML
    private TextArea controlArea;

    @FXML
    private AnchorPane controlPane;

    @FXML
    private TextFlow packetDisplay;

    @FXML
    public void initialize() {
        port = new Ports(this, this, this);

        inputArea.setDisable(true);
        Text config = new Text("BAUDRATE_9600\nDATABITS_8\nSTOPBITS_1\nPARITY_NONE\n");
        packetDisplay.getChildren().add(config);

        inputBox.setOnShowing(event -> updatePortList(inputBox));
        outputBox.setOnShowing(event -> updatePortList(outputBox));

        inputBox.setOnAction(event -> {
            String selectedPort1 = inputBox.getSelectionModel().getSelectedItem();
            if (selectedPort1 != null) {
                updateOutputPortVisibility(selectedPort1);
                port.chooseFirstPort(selectedPort1);
                port.firstPort();
                portstr = selectedPort1;
                inputArea.setDisable(false);
            }
        });
    
        outputBox.setOnAction(event -> {
            String selectedPort2 = outputBox.getSelectionModel().getSelectedItem();
            if (selectedPort2 != null) {
                updateInputPortVisibility(selectedPort2);
                port.chooseSecondPort(selectedPort2);
                port.secondPort();
            }
        });

        inputArea.textProperty().addListener((info, oldValue, newValue) -> {
            String data = newValue.substring(newValue.length() - 1);
            var result = port.message(data, portstr);
            //if (result.isPresent()) {
                displayPacketWithColors(result.get());
            //}
        });
        restrictInputToBinary(inputArea);
        setupPeriodicUpdate();
        port.closePorts();
    }

    public void displayPacketWithColors(String packet) {
        packetDisplay.getChildren().clear();

        String newpacket = packet.replace("\n", "\\n");
        
        Text config = new Text("BAUDRATE_9600\nDATABITS_8\nSTOPBITS_1\nPARITY_NONE\n");
        packetDisplay.getChildren().add(config);

        String flag = PackageConstants.FLAG;
        String flagStart = flag.substring(0, 7);
        char inverseBit = flag.charAt(7) == '0' ? '1' : '0';

        int i = 0;
        for (; i < 8; i++) {
            Text bitText = new Text(String.valueOf(newpacket.charAt(i)));
            packetDisplay.getChildren().add(bitText);
        }
        
        StringBuilder currentBits = new StringBuilder(flagStart);
        for (; i < newpacket.length(); i++) {
            char currentBit = newpacket.charAt(i);
            Text bitText = new Text(String.valueOf(currentBit));
            
            if (currentBits.toString().equals(flagStart) && currentBit == inverseBit) {
                bitText.setFill(Color.RED);
                currentBits = new StringBuilder();
            } else {
                bitText.setFill(Color.BLACK);
                currentBits.append(currentBit);
                if (currentBits.length() > 7) {
                    currentBits.deleteCharAt(0);
                }
            }

            if (i == newpacket.length() - 5) {
                Text space = new Text(" ");
                packetDisplay.getChildren().add(space);
            }
            
            packetDisplay.getChildren().add(bitText);
        }
    }

    private void restrictInputToBinary(TextArea textArea) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getText();
            if (newText.matches("[01\n]*")) {
                return change;
            }
            return null;
        };
        TextFormatter<String> textFormatter = new TextFormatter<>(new DefaultStringConverter(), "", filter);
        textArea.setTextFormatter(textFormatter);
    }
    

    public void updateOutputPortVisibility(String selectedPort1) {
        int selectedPortNum = Integer.parseInt(selectedPort1.replaceAll("\\D", ""));
        outputBox.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    int portNum = Integer.parseInt(item.replaceAll("\\D", ""));
                    if (portNum == selectedPortNum || portNum == selectedPortNum + 1 ) {
                        setDisable(true);
                        setStyle("-fx-opacity: 0.5;");
                    } else {
                        setDisable(false);
                        setStyle("-fx-opacity: 1;");
                    }
                    setText(item);
                }
            }
        });
    }
    
    public void updateInputPortVisibility(String selectedPort2) {
        int selectedPortNum = Integer.parseInt(selectedPort2.replaceAll("\\D", ""));
        inputBox.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    int portNum = Integer.parseInt(item.replaceAll("\\D", ""));
                    if (portNum == selectedPortNum || portNum == selectedPortNum - 1 ) {
                        setDisable(true);
                        setStyle("-fx-opacity: 0.5;");
                    } else {
                        setDisable(false);
                        setStyle("-fx-opacity: 1;");
                    }
                    setText(item);
                }
            }
        });
    }

    public void updatePortList(ComboBox<String> comboBox) {
        List<String> portNames = new ArrayList<>(Arrays.asList(SerialPortList.getPortNames()));
        portNames.remove("COM3");
        portNames.remove("COM4");
        portNames.remove("COM5");
        portNames.remove("COM6");
        comboBox.setItems(FXCollections.observableArrayList(portNames)); 
    }

    @Override
    public void onDataReceived(String data) {
        String cleanData = PackageBuilder.removeBitStaffing(data);
        Platform.runLater(() -> {
            outputArea.appendText(cleanData.substring(16, cleanData.length() - 5));
        });
    }

    @Override 
    public void onErrorReceived(String error) {
        Platform.runLater(() -> {
            errorLabel.setText(error);
            if(error.contains("first")){
                inputBox.getSelectionModel().clearSelection();
            }
            if(error.contains("second")){
                outputBox.getSelectionModel().clearSelection();
            }
        });
    }

    private void setupPeriodicUpdate() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            Platform.runLater(() -> {
                bytesLabel.setText(" Bytes transmitted: " + transmittedBytesCount);
            });
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @Override
    public void onCountIncrement(Integer countBytes) {
        transmittedBytesCount = countBytes;
    }
}
