package org.example.capaenlaceme;

import java.util.Random;

public class EnlaceFisico {
    private CapaEnlace hostA;
    private CapaEnlace hostB;
    private final double probErrorEnMedio ;

    public EnlaceFisico(double errorEnMedio) {
        this.probErrorEnMedio = errorEnMedio;
    }

    //simular la conexión entre los host
    public void conectar (CapaEnlace a , CapaEnlace b) {
        hostA = a;
        hostB = b;
        a.establecerMedio(this,true);
        b.establecerMedio(this,false);

    }
    //simulación de transmisión a traves del medio simulado

    public byte[] transmitir(byte[] trama, boolean desdeA){
        final Random probabError  = new Random();
        byte [] tramaEnviar = trama.clone();
        //corrompemos cualquier campo de la trama evitando los flags. para simular errores en el canal fisico
        // solo se corrompe si la trama es aceptable y el errorProbabilidad generado es menor al configurable
        if(tramaEnviar.length>4 && probabError.nextDouble()<probErrorEnMedio){
            //evitamos cambiar las FLAGS. generamos un entero q indicara el campo de la trama a cambiar
            int pos = 1 + probabError.nextInt(tramaEnviar.length-2);
            tramaEnviar[pos] ^= (byte) (probabError.nextInt(0xff));
            System.out.println("byte corrompido en la posición "+pos +"de la trama");
        }
        if(desdeA){
            if(hostB!=null)
                return hostB.reciboTrama(tramaEnviar);
        }else
            return hostA.reciboTrama(tramaEnviar);
        return null;
    }
}
