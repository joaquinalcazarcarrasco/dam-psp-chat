package com.pspchat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * @author joaquinalcazarcarrasco
 * 
 * Clase Chat que hereda de JFrame. Clase destinada a representar la interfaz gráfica hacia el usuario cliente, así como destinada a establecer
 * conexión con el servidor para enviar y recibir datos, así como generar hilo que reciba datos del servidor y los pinta en la interfaz.
 *
 */
public class Chat extends JFrame {

	//Atributos
	private JTextArea _taChat;//Área de texto que muestra el chat
	private JTextField _tfEntradaUsuario;//Campo de texto donde escribe el/la usuario/a
	protected static String _nombreUsuario;//Nombre de usuario en sala de chat
	private PrintWriter _canalSalida;//stream de salida que almacenará el socket conectado al servidor
	private JButton _botonConectados;
	protected static JTextArea _areaConectados;
	private JDialog _dialogConexion;
	
	//Constructor
	/**
	 * Constructor de la clase Chat que recibe como parámetros cada uno de los atributos de la clase
	 * 
	 * @param socket Objeto de la clase Socket. Conexión que se ha establecido con el servidor
	 * @param nombreUsuario String nombre del usuario/a que está usando la instancia de Chat
	 */
	public Chat(Socket socket, String nombreUsuario) {
		
		/*
		 * llamamos al constructor de la clase padre (JFrame) pasándole como argumento
		 * el nombre de usuario y concatenamos el título para la ventana
		 */
		super(nombreUsuario + " - Ventana de Chat");
		
		//Almacenamos el nombre de usuario
		_nombreUsuario = nombreUsuario;

		//Iniciamos la ventana de chat
		inicializarVentana();
		
		try {
			
			//Capturamos el stream de salida del socket en nuestra variable destinada a ello
			_canalSalida = new PrintWriter(socket.getOutputStream());
		
		} catch (IOException e) {
			
			//Si salta excepción de entrada/salida mostramos texto de error en la ventana de texto
			agregaTexto("-- Error de conexión --");
			return;
		}
		
		//Nueva instancia de RecibeYEscribe
		RecibeYEscribe rye;
		
		try {
			
			//Inicalizamos rye recibiendo el stream de entrada del socket al que nos conectamos
			rye = new RecibeYEscribe(socket.getInputStream(), this);
		
		} catch (IOException e) {
			
			//Si salta excepción de entrada/salida mostramos texto de error en la ventana de texto
			agregaTexto("-- Error de conexión --");
			return;
		}
		
		//Creamos nuevo hilo asociando la instancia rye y la iniciamos
		new Thread(rye).start();
		
		//Enviamos mensaje al servidor de que la consexión ha sido exitosa
		enviarServidor("CONEXIÓN ESTABLECIDA");
		
	}// Fin constructor

