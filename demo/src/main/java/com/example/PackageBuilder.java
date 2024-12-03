package com.example;

public class PackageBuilder {
    public static String createPackage(String data, String portString){
        var port = Integer.toBinaryString(Integer.parseInt(portString.replaceAll("\\D", "")));
        if(port.length() < 4){
            while(port.length()!= 4){
                port = "0" + port;
            }
        }
        String fcs = HammingCode.encode(data);
        return applyBitStaffing(PackageConstants.FLAG + PackageConstants.DESTINATION_ADRESS + port + data + fcs);
    }

    private static String applyBitStaffing(String packet) {
        StringBuilder stuffedPacket = new StringBuilder();
        String flagStart = PackageConstants.FLAG.substring(0, 7);
        char inverseBit = '1';

        for (int i = 0; i < packet.length(); i++) {
            stuffedPacket.append(packet.charAt(i));
            if (stuffedPacket.length() >= 15 && stuffedPacket.substring(stuffedPacket.length() - 7).equals(flagStart)) {
                stuffedPacket.append(inverseBit);
            }
        }
        
        return stuffedPacket.toString();
    }

    public static String removeBitStaffing(String packet) {
        StringBuilder originalPacket = new StringBuilder();
        String flagStart = PackageConstants.FLAG.substring(0, 7);
        char inverseBit = '1'; 

        for (int i = 0; i < packet.length(); i++) {
            if (originalPacket.length() >= 7 && originalPacket.substring(originalPacket.length() - 7).equals(flagStart)) {
                if (packet.charAt(i) == inverseBit) {
                    continue;
                }
            }
            originalPacket.append(packet.charAt(i));
        }
        String errorData = introduceError(originalPacket.toString().substring(16, originalPacket.toString().length() - 5));
        return HammingCode.decode(originalPacket.toString().substring(0, 16) + errorData + originalPacket.toString().substring(originalPacket.toString().length() - 5));
    }

    public static String introduceError(String data) {
        if (Math.random() < 0.3) {
            int position = (int) (Math.random() * data.length());
            char[] dataArray = data.toCharArray();
            if(dataArray[position] == '\n'){
                return data;
            }
            if (dataArray[position] == '1') {
                dataArray[position] = '0';
            } else {
                dataArray[position] = '1';
            }            
            return new String(dataArray);
        }
        return data;
    }
}
