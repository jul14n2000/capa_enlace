package org.example.capaenlaceme;

//CALCULA EL CRC PARA EL PAYLOAD
public class CRC8 {
    private static final int polinomio = 0x07; // se corresponde con el polinomio  x2 +x1 +1 polinomio generador

    public static int calcularCRC(byte[] payload){
        int crc = 0x00;
        for(byte b : payload){
            crc = crc ^ b & 0xff;
            for(int i =0 ; i<8;i++){
                if((crc & 0x80)!=0)
                    crc = ((crc<<1)^polinomio) & 0xFF;
                else
                    crc = (crc<<1) & 0xFF;
            }
        }
        return crc & 0xff;
    }




}