	/**
	 * 
	 * Método para iniciar la ventana de chat creando y configurando cada uno de sus controles, así como manejadores de eventos
	 * 
	 */
	protected void inicializarVentana() {
		
		//Establecemos el tamaño de la ventana
		setSize(400,420);
		
		//Inicializamos nueva área de texto
		_taChat = new JTextArea();
		_taChat.setEditable(false);//Configuramos para que no sea editable
		_taChat.setLineWrap(true);//Configuramos para que haya ajuste de línea
		_taChat.setWrapStyleWord(true);//Configuramos para que el ajuste de línea sea por palabra y no por caracteres
		_taChat.setMargin(new Insets(10,10,10,10));//Establecemos un margin para dejar algo de sangría
		
		//Añadimos el área de texto a una instancia de JScrollPane para que se pueda hacer scroll
		JScrollPane scrollPane = new JScrollPane(_taChat);
		
		//inicializamos nuevo campo de texto
		_tfEntradaUsuario = new JTextField();
		
		//inicializamos botón
		_botonConectados = new JButton("Conectados/as");
			
		//Configuramos nueva capa de ubicación
		setLayout(null);
		add(scrollPane);//Ubicamos el área de texto deslizable
		scrollPane.setBounds(0,0,400,330);
		add(_tfEntradaUsuario);//Ubicamos el campo de texto
		_tfEntradaUsuario.setBounds(0, 330, 400, 25);
		add(_botonConectados);
		_botonConectados.setBounds(0, 355, 400, 30);
		
		
		//Creamos manejador de evento para el campo de texto para cuando se pulse intro
		_tfEntradaUsuario.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//Invocamos al método onTextoEscrito()
				onTextoEscrito();
			}
		});
		
		//Creamos manejador de eventos para el botón de personas conectadas
		_botonConectados.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				personasConectadas("dime#");
				
			}
		});
		
		//Hacemos visible la ventana de chat
		setVisible(true);
		//Ponemos el foco en el campo de texto para facilitar el uso a la persona usuaria
		_tfEntradaUsuario.requestFocusInWindow();
		
		//Creamos manejador de evantos para la ventana para controlar si se cierra
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				
				//Enviamos mensaje de desconexión al servidor
				enviarServidor(" -- DESCONECTADO --");
				//Cerramos el stream de salida conectado al servidor
				_canalSalida.close();
				
			}
		});
		
		//Ajustamos la acción por defecto cuando se cierra ventana para que se cierra la aplicación
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}// fin método inicializar ventana
	
	/**
	 * Método para añadir el texto escrito al área de texto
	 * 
	 * @param texto String. Texto para agregar al área de texto _taChat
	 */
	protected void agregaTexto(String texto) {
		
		//Añadimos el texto pasado como argumento al área de texto con el método append()
		_taChat.append(texto);
		
		//Ajusta la posición del texto insertado al final del área
		_taChat.setCaretPosition(_taChat.getDocument().getLength());
		
	}//fin método agrega texto
	
	/**
	 * Método para enviar texto a través del canal de salida del servidor al que nos hemos conectado
	 * 
	 * @param texto String. Texto destinado a ser enviado al servidor a través del canal de salida establecido mediante la conexión con el socket
	 */
	protected void enviarServidor(String texto) {
		
		//if(texto.equals("dime#")) {}
		//else {
			//se envía por la salida de nuestra instancia de printWriter
			_canalSalida.println(_nombreUsuario + " >> " + texto);
			_canalSalida.flush();//se hace flush() para forzar envío
		//}
		
		
		
		
	}//Fin enviarServidor
	
	/**
	 * Método para enviar petición de lectura de personas conectadas y generar la ventana de diálogo donde se mostrará la información
	 * 
	 * @param texto String. Texto destinado a ser enviado al servidor a través de la conexión establecido con Socket. Con este texto se solicitará la lista de personas conectadas.
	 */
	protected void personasConectadas(String texto) {
		
		//En caso de que se haya abierto, reseteamos la ventana de diálogo
		if(_dialogConexion != null) _dialogConexion.dispose(); 
		//Inicializamos la ventana de diálogo
		_dialogConexion = new JDialog(this, "Usuarios/as conectados/as");
		
		_dialogConexion.setLayout(new BorderLayout());
		
		//se crea instancia de JLabel para el título que precede a la lista de personas conectadas
        JLabel titulo = new JLabel("\tUsuarios/as conectados/as");
        //Se añade el objeto JLabel al JDialog
        _dialogConexion.add(titulo, BorderLayout.NORTH);
        
        //Configuramos JTextArea para mostrar los usuarios/as conectados/as
        _areaConectados = new JTextArea();
		_areaConectados.setEditable(false);//Configuramos para que no sea editable
		_areaConectados.setLineWrap(true);//Configuramos para que haya ajuste de línea
		_areaConectados.setWrapStyleWord(true);
		_areaConectados.setMargin(new Insets(10,10,10,10));//Establecemos un margin para dejar algo de sangría
		JScrollPane scrollAreaConectados = new JScrollPane(_areaConectados);
        
        _dialogConexion.add(scrollAreaConectados, BorderLayout.CENTER);
        
        //tamaño para JDialog
        _dialogConexion.setSize(300, 200);

        // set visibility of dialog
        _dialogConexion.setVisible(true);
		
		_canalSalida.println(texto);
		_canalSalida.flush();
	}
	
	/**
	 * Método invocado cuando se pulsa Intro sobre el campo de texto habilitado para entrada de texto del usuario/a
	 * Se obtiene el texto escrito, se envía al servidor y se agrega al área de texto del chat.
	 */
	protected void onTextoEscrito() {
		
		//Almacenamos el texto escrito en el campo de texto de la persona usuaria en una variable tipo String
		String textoUsuario = _tfEntradaUsuario.getText();
		
		//En el caso de que haya contenido en la variable, de que haya texto escrito
		if(textoUsuario!=null) {
			
			//enviamos al servidor el texto introducido por la persona usuaria
			enviarServidor(textoUsuario);
			
			//Se invoca al método agregaTexto y se pasa el texto escrito más el nombre de usuario/a
			agregaTexto("Yo >> " + textoUsuario + "\n");
			//Vacíamos el campo de texto
			_tfEntradaUsuario.setText("");
			
		} 
	}//fin método onTextoEscrito
	
	/**
	 * Método invocado cuando se recibe texto desde el servidor. Se pinta el texto en el área de texto del cliente
	 * 
	 * @param texto String. Texto recibido del servidor por el canal de conexión por socket y se agrega al chat del cliente
	 */
	public void onTextoRecibido(String texto) {
		
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				//Llamamos al método agregaTexto y pasamos el texto recibido desde el hilo
				agregaTexto(texto);	
			}
			
		});
		
	}
	
	/**
	 * Método principal. Carga del programa.
	 * 
	 * @param args String. Se espera parámetro con el nombre con el que se concta el cliente.
	 */
	public static void main(String []args) {
		
		//args = new String[]{"Pedro"};
		
		//Variables
		Socket socket;//socket
		String host = "localhost";//nombre del host
		String nombreUsuario;//nombre de usuario/a
		int port = 4567;//puerto
		
		//Comprobamos si se reciben el número de argumentos correcto
		if(args.length<1) {
			
			//si no es así, se muestra error y se para el programa
			System.err.println("Faltan el nombre de usuario/a.");
			return;
		}
		
		//Se almacena el tercer argumento en nombreUsuario
		nombreUsuario = args[0];
		
		try {
			
			//Se inicializa el socket con los datos recibidos de conexión
			socket = new Socket(host, port);
			
		}catch(UnknownHostException e) {
			
			//Si salta error en cuanto al host se muestra error y se cierra el programa
			System.err.println("No se pudo resolver " + host);
			return;
			
		}catch(IOException e) {
			
			//Si salta excepción de entrada/salida se muestra error y se cierra el programa
			System.err.println("Error de entrada/salida al crear el socket"
					+ e.getLocalizedMessage());
			return;
		}
		
		//Creamos nueva instancia de Chat pasando como argumentos el socket conectado y el nombre de usuario/a
		Chat ventana = new Chat(socket, nombreUsuario);
	}
}


