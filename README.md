
# Protocolo de capa de enlace
Se diseño un protocolo de la CED que toma el medio de tranmisión en bruto y lo transforma en una linea libre que pareza libre de errores a la capa de red. Esto se lleva a cabo dividiendo los datos de entrada en marcos de datos, recibidos de forma secuencial, y del procesamiento de los acuse/confirmaciones devueltas por el receptor. Todo envio , recepción y control de errores ocurrirá para simplificar el programa en forma secuencial
el programa fue desarrollado en java , en la  version 17.0.8. usando el entorno intellegi IDEA.
### Integrandes y docente
- Docente a cargo de TPs:  Mag. Ing. Paula Martinez
- Comisión: 4k09
- Mauro,Renzo
- Fernandez, Nicolas
- Fernandez  Julian
- Quitanta, Adolfo
- Zubelza, Cristian
- ciclo lectivo: 2025

## Características implementadas del protocolo

- servicio orientado a la conexión: establecemos una conexión previa entre 2 host antes de comenzar a transferir datos entre ambos
- Enmarcado: se encapsulan los datos en una trama , limitada por banderas con adición de un CRC-8 bits , mas otros campos de interes como el tipo_trama y el nro de secuencia de la misma.
- Control de errores:  verificamos si la trama sufrió alguna corrupción en los datos al recalcular el CRC en el recpetor. Ademas recibimos retroalimentación del receptor con tramas de control ACK/NAK para determinar si se recibió correctamente la trama o en caso de que no y se necesite retransmitir. Evita tramas verificando los nros de cuencia y tramas perdidas con el envio de la señal NAK
- Control de flujo: implementamos una ventana deslizante de tamaño parametrizado en el emisor, en nuestro caso de tamaño 4, la cual permite tener hasta 4 tramas en "vuelo" sin recibir ACK, asi evitamos saturar al receptor. también implementamos un máximo de retransmisiones , de hasta 3 envios para evitar un bucle infinito en caso de que un trama envidada nunca reciba ACK. cuando se alcanzan las 3 transmisiones de una misma trama y esta no recibe una aACK , se supone q se recibió correctamente y se avanza el puntero base de la ventana para continuar con el sgte nro de secuencia de trmaa a transmitir.
- simulación del medio fisico con probabiildad de error parametrizado, con llamado de metodos(no usamos sockets) : implementamos un medio fisico simulado con una cierta probabillidad de error, simulando asi un medio real en el cual los bits de la trama pueden corromperse. En nuestro caso, si elegimos una determinada probabilidad de error, el algoritmo corromperá con mayor probabilidad un campo aleatorio de la trama.
- relleno de bytes: la cual es una tecnica para evitar que ciertos simbolos especiales, en nuestro caso ESC y FLAG , en los datos estropescan la correcta interpretación de la trama en el recpetor.

## Estructura de la trama
|FLAG| TIPO_TRAMA| NRO_SEQ| DATOS(PAYLOAD)+ RELLENO DE BYTES | CRC|FLAG | 

  -FLAG --> 0X7E : delimiador de inicio y fin de la trama
  
  -TIPO_TRAMA --> 1/2/3 : 1-->Datos  | 2--> ACK | 3-->NAK
  
  -DATOS --> datos de la capa de red, maximo : 1472 bytes
  
  _RELLENO DE BYTES --> si en los datos se encuentran los bytes especiales FLAG/ESC se escapan con ESC 
  
  _CRC--> de 1 bytes para la detección de errores.

