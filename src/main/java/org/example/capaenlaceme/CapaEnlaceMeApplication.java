package org.example.capaenlaceme;
import org.example.capaenlaceme.CtorTrama;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.example.capaenlaceme.CtorTrama.*;

@SpringBootApplication
public class CapaEnlaceMeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CapaEnlaceMeApplication.class, args);

		/*
		byte[] datos = {23, 12, 3};

		System.out.println("¡Hola, mundo!");
		byte[] resultado = ctorTramaDatos(10, datos);

		System.out.println(resultado[0]);
		System.out.println(resultado[1]);
		System.out.println(resultado[2]);

		System.out.println(resultado[3]);
		System.out.println(resultado[4]);
		System.out.println(resultado[5]);
		System.out.println(resultado[6]);
		System.out.println(resultado[7]);


		System.out.println("trama de ack");

		byte[] tramaACK = ctorTramaAck(2);
		System.out.println("bandera superior " + tramaACK[0]);
		System.out.println("tipo de trama " + tramaACK[1]);
		System.out.println("numero de secuencia " + tramaACK[2]);
		System.out.println("bandera inferiro " + tramaACK[3]);

		System.out.println("-------------trama NAK----------------");
		byte[] tramaNAK = ctorTramaNak(1);
		System.out.println("bandera superior " + tramaNAK[0]);
		System.out.println("tipo de trama " + tramaNAK[1]);
		System.out.println("numero de secuencia " + tramaNAK[2]);
		System.out.println("bandera inferiro " + tramaNAK[3]);

		System.out.println("-------------recepción-------------");

		ParseFrame tramaRecibida = parseFrame(resultado);

		System.out.println(tramaRecibida.toString()); */
		System.out.println("------------------PRUEBA-----------------------------");
		double probCorrupciónDDatps = 0.00;
		EnlaceFisico canal = new EnlaceFisico(probCorrupciónDDatps);
		CapaEnlace A = new CapaEnlace();
		CapaEnlace B = new CapaEnlace();

		// establecemos el medio fisico simulado entre 2 host que se usara para transmitir las tramas
		canal.conectar(A, B);
		//el metodo envio de datos construye el formato de la trama, le asigna un nro de secuencia y lo coloca en el buffer de envio
		// no imploca que   que se hayan transmitido aun por el medio
		A.envioDatos("hola B");
		//A.envioDatos("como estas B");
		// para transmitir la trama por el medio usamos el metodo transmitir del medio o canal propio de la xonexión establecida
		//canal.transmitir(A.getBufferEnvio(0),A.hostA); //Envia la trama en el buffer con nro de secuencia 0 "hola B"
		//canal.transmitir(A.getBufferEnvio(1),A.hostA);

		B.envioDatos("hola A ");
		//B.envioDatos("bien");




	}



}
