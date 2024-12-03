package com.example;

public class HammingCode {

    public static String encode(String data) {
        int parityBits = 5;
        char[] encoded = new char[27];
        int j = 0;

        for (int i = 0; i < encoded.length; i++) {
            if (Math.pow(2, j) - 1 == i) {
                encoded[i] = '0';
                j++;
            } else {
                encoded[i] = data.charAt(i - j);
            }
        }

        StringBuilder parityBitsSequence = new StringBuilder();
        for (int i = 0; i < parityBits; i++) {
            int position = (int) Math.pow(2, i) - 1;
            int count = 0;
    
            for (int k = position; k < encoded.length; k += 2 * (position + 1)) {
                for (int l = k; l < k + position + 1 && l < encoded.length; l++) {
                    if (encoded[l] == '1') {
                        count++;
                    }
                }
            }
            if (count % 2 == 0) {
                encoded[position] = '0';
            } else {
                encoded[position] = '1';
            }
            parityBitsSequence.append(encoded[position]);
        }
    
        return parityBitsSequence.toString();
    }

    public static String decode(String packet) {

        String data = packet.substring(16, packet.length() - 5);
        String fcs = packet.substring(packet.length() - 5);
        int parityBits = 5;
        char[] dataArray = new char[27];
        int j = 0;
    
        for (int i = 0; i < dataArray.length; i++) {
            if (Math.pow(2, j) - 1 == i) {
                dataArray[i] = '0';
                j++;
            } else {
                dataArray[i] = data.charAt(i - j);
            }
        }
    
        StringBuilder calculatedFcs = new StringBuilder();
        for (int i = 0; i < parityBits; i++) {
            int position = (int) Math.pow(2, i) - 1;
            int count = 0;
    
            for (int k = position; k < dataArray.length; k += 2 * (position + 1)) {
                for (int l = k; l < k + position + 1 && l < dataArray.length; l++) {
                    if (dataArray[l] == '1') {
                        count++;
                    }
                }
            }
            if (count % 2 == 0) {
                calculatedFcs.append('0');
            } else {
                calculatedFcs.append('1');
            }            
        }
    
        int errorPosition = 0;
        for (int i = 0; i < parityBits; i++) {
            if (fcs.charAt(i) != calculatedFcs.charAt(i)) {
                errorPosition += Math.pow(2, i);
            }
        }
    
        if (errorPosition > 0) {
            int index = errorPosition - 1;
            System.out.println("ERROR INDEX " + index);
            if (dataArray[index] == '1') {
                dataArray[index] = '0';
            } else {
                dataArray[index] = '1';
            }            
        }
    
        StringBuilder correctedData = new StringBuilder();
        j = 0;
        for (int i = 0; i < dataArray.length; i++) {
            if (Math.pow(2, j) - 1 != i) {
                correctedData.append(dataArray[i]);
            } else {
                j++;
            }
        }
    
        return data.substring(0, 16) + correctedData.toString() + fcs;
    }
}
