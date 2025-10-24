package org.example.capaenlaceme;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import static org.example.capaenlaceme.RellenoBytes.Flag;
import static org.example.capaenlaceme.RellenoBytes.rellenoBytes;

public class CtorTrama {
    //caracteres especiales
    public static final byte Flag = 0x7E;
    public static final byte Esc = 0x7D;

    //tipos de tramas
    public static final byte tipo_DATA = 0x01;
    public static final byte tipo_ACK = 0x02;
    public static final byte tipo_NAK = 0x03;


    // estructura de una trama NAK --> [flag|type_NAK|secuencia|payload|crc|flag]
    // ESTRUCTURA DE LA TRAMA  [FLAG|TYPE(1byte)|SEQUENCIA(1 byte)|PAYLOAD|CRC(1 byte)|FLAG]

    //Solo aplicamos rellenos de bytes a las tramas de datos
    public static byte[] ctorTramaDatos(int seq, byte[] datos){
        //construir campo de control y nro de secuencia
        byte campoSeq = (byte) (seq & 0x0f) ;//asegura q solo sean 4 bits

        byte[] cabecera = new byte[2];
        cabecera[0] = tipo_DATA;
        cabecera[1] = campoSeq ;

        //añadimos el CRC al payload
        byte[] payload  = new byte[1+1 +datos.length];
        System.arraycopy(cabecera,0,payload,0,cabecera.length);
        System.arraycopy(datos,0,payload,cabecera.length,datos.length);
        int CRC = CRC8.calcularCRC(payload);

        //System.out.println("--------------------------");

        //System.out.println(CRC);

        //System.out.println("---------------------");

        byte[] conCRC = new byte[payload.length +1];
        System.arraycopy(payload,0,conCRC,0,payload.length);
        conCRC[conCRC.length-1] = (byte) (CRC & 0xff); // se adjunta el crc calculado al payload

        byte[] rellenoYFlag = rellenoBytes(conCRC);
        return rellenoYFlag;
        //
    }

    public static byte[] ctorTramaAck (int seq){
        byte[] tramaSinDelimitar = new byte[2];
        tramaSinDelimitar[0] = tipo_ACK ;
        tramaSinDelimitar[1] = (byte)(seq & 0xff);
        byte[] tramaLimitada  = delimitadorTrama(tramaSinDelimitar);
        return tramaLimitada ;
        //NO ES NECESARIO CALCULAR EL CRC DE LA TRAMAS DE CONTROL COMO TYPE_ACK Y TYPE_NAK. PERO SI ES NECESARIO
        //CALCULAR EL RELLENO DE BITS
        //int CRC = CRC8.calcularCRC(tramaSinCrc);
        //tramaSinCrc[2] = (byte)(CRC & 0xff);
        //byte[] rellenoYFlag= rellenoBytes(tramaSinCrc);

    }

    //trama NAK de solicitud de recepción negativa. LA TRAMA NAK no contiene payload
    public static byte[] ctorTramaNak(int seq){
        byte[] tramaSinLimitar = new byte[2];
        tramaSinLimitar[0] = tipo_NAK;
        tramaSinLimitar[1] = (byte) (seq & 0xff);
        byte[] tramaLimitada = delimitadorTrama(tramaSinLimitar);

        return tramaLimitada;


    }

    public static byte[] delimitadorTrama(byte[] tramaSinLimitar){
        ArrayList<Byte> salida = new ArrayList<>();
        salida.add(Flag);
        for(byte b: tramaSinLimitar){
            salida.add(b);
        }
        salida.add(Flag);
        byte[] arreglloBytes = new byte[salida.size()];

        for(int j = 0 ; j < salida.size(); j++)
            arreglloBytes[j] = salida.get(j);
        return arreglloBytes;
    }



    //devuelve un objeto con información de la trama
    // recepeción verificar banderas, deshacer relleno de bytes , verificar banderas , validar CRC
    //extrae llos componentes de la trama
    public static class ParseFrame{
        public final boolean ok; // indicar si la trama esta correcta o no
        public final String error; //tipo de error
        public final byte tipo; //NAK /ACK/DATA
        public final int seq;
        public final byte[] payload; // para datos, es null para ACK/NAK

        public ParseFrame(boolean ok, String error, byte tipo, int seq, byte[] payload){
            this.ok = ok;
            this.error = error;
            this.tipo = tipo;
            this.seq = seq;
            this.payload = payload;

        }
    @Override
        public String toString(){
            if(this.tipo == tipo_DATA){
                String datos = "";
                for(byte b : payload){
                    datos += b+" ";
                }
                return "trama recibida --> |"+this.tipo+"|" +this.seq+"|"+datos ;

            }
            return "trama recibida --> |"+this.tipo+"|" +this.seq+"|"+payload ;
    }

    }

    public static ParseFrame parseFrame(byte[] trama){
        // verificamos q la trama no este vacia ni incompleta. la trama mas corta es de 4 bytes(NAK o ACK)
        if(trama ==null || trama.length < 4)
            return new ParseFrame(false,"trama demasiado corta",trama[1],-1,null);
        if(trama[0]!= Flag || trama[trama.length-1]!=Flag)
            return new ParseFrame(false, "la trama no tiene banderas",trama[1],-1,null);

        //si la trama no pasa por ningun validación anterior,se realiza la desempaquetación por parte del receptor
        // al campo FLAG no se le aplica el relleno de byte
        // solo desestructuramos tramas de datos, ya que son de estas q tenemos que exter el payload

        if(trama[1]==tipo_DATA) {
            ArrayList<Byte> desemp = new ArrayList<>(); // el host receptor extrae el payload de la trama para pasarlo a la cr
            boolean escNext = false;
            //escNext se inicializa en false para evitar que desrellene el campo tipo de la trama
            // quitamos los rellenos de bytes en caso de que haya simbolos especiales en el payload escapados. tambien quitamos las banderas
            for (int j = 1; j < trama.length - 1; j++) {
                byte b = trama[j];
                if (escNext) {
                    desemp.add((byte) (b ^ 0x20));
                    escNext = false;
                } else if (b == Esc) {
                    escNext = true;
                } else {
                    desemp.add(b);
                }
            }
            //convertimos a arreglo
            byte[] data = new byte[desemp.size()];
            for (int i = 0; i < desemp.size(); i++)
                data[i] = desemp.get(i);
            int crcRecibido = data[data.length - 1] & 0xff;
            byte[] sinCRC = Arrays.copyOf(data, data.length - 1);
            int crcCalculado = CRC8.calcularCRC(sinCRC); //calcuamos el crc de la trama recibida
            if (crcCalculado != crcRecibido) {
                byte tipoTrama = data.length > 0 ? data[0] : 0;
                int secuc = data.length > 1 ? data[1] & 0xff : -1;
                return new ParseFrame(false, "CRC no coincide", tipoTrama, secuc, null);
            }
            int sec = data[1] & 0xff;

                byte[] payload  = Arrays.copyOfRange(data,2,data.length-1); //extraemos los datos
                return new ParseFrame(true,null,data[0],sec,payload);
            }
        else{

            return new ParseFrame(true,null,trama[1],trama[2],null); // trama ack/nak que no tienen campo de datos
        }
    }

}