## instalación  y uso 
1. Clona el repositorio:
   ```bash
   https://github.com/jul14n2000/capa_enlace.git
2. desde algun IDE compilar y ejecutar la clase capaEnlaceMeaplicacion, con algunas acciones ya hardcodeadas
3. o desde la linea de comandos ir a la carpeta en donde está el archivo clonado --> compile el archivo con el comando javac *. y si la compilacion fue exitosa luego ejecute el comando java capaEnlaceApliacion
## Pruebas de envio y recepciones
1. crear una instancia de capa fisica simulada con una probabibilidad de error del canal

   -EnlaceFisico canal = new EnlaceFisico(probabilidad_error)
3. crear por lo menos 2 objetos de CapaEnlace 

   -CapaEnlace A = new CapaEnlace();

   -CapaEnlace B = new CapaEnlace();
5. simular la conexión entre los host a traves del metodo conectar(CapaEnlace a, CapaEnlace B,) proporcionado por el medio

   -canal.conectar(A, B);
7. usar el metodo envioDatos("string") proveido para los objetos del tipo CapaEnlace

   -A.envioDatos("hola B");
9. verificar en la terminal el envio, recepción de ack/nak , desplazamiento de la venetana deslizante de la ejecución.
10. Probarlo con difernetes probabilidad de error del medio fisico para poder ver todas los casos posibles con sus determinas respuestas y msj de error. 

## algunas de lsa implementaciones fundamentales
1. CapaEnlace
- en la clase fundamental de todo el protocolo, desde la cual comienza el proceso de construcción y transmisión de la trama.  se definen las variables que controlan el tamaño de la ventana, la cantidad maxima de reenvios, el nro de secuencia actual disponible. definimos 3 estructuras de datos: bufferEnvio que contiene las tramas ya constuidas y listas para ser transmitidas, el ctadorTranmisiones que almacena el nro de reenvios por trama. ambas del tipo HashMap, la primera usa como clave-valor el nro de secuencia y la trama , mientras q  la 2da usa el nro de secuncia como clave y el nro de reenvio como valor.Y almacenamos los ack recibos en un HashSet para evitar almacenar nro de secuencia duplicados. Y una variable booleana HostA, la cual nos va a permitir saber que host es el emisor y cual es el receptor.
- metodo sendEnvioDatos --> recibirá los datos a enviar en formato String, los convertirá a un arreglo de bytes y llamará al metodo "CtorTrama.ctorTramaDatos(seq, payload)" pasandole el nro de secuencia actual disponible y el dato convertido en arreglo de bytes. Este retornará la trama complementamente construida y lista para transmitir. Y finalmente llama al metodo VentanaDeslizante
- ventanaDeslizante --> primero verifica que existan tramas sin ack en el bufferEnvio, si las hay ,verifica que esté en la ventana y que no haya recibido ACK si es asi la retransmite , aumentado el contador de retransmisiones ,hasta el maximo de 3, donde se marca como ack.Luego se llama al metodo transmitir() de la CapaEnLace, al cual se le pasan 2 argumentos: la trama del buffer y la bandera HostA
- metodo public byte[] transmitir(byte[] trama, boolean desdeA) --> este metodo simula la tranmsisión por el medio fisico, corrompe los bits de 1 campo al azar de la trama, en función de la probabilidad asignada previamente cuando se definio el canal fisico. Este metodo verifica quien de los 2 host actua como emisor o receptor mirando la bandera HostA, y deetermina asi el receptor de la trama enviada, el cual llama la metodo reciboTrama() el cual recibe como argumento la trama enviada por el emisor
- public byte[] reciboTrama(byte[] trama) --> aqui se crea un objeto del tipo ParseFrame el cual contiene info de la trama (si está ok, error, payload, el nro de secuencia, el tipo de trama). se verifica que la trama no esté corrupta y el tipo de trama. si es de datos se extree el payload de la misma y construye y retorna una trama ack para confirmar al emisor el correcto recibo. esa secuencia de llamadas desde el codigo de ventanaDeslizante retorna una trama , la cual es procesada con el metodod procesarRespuesta
- private void procesarRespuesta(byte[] respuesta,int seq) --> recibe la trama que respondió el receptor y el nro de sec. crea un objeto ParseFrame con la trama, verifica q no este corrupta y el tipo de la misma. si es una trama ACK valida, agrega el nro de sec de la misma en la coleccipon  tramasACK , para despues eliminarla de el buffer y avanzar la ventana. toda esta secuencia vuelta a iterar por cada trama almaceenada en el buffer de envio. finalmente se llama al metodo avanzarPunteroVentana
- private void avanzarPunteroVentana() --> el cual primero verifica que en el bufferEnvio haya una trama con nro de secuencia igual al punterO DE VENTANA y q tenga ack. si lo encuentra elimina la trama del buffer , del contador de transmisiones y incrementa el puntero de la ventana en 1.




