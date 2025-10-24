package org.example.capaenlaceme;

import java.util.*;

//coordina la entrega y recepción de las tramas
public class CapaEnlace {
    private EnlaceFisico canal ;
    public boolean hostA;
    private int inicioVentana = 0;
    private int nroSeqActual = 0;
    private int tamanioVentana = 4 ;
    private int maxReintentos =3 ;

    private final LinkedHashMap<Integer,byte[]> bufferEnvio = new LinkedHashMap<>();
    private final Map<Integer,Integer> ctadorRetransm  = new HashMap<>();
    private final Set<Integer> tramasACK  = new HashSet<>();

    public void establecerMedio(EnlaceFisico canal, boolean hostA){
        this.canal = canal;
        this.hostA = hostA;
    }
    // datos a enviar a la cola. este sera el datagrama recibido desde la capa de red
    public void envioDatos(String datos){
        int seq = nroSeqActual & 0xff;
        byte[] payload = datos.getBytes();
        byte[] trama = CtorTrama.ctorTramaDatos(seq, payload);
        bufferEnvio.put(seq, trama); //se coloca en el buffer de envio
        ctadorRetransm.put(seq, 0); // inicializamos el contador de retransmisiones
        nroSeqActual = (nroSeqActual + 1) % 256; // incrementamos el nro de secuencia actual para la sgte trama a enviar
        System.out.println((hostA?"[A]":"[B]") + "Nro de secuencia: " + seq + " almacenada en el buffer con payload='" + datos + "'");
        ventanaDeslizante();
    }

    private void ventanaDeslizante(){
        //mientras existan tramas no reconocidas en el buffer
        while(tramasNoReconocidas()){
            //se envian las tramas dentro del bufferEnvio q aun no se han enviado o necesitan retransmitirse pq no han sido confirmadas
            for(int seq:new ArrayList<>(bufferEnvio.keySet())){
                //System.out.println(" ventana: punteroActual=" + nroSeqActual + " seq=" + seq + " diff=" + ((seq - nroSeqActual + 256) % 256) + " < ventana? " + estaEnVentana(seq));
                if(!estaEnVentana(seq)) continue; // si no está en ventana la trama, salimos de la iteración actual a la sgte para otra otrama
                if(tramasACK.contains(seq)) continue;
                int nroTranmision = ctadorRetransm.getOrDefault(seq, 0);
                if(nroTranmision > maxReintentos){
                    System.out.println((hostA?"[A]":"[B]") +"seq"+ seq + "se alcanzp el nro maximo de reintentos- se marca como ack para avanzar ventana");
                    tramasACK.add(seq);
                    continue;
                }
                //enviar/retransmitir
                ctadorRetransm.put(seq, nroTranmision+1);
                System.out.println("*********transmisión por el canal fisico*******");
                System.out.println((hostA?"[A]":"[B]") + " enviando la trama con n° de seq= " + seq + " ,cantidad de veces enviadas= " + (nroTranmision+1));
                byte[] respuesta = canal.transmitir(bufferEnvio.get(seq),hostA); //capturamos la respuesta del receptor(ack/nak/null)
                procesarRespuesta(respuesta,seq);
            }
            avanzarPunteroVentana();
            if(ventanaLimpia()) break;
        }
        limpiarBufferEnvio();
    }
    private boolean tramasNoReconocidas(){
        for(int k:bufferEnvio.keySet()){
            if(!tramasACK.contains(k))
                return true;

        }
        return false;
    }
    private boolean ventanaLimpia(){
        int cantTramasSinConf =0 ;
        for(int k:bufferEnvio.keySet()){
            if(!tramasACK.contains(k) && estaEnVentana(k))
                cantTramasSinConf++;
        }
        return cantTramasSinConf ==0;
    }