/**
 * @author joaquinalcazarcarrasco
 * 
 * Clase RecibeYEscribe. Implementa la clase Runnable. Se usa para crear hilo de ejecución que permanecerá a la escucha de los datos que
 * le envíe el servidor y los escribirá en el área de texto del cliente
 *
 */
class RecibeYEscribe implements Runnable {

	//Atributos
	private BufferedReader _reader;
	private Chat _chat;
	
	//Constructor
	/**
	 * Constructor de la clase
	 * 
	 * @param is Objeto de la clase InputStream. Canal de entrada por el que se recibirán datos del servidor. Se asociará a una instancia de BufferedReader, atributo de _reader de esta clase
	 * @param chat Objeto de la clase Chat. Ventana sobre la que se pintarán los datos recibidos desde el servidor
	 */
	public RecibeYEscribe(InputStream is, Chat chat) {
		
		//Creamos nuevo buffer de lectura asociado al stream de entrada pasado como argumento
		_reader = new BufferedReader(new InputStreamReader(is));
		//Almacenamos la ventana de chat en nuestra instancia de Chat
		_chat = chat;
	}
	
	/**
	 * Método invocado con el método start() de la clase Thread. La ejecución del hilo que se haya abierto.
	 *
	 */
	@Override
	public void run() {
		
		//Variable para acumular texto recibido desde el servidor
		String leido;
		
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
			
			if(leido.contains("##cambioNick##")) {
				
				String[] nuevoNick = leido.split("##cambioNick##");
				
				Chat._nombreUsuario = nuevoNick[1];
				_chat.setTitle(Chat._nombreUsuario + " - Ventana de Chat");
				
			}
			
			if(leido.equals("##inicio##")) Chat._areaConectados.setText("");
			
			else if(leido.contains("##user##")) {
				
				String[] conectados = leido.split("##user##");
				
				String conectado = conectados[1];
				
				Chat._areaConectados.append("\n" + conectado);
			
			}else {
		
				//Invocamos método para mostrar en ventana de chat el texto recibido desde el servidor
				_chat.onTextoRecibido(leido + "\n");
			
			}
			
		}//while
		
		try {
			
			//Cerramos el canal de entrada
			_reader.close();
		
		} catch (IOException e) {
		
		}
		
		_chat.onTextoRecibido("Fin de la hebra");
		
	}//run

}
