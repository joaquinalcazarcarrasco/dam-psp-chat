package com.pspchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author joaquinalcazarcarrasco
 * 
 * Clase SalaDeChat destinada a tener rol de servidor del chat. Establecerá puerto por el que podrán enviar peticiones los diferentes clientes.
 * Además, creará hilo de ejecución por cada cliente que se conecte para recibir datos y reenviarlo al resto de clientes. 
 *
 */
public class SalaDeChat {
	
	/**
	 * Arraylist para almacenar las personas usuarias conectadas
	 */
	protected static ArrayList<String> _conectados = new ArrayList<String>();
	
	/**
	 * Método principal
	 * 
	 * @param args Se recibe como parámetro el puerto que se desea abrir para la conexión cliente-servidor
	 */
	public static void main(String[] args) {
		
		//variables
		int port;
		ServerSocket serverSocket;
		List<PrintWriter> writers = new ArrayList<PrintWriter>();
		Socket socket;
		BufferedReader reader;
		String textoNuevaConexion;
		
		//args = new String[]{"4567"};
		
		if(args.length<1) {
			System.err.println("Falta el número de puerto.");
			return;
		}
		try {
			
			port = Integer.parseInt(args[0]);
		
		}catch(NumberFormatException e){
			
			System.err.println("Puerto inválido");
			return;
		}
		
		
		try {
		
			serverSocket = new ServerSocket(port);
		
		}catch(IOException e){
		
			System.out.println("No se pudo escuchar en el puerto " + port);
			return;
			
		}
		
		while(true) {
			
			try {
				
				socket = serverSocket.accept();
			
			}catch(IOException e) {
				
				System.err.println("Error esperando clientes: "
						+ e.getLocalizedMessage());
				return;
			}
			
			PrintWriter out;
			
			try {
				
				out = new PrintWriter(socket.getOutputStream());
			
			} catch (IOException e) {
				
				System.err.println("No se pudo conseguir el canal de escritura de socket");
				continue;
			}
			
			writers.add(out);
			
			//Nueva instancia de RecibeYEscribe
			RecibeYReenvia ryr;
			
			try {
				
				//para leer si hay nuevas conexiones
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				textoNuevaConexion = reader.readLine();
								
				//Divido la cadena recibida
				String []fragmentos = textoNuevaConexion.split(" >> ");
				
				//Recupero el nombre de la persona usuaria
				String nick = fragmentos[0];
				
				//se comprueba que no haya un nick ya así
				for(String conectado : _conectados) {
					
					if(nick.equals(conectado)) {
						
						nick = nick + "2";
						out.println("##cambioNick##" + nick);
						out.flush();
						
						textoNuevaConexion = nick + " >> " + fragmentos[1];
					}
					
				}
				
				//Lo almaceno en el arraylist de personas usuarias conectadas
				_conectados.add(nick);
				
				/*for(String conectado : _conectados) {
					
					System.err.println("Conectado: " + conectado);
				
				}*/
				
				//Inicalizamos rye recibiendo el stream de entrada del socket al que nos conectamos
				ryr = new RecibeYReenvia(socket.getInputStream(), out, writers, textoNuevaConexion);
			
			} catch (IOException e) {
				
				//Si salta excepción de entrada/salida mostramos texto de error en la ventana de texto
				System.err.println("-- Error de conexión --");
				return;
			}
			
					
			//Creamos nuevo hilo asociando la instancia rye y la iniciamos
			new Thread(ryr).start();
			
			//Enviamos mensaje al servidor de que la consexión ha sido exitosa
			System.out.println("CONEXIÓN ESTABLECIDA");
			
		}//while (true)

	}//main

}

/**
 * @author joaquinalcazarcarrasco
 * 
 * Clase RecibeYReenvia que implementa Runnable. Clase pensada para albergar la ejecución del hilo creado por la clase SalaDeChat. Se encarga
 * de mantenerse a la escucha de peticiones de clientes y de reenviar la información al resto de clientes.
 *
 */
class RecibeYReenvia implements Runnable {