    private   boolean estaEnVentana(int seq){
        int diff = (seq-inicioVentana +256) % 256;
        return diff >=0 && diff< tamanioVentana;
    }
    //nota: el nro de seq tambien podria sacarlo de parametro respuesta, pero lo hacemos asi en caso de que se devuelva una trama nula
    private void procesarRespuesta(byte[] respuesta,int seq){
        if(respuesta == null) {
            System.out.println(hostA ? "[A]" : "[B]" + "no responde para la secuencia " + seq);
            return;
        }
        CtorTrama.ParseFrame p = CtorTrama.parseFrame(respuesta);
        if(!p.ok){
            System.out.println((hostA?"[A]":"[B]")+ "respuesta del error "+p.error);
            return;
        }
        if(p.tipo ==CtorTrama.tipo_ACK && p.seq == seq){
            System.out.println((hostA?"[A]":"[B]") + "ACK recibido para la trama con nro se secuencia"+p.seq);
            tramasACK.add(seq);
            // comparamos los p.seq con seq pq  el p.seq se corresponde con el nro de secuencia de la trama NAK en cambio el seq al nro de la trama originak
            // hacemos esto pq pueden no coincidir debido a una error en el campo de nro de secuencia
        }else if (p.tipo == CtorTrama.tipo_NAK && p.seq == seq){
            System.out.println((hostA?"[A]":"[B]") + "NAK recibido para la secuencia " + seq);

        }else
            System.out.println((hostA?"[A]":"[B]")+" tipo de respuesta inesperada="+p.tipo+" seq="+p.seq);
    }

    private void avanzarPunteroVentana(){
        //verificamos que  la trama en la ventana está en el buffer y se ha recibido ack de la misma
        while(bufferEnvio.containsKey(inicioVentana) && tramasACK.contains(inicioVentana)){
            bufferEnvio.remove(inicioVentana); // eliminamos la trama del buufer
            ctadorRetransm.remove(inicioVentana, 0); //eliminamos el contador de retransimisiones
            System.out.println((hostA?"[A]":"[B]") + " avanza la ventana, nro de secuencia " + inicioVentana+" eliminada");
            inicioVentana = (inicioVentana+1)% 256; // desplazamos el puntero de la ventana

        }
    }
    // liberamos el buffer de tramas q ya han sido confirmadas. no las borramos directamente pq ocurre una excepción al tratar de modificar
    // la colección map mientras se itera. entonces por eso creamos un arraglo con los nros de secuencias a eliminar del buffer
    private void limpiarBufferEnvio(){
        List<Integer> eliminar = new ArrayList<>();
        for(int k:bufferEnvio.keySet())
        {
            if(tramasACK.contains(k))
                eliminar.add(k);
        }
        for(int r:eliminar){
            bufferEnvio.remove(r);
            ctadorRetransm.remove(r);
        }
    }
    public byte[] reciboTrama(byte[] trama){
        CtorTrama.ParseFrame p = CtorTrama.parseFrame(trama);
        if(!p.ok){
            System.out.println((hostA?"[A]":"[B]")+ "La trama recibida está corrupta. Error: "+p.error+", enviar Nak");
            int seq = (p.seq>=0?p.seq:0);
            return CtorTrama.ctorTramaNak(seq);
        }
        if(p.tipo ==CtorTrama.tipo_DATA){
            String datos = new String(p.payload);
            System.out.println((hostA?"[A]":"[B]") + " trama recibida con n° de secuencia=" + p.seq + " payload='" + datos + "'");
            return CtorTrama.ctorTramaAck(p.seq);

        }else {
            System.out.println((hostA?"[A]":"[B]") + " otro tipo de trama de control =" + p.tipo + " seq=" + p.seq);
            return null;
        }
    }

    public void estadoActualBufferEnvio (){
        System.out.println("Estado actual del bufferEnvio del host");
        for(int seq :bufferEnvio.keySet()){
            byte[] datos = bufferEnvio.get(seq);
            String texto = new String(datos);
            System.out.println(texto);
        }
    }

    public byte[] getBufferEnvio(int seq){
        return  bufferEnvio.get(seq);
    }

}
