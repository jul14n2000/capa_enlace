package org.example.capaenlaceme;

import java.util.ArrayList;

public class RellenoBytes {
    public static final byte Flag = 0x7E;
    public static final byte Esc = 0x7D;
    //realiza el rellleno de bytes y se agrega la bandera de inicio y fin
    public static byte[] rellenoBytes(byte[] payload)
    {
        ArrayList<Byte> salida = new ArrayList<>();
        salida.add(Flag);
        for(byte b : payload)
        {
            if(b==Flag ||b==Esc){
                salida.add(Esc);
                salida.add((byte)(b^ 0x20));
            }else
                salida.add(b);
        }
        salida.add(Flag);
        //convertimos el arrayList en un arreglo
        byte[] arregloBytes = new byte[salida.size()];
        for(int i = 0; i < salida.size(); i++)
            arregloBytes[i] = salida.get(i);

        return arregloBytes;

    }
}