	//Atributos
	private BufferedReader _reader;
	private PrintWriter _pw;
	private List<PrintWriter> _writers;
	private String _textoNuevaConexion;
	
	//Constructor
	/**
	 * Constructor de la clase
	 * 
	 * @param is Objeto de la clase InputStream. Canal de entrada de datos del cliente que ha realizado petición de conexión
	 * @param pw Objeto de la clase PrintWriter. Canal de salida de datos con el cliente con el que se ha realzado conexión
	 * @param writers Objeto de la clase List que almacena objetos de tipo PrintWriter. Contiene todos los canales de salida de datos hacia los clientes con los que se tiene conexión, para reenviar los datos recibidos por un cliente
	 * @param textoNuevaConexion String. Texto que se mostrará en el resto de clientes cuando se conecte un nuevo cliente
	 */
	public RecibeYReenvia(InputStream is, PrintWriter pw, List<PrintWriter> writers, String textoNuevaConexion) {
		
		//Creamos nuevo buffer de lectura asociado al stream de entrada pasado como argumento
		_reader = new BufferedReader(new InputStreamReader(is));
		//Almacenamos la ventana de chat en nuestra instancia de Chat
		_pw = pw;
		_writers = writers;
		_textoNuevaConexion = textoNuevaConexion;
	}
	
	/**
	 *Método run que representa la funcionalidad que tiene la clase cuando se carga como hijo de ejecución.
	 *
	 */
	@Override
	public void run() {
		
		//Variable para acumular texto recibido desde el servidor
		String leido;
		
		
		//recorro la lista de clientes para mostrar primera conexión
		for(PrintWriter cliente : _writers) {
			
			//Compruebo que el cliente no es quien envió el mensaje
			if(cliente != _pw) {
				
				//envío por canal de salida el texto recibido
				cliente.println(_textoNuevaConexion);
				
				if(cliente.checkError()) System.out.println("\t-- Error en el último envío --");
			}
			
		}
		
		//Bucle que va leyendo del stream de entrada
		while(true) {
			
			try {
				
				//almacenamos en leido la línea recibida en el buffer de entrada
				leido = _reader.readLine();
			
			} catch (IOException e) {
				
				//Si salta excepción salimos del bucle
				break;
			}
			
			//si no llega texto, salimos del bucle
			if(leido==null) break;
			
			//Si se ha establecido desconexión
			if(leido.contains("DESCONECTADO")) {
				
				//System.out.println("Entro en desconectado");
				
				//Divido la cadena recibida
				String []fragmentos = leido.split(" >> ");
				
				//Recupero el nombre de la persona usuaria
				String nick = fragmentos[0];
				
				//Lo almaceno en el arraylist de personas usuarias conectadas
				SalaDeChat._conectados.remove(nick);
				
				/*for(String conectado : SalaDeChat._conectados) {
					
					System.err.println("Aún Conectado: " + conectado);
				
				}*/
				
			}
			
			//si se pide lista de conectados/as
			if(leido.equals("dime#")) {
				
				_pw.println("##inicio##");
				_pw.flush();
				
				//itero el arraylist de personas conectadas
				for(String conectado : SalaDeChat._conectados) {
					
					//envío cada conexión al cliente que lo pidió
					_pw.println("##user##" + conectado);
					_pw.flush();
					if(_pw.checkError()) System.out.println("\t-- Error en el último envío dime --");
					
				}
				
			}else {
				
				//recorro la lista de clientes
				for(PrintWriter cliente : _writers) {
					
					//Compruebo que el cliente no es quien envió el mensaje
					if(cliente != _pw) {
						
						//envío por canal de salida el texto recibido
						cliente.println(leido);
						
						if(cliente.checkError()) System.out.println("\t-- Error en el último envío --");
					}
					
				}
				
			}

			//Lo escribo en la salida del servidor
			System.out.println(leido);
			
			
		}//while
		
		try {
			
			//Cerramos el canal de entrada
			_reader.close();
		
		} catch (IOException e) {
		
		}
		
	}//run

}
