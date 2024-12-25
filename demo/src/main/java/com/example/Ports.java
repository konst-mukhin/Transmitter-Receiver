package com.example;

import java.util.Optional;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class Ports {
    
    private static SerialPort serialPort1;
    private static SerialPort serialPort2;
    
    public Ports(){}
    
    private DataListener dataListener;

    private ErrorListener errorListener;

    private CountSendedBytes countSendedBytes;

    private SendBit sendBit;

    private Integer sendBytesCount = 0;

    private String newdata = "";

    private StringBuilder resdata = new StringBuilder();

    public interface DataListener {
        void onDataReceived(String data);
    }
    public interface SendBit {
        void onBitSend(String data);
    }

    public interface ErrorListener {
        void onErrorReceived(String error);
    }

    public interface CountSendedBytes {
        void onCountIncrement(Integer countSendBytes);
    }

    public Ports(DataListener listener, ErrorListener error, CountSendedBytes bytes, SendBit bit) {
        this.dataListener = listener;
        this.errorListener = error;
        this.countSendedBytes = bytes;
        sendBit = bit;
    }

    public Optional<String> message(String data, String sourcePort) {
        
        newdata += data;
        System.out.println(newdata);
        if (newdata.length() == 22) {
            System.out.println(newdata.length());
            String packet = PackageBuilder.createPackage(newdata, sourcePort);
            try {
                int attempt = 0;
                for (int i = 0; i < packet.length(); i++) {
                    while (CSMACD.isBusy());
                    serialPort1.writeString(packet.substring(i, i + 1));
                    if (CSMACD.isCollision()) {
                        attempt++;
                        if(sendBit != null) {
                            sendBit.onBitSend("!");
                        }
                        serialPort1.writeString(CSMACD.JAM_SIGNAL);
                        i--;
                        CSMACD.setDelay(attempt);
                    } else {
                        if(sendBit != null) {
                            sendBit.onBitSend(". ");
                        }
                        attempt = 0;
                    }
                }
                serialPort1.writeString(CSMACD.PACKET_END);
                this.sendBytesCount += packet.length();
                if (countSendedBytes != null) {
                    countSendedBytes.onCountIncrement(sendBytesCount);
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
            newdata = "";
            System.out.println(packet);
            return Optional.of(packet);
        } else {
            return Optional.empty();
        }
    }    

    public void firstPort() {
        try {
            if (serialPort1 != null && !serialPort1.isOpened()) {
                serialPort1.openPort();
                serialPort1.setParams(SerialPort.BAUDRATE_9600,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
                System.out.println("First port opened.");
            }
        } catch (SerialPortException ex) {
            if (errorListener != null) {
                errorListener.onErrorReceived("Error opening first port: " + ex);
            }
        }
    }

    public void secondPort() {
        try {
            if (serialPort2 != null && !serialPort2.isOpened()) {
                serialPort2.openPort();
                serialPort2.setParams(SerialPort.BAUDRATE_9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
                serialPort2.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
                System.out.println("Second port opened.");
            }
        } catch (SerialPortException ex) {
            if (errorListener != null) {
                errorListener.onErrorReceived("Error opening second port: " + ex);
            }
        }
    }

    public void chooseFirstPort(String name) {
        try {
            if (serialPort1 != null && serialPort1.isOpened()) {
                closePort(serialPort1);  
                Thread.sleep(500);
            }
            serialPort1 = new SerialPort(name);
            System.out.println("First port selected: " + name);
        } catch (InterruptedException ex) {
            if (errorListener != null) {
                errorListener.onErrorReceived("Error selecting first port: " + ex);
            }
        }
    }

    public void chooseSecondPort(String name) {
        try {
            if (serialPort2 != null && serialPort2.isOpened()) {
                closePort(serialPort2); 
                Thread.sleep(500); 
            }
            serialPort2 = new SerialPort(name);  
            System.out.println("Second port selected: " + name);
        } catch (InterruptedException ex) {
            if (errorListener != null) {
                errorListener.onErrorReceived("Error selecting second port: " + ex);
            }
        }
    }

    public void closePorts() {
        try {
            if (serialPort1 != null && serialPort1.isOpened()) {
                serialPort1.closePort();
                System.out.println("First port closed.");
            }
            if (serialPort2 != null && serialPort2.isOpened()) {
                serialPort2.closePort();
                System.out.println("Second port closed.");
            }
        } catch (SerialPortException ex) {
            if (errorListener != null) {
                errorListener.onErrorReceived("Error closing ports: " + ex);
            }
        }
    }

    public void closePort(SerialPort port) {
        try {
            if (port != null && port.isOpened()) {
                port.closePort();
                System.out.println(port.getPortName() + " closed.");
            }
        } catch (SerialPortException ex) {
            if (errorListener != null) {
                errorListener.onErrorReceived("Error closing port: " + ex);
            }
        }
    }

    private class PortReader implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.isRXCHAR() && event.getEventValue() > 0) {
                try {
                    String data = serialPort2.readString();
                    resdata.append(data);
                    if(data.contains(CSMACD.JAM_SIGNAL)){
                        resdata.delete(resdata.length()-2, resdata.length());
                        System.out.println(resdata.toString());
                    }
                    else if(data.contains(CSMACD.PACKET_END)){
                        resdata.delete(resdata.length()-1, resdata.length());
                        String packet = PackageBuilder.removeBitStaffing(resdata.toString());
                        if (dataListener != null) {
                            dataListener.onDataReceived(packet);
                        }
                        resdata.delete(0, resdata.length());
                    }
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
}
