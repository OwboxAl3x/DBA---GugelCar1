/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guglecar;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * Clase que hereda de SingleAgent, que controla al agente coche.
 *
 * @author Adrian
 * @author Alejandro García
 */
public class Coche extends SingleAgent {
    

    JsonObject percepcionJson = new JsonObject();
    boolean percepcionRecibida = false;
  
    ACLMessage outbox = new ACLMessage();
    
    String clave;
    String comando = "login";
    String mapa = "map1";
    String nombrePerceptor;
    
    double bateria = 0.0;
    
    int x;
    int y;
    int contador = 0;
    int contadorP = 0;
    
    ArrayList<ArrayList<Integer>> mapaMemoria = new ArrayList<>();
    ArrayList<ArrayList<Integer>> mapaPasos = new ArrayList<>();
    
    public Coche(AgentID aid, String nombrePerceptor) throws Exception  {
        super(aid);
        this.nombrePerceptor = nombrePerceptor;
    }
    
    /**
    *
    * @author Adrian
    * @author Alejandro García
    */
    @Override
    public void init()  {
        
        System.out.println("\nAgente("+this.getName()+") Iniciando");
             
    }
    
    /**
    *
    * @author Adrian
    * @author Alejandro García
    */
    @Override
    public void execute()  {
        
       System.out.println("\nAgente("+this.getName()+") haciendo el login en el servidor");
       this.logearse();
       
       this.calcularAccion();
        
    }
    
    /**
    * 
    * Hace que el coche le diga al servidor que quiere loguearse.
    *
    * @author Alejandro García
    * @author Manuel Ros Rodríguez
    * 
    */
    public void logearse() {
        
        JsonObject jsonLogin = new JsonObject();
        
        //outObjetoJSON.add("command", "asd"); // Para cuando devuelve traza el servidor
        jsonLogin.add("command", comando);
        jsonLogin.add("world", mapa);
        jsonLogin.add("radar", nombrePerceptor);
        jsonLogin.add("scanner", nombrePerceptor);
        jsonLogin.add("gps", nombrePerceptor);
        
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Cerastes"));
        outbox.setContent(jsonLogin.toString());
        this.send(outbox);
        
        JsonObject inJsonLogin = null;
        
        try {
              
            System.out.println("\nAgente("+this.getName()+") obteniendo respuesta del servidor");
            
            ACLMessage inboxLogin = this.receiveACLMessage();
            
            
            if (inboxLogin.getContent().contains("trace")){
                // Para ver la traza del último intento fallido
                inJsonLogin = Json.parse(inboxLogin.getContent()).asObject(); 
                this.crearImagen(inJsonLogin);
                // Hasta aqui
                
                inboxLogin = this.receiveACLMessage();
            }
            
            
            inJsonLogin = Json.parse(inboxLogin.getContent()).asObject();  
            
            if(!inJsonLogin.get("result").asString().equals("BAD_MAP") && !inJsonLogin.get("result").asString().equals("BAD_PROTOCOL")){
                System.out.println("Login: "+inJsonLogin);
                this.clave = inJsonLogin.get("result").asString();
                System.out.println("\nAgente("+this.getName()+") logueado");

            }else{
                System.err.println("Fallo en el mapa o en la estructura del mensaje");
                this.logout();
            }
            
        } catch (InterruptedException ex) {
            
            System.err.println("Error al hacer el login");
            
        }
        
    }
    
    /**
     * Calcula la siguiente acción que hará.
     * 
     * @author Manuel Ros Rodríguez
     * @author Fernando Ruiz Hernández
     * @author Adrian Martin
     * 
     */
    public void calcularAccion(){
        boolean salir = false;
        
        while (!salir){
            
            try {
                
                // Recibimos el mensaje del perceptor
                System.out.println("\nAgente("+this.getName()+") recibiendo percepción del perceptor");
                System.out.println("contador cocheMove:"+contador);
                System.out.println("contador cocheP:"+contadorP);
                if (!percepcionRecibida){
                    ACLMessage inboxAccion = this.receiveACLMessage();
                    percepcionJson = Json.parse(inboxAccion.getContent()).asObject();
                    contadorP++;
                } else {
                    percepcionRecibida = false;
                }
                 
                // Actualiza el mapa en memoria y el mapa de los pasos
                this.actualizarMapa(percepcionJson); 
                this.actualizarMapaPasos(percepcionJson); 
                
                // *** Comprobar si tiene que hacer refuel
                 if(bateria <= 1.0){
                    refuel();
                } else if (percepcionJson.get("radar").asArray().get(12).asInt() != 2){
                    // Algoritmo de cálculo de movimiento 
                    TreeMap<Float,String> casillas = new TreeMap<Float,String>();

                    if (percepcionJson.get("radar").asArray().get(6).asInt() != 1){
                        casillas.put(percepcionJson.get("scanner").asArray().get(6).asFloat(), "NW");
                    }
                    if (percepcionJson.get("radar").asArray().get(7).asInt() != 1){
                        casillas.put(percepcionJson.get("scanner").asArray().get(7).asFloat(), "N");
                    }
                    if (percepcionJson.get("radar").asArray().get(8).asInt() != 1){
                        casillas.put(percepcionJson.get("scanner").asArray().get(8).asFloat(), "NE");
                    }
                    if (percepcionJson.get("radar").asArray().get(11).asInt() != 1){
                        casillas.put(percepcionJson.get("scanner").asArray().get(11).asFloat(), "W");
                    }
                    if (percepcionJson.get("radar").asArray().get(13).asInt() != 1){
                        casillas.put(percepcionJson.get("scanner").asArray().get(13).asFloat(), "E");
                    }
                    if (percepcionJson.get("radar").asArray().get(16).asInt() != 1){
                        casillas.put(percepcionJson.get("scanner").asArray().get(16).asFloat(), "SW");
                    }
                    if (percepcionJson.get("radar").asArray().get(17).asInt() != 1){
                        casillas.put(percepcionJson.get("scanner").asArray().get(17).asFloat(), "S");
                    }
                    if (percepcionJson.get("radar").asArray().get(18).asInt() != 1){
                        casillas.put(percepcionJson.get("scanner").asArray().get(18).asFloat(), "SE");
                    }

                    Map.Entry<Float,String> casillaResultado = casillas.firstEntry();
                    
                    contador++;
                    this.moverse("move"+casillaResultado.getValue());
                    bateria--;
                } else {
                    // logout
                    System.out.println("Hemos llegado al objetivo"+percepcionJson);
                    salir = true;
                    this.logout();    
                }
            } catch (InterruptedException ex) {
                System.out.println("Error al recibir mensaje");
            }
        }
    }
    
    /**
    * 
    * Hace que el coche le diga al servidor a donde quiere moverse.
    *
    * @author Alejandro García
    * @param aDonde Indica la direccion a la que se va a mover el coche
    * 
    */
    public void moverse(String aDonde) {
        
        JsonObject outObjetoJSON = new JsonObject();
        outObjetoJSON.add("command", aDonde);
        outObjetoJSON.add("key", this.clave);
        
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Cerastes"));
        outbox.setContent(outObjetoJSON.toString());
        
        System.out.println("\nAgente("+this.getName()+") enviando movimiento al servidor");
        this.send(outbox);
        
        try {
            
            System.out.println("\nAgente("+this.getName()+") obteniendo respuesta del servidor");
            ACLMessage inboxMover = this.receiveACLMessage();
            
            if (inboxMover.getContent().contains("perceptor")){
                percepcionRecibida = true;
                percepcionJson = Json.parse(inboxMover.getContent()).asObject();
                System.out.println("perceptor:"+inboxMover.getContent());
                inboxMover = this.receiveACLMessage(); // ok
                contadorP++;
            } 
            JsonObject inJsonMover = null;
            System.out.println("fueraPerceptor:"+inboxMover.getContent());
            inJsonMover = Json.parse(inboxMover.getContent()).asObject();
            
            if(!inJsonMover.get("result").asString().equals("BAD_KEY") && !inJsonMover.get("result").asString().equals("BAD_PROTOCOL") && !inJsonMover.get("result").asString().equals("BAD_COMMAND")){
                
                if(!inJsonMover.get("result").asString().equals("CRASHED")){
                    
                    System.out.println("\nAgente("+this.getName()+") se ha movido");
                    
                } else {
                    
                    System.out.println("\nAgente("+this.getName()+") se ha chocado o se ha quedado sin bateria");
                    
                    //Desloguearse y avisar al Perceptor para que se cierre
                    this.logout();
                    
                    
                }
                
                
            }else{
                //Desloguearse y avisar al Perceptor para que se cierre
                System.err.println("Fallo en la estructura del mensaje");
                this.logout();
            }
            
        } catch (InterruptedException ex) {
            
            System.err.println("Error al moverse");
            
        }
        
    }
    
    /**
     * Actualiza el mapa en la memoria. Las casillas desconocidas se representan
     * con el valor -1.
     * 
     * @author Fernando Ruiz Hernández
     * @param percepcion Objeto JSON con la percepción recibida
     */
    public void actualizarMapa(JsonObject percepcion) {
        // Coordenadas de posición
        x = percepcion.get("gps").asObject().get("x").asInt();
        y = percepcion.get("gps").asObject().get("y").asInt();
        
        // Ajustar tamaño del mapa si es necesario
        int sizeNuevo = x + 3;
        if (sizeNuevo < y + 3)
            sizeNuevo = y + 3;
        if (sizeNuevo > mapaMemoria.size())
            this.extenderMapa(sizeNuevo);
        
        // Actualizar casillas
        int casilla_valor;
        int casilla_x;
        int casilla_y;
        for (int i=0; i<5; i++) {
            casilla_y = y + i - 2;
            for (int j=0; j<5; j++) {
                casilla_x = x + j - 2;
                casilla_valor = percepcion.get("radar").asArray().get(i*5 + j).asInt();
                if (casilla_x >= 0 && casilla_y >= 0)
                    mapaMemoria.get(casilla_y).set(casilla_x, casilla_valor);
            }
        }
    }
    
    /**
     * Extiende el tamaño del mapa en la memoria. Se rellena con el valor -1.
     * 
     * @author Fernando Ruiz Hernández
     * @param sizeNuevo Tamaño nuevo
     */
    public void extenderMapa(int sizeNuevo) {
        int size = mapaMemoria.size();
        
        // Extender filas existentes
        for (int i=0; i<size; i++) {
            for (int j=size; j<sizeNuevo; j++) {
                mapaMemoria.get(i).add(-1);
            }
        }

        // Añadir nuevas filas
        ArrayList<Integer> fila;
        for (int i=size; i<sizeNuevo; i++) {
            fila = new ArrayList<>();
            for (int j=0; j<sizeNuevo; j++) {
                fila.add(-1);
            }
            mapaMemoria.add(fila);
        }
    }
    
    
    /**
     * Actualiza el mapa de los pasos. Cada casilla contiene el número de veces
     * que se ha pasado por ella.
     * @author Fernando Ruiz Hernández
     * @param percepcion Objeto JSON con la percepción recibida
     */
    public void actualizarMapaPasos(JsonObject percepcion) {
        // Coordenadas de posición
        x = percepcion.get("gps").asObject().get("x").asInt();
        y = percepcion.get("gps").asObject().get("y").asInt();
        
        // Ajustar tamaño del mapa si es necesario
        int sizeNuevo = x + 3;
        if (sizeNuevo < y + 3)
            sizeNuevo = y + 3;
        if (sizeNuevo > mapaPasos.size())
            this.extenderMapaPasos(sizeNuevo);
        
        // Actualizar casillas
        int casilla_valor = mapaPasos.get(y).get(x);
        mapaPasos.get(y).set(x, casilla_valor+1);
    }
    
    /**
     * Extiende el tamaño del mapa de pasos. Se rellena con el valor 0.
     * 
     * @author Fernando Ruiz Hernández
     * @param sizeNuevo Tamaño nuevo
     */
    public void extenderMapaPasos(int sizeNuevo) {
        int size = mapaPasos.size();
        
        // Extender filas existentes
        for (int i=0; i<size; i++) {
            for (int j=size; j<sizeNuevo; j++) {
                mapaPasos.get(i).add(0);
            }
        }

        // Añadir nuevas filas
        ArrayList<Integer> fila;
        for (int i=size; i<sizeNuevo; i++) {
            fila = new ArrayList<>();
            for (int j=0; j<sizeNuevo; j++) {
                fila.add(0);
            }
            mapaPasos.add(fila);
        }
    }
    
    /**
    *
    * @author Adrian Martin
    * @author Manuel Ros Rodríguez
    */
    public void refuel(){
        
        System.out.println("\nAgente("+this.getName()+") recargando bateria");
        JsonObject jsonRefuel = new JsonObject();
        
        jsonRefuel.add("command", "refuel");
        jsonRefuel.add("key", this.clave);

        outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Cerastes"));
        outbox.setContent(jsonRefuel.toString());
        this.send(outbox);

        JsonObject inJsonRefuel = null;
        try {
            
            System.out.println("\nAgente("+this.getName()+") obteniendo respuesta del servidor");
            ACLMessage inboxRefuel = this.receiveACLMessage();
            inJsonRefuel = Json.parse(inboxRefuel.getContent()).asObject();
         
            if(inJsonRefuel.get("result").asString().equals("OK")){
                
                System.out.println("\nAgente("+this.getName()+") a tope de bateria");
                this.bateria = 99.0;
            } else {
                System.err.println("Fallo en la estructura del mensaje");
                this.logout();
            }
            
        } catch (InterruptedException ex) {
            
            System.err.println("Error al hacer el refuel");
            
        }
    }
    
    /**
    *
    * @author Adrian Martin
    * @author Manuel Ros Rodríguez
    */
    
    public void logout(){
        
        System.out.println("\nAgente("+this.getName()+") haciendo logout");
        System.out.println("contador cocheMove:"+contador);
        System.out.println("contador cocheP:"+contadorP);
        JsonObject jsonLogout = new JsonObject();
        
        jsonLogout.add("command", "logout");
        jsonLogout.add("key", this.clave);
            
        outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Cerastes"));
        outbox.setContent(jsonLogout.toString());
        this.send(outbox);
        
        JsonObject inJsonLogout = null;
        try {
            System.out.println("\nAgente("+this.getName()+") obteniendo respuesta del servidor");
            ACLMessage inboxLogout = this.receiveACLMessage();
            
            for (int i=0; i<3; i++){
                if (inboxLogout.getContent().contains("trace")){
                    inJsonLogout = Json.parse(inboxLogout.getContent()).asObject();
                } else if (!inboxLogout.getContent().contains("perceptor") && !inboxLogout.getContent().contains("OK")){
                    // ha ocurrido un fallo
                }
                inboxLogout = this.receiveACLMessage();
            }
            
            System.out.println("\nAgente("+this.getName()+") traza recibida, creando imagen");
            this.crearImagen(inJsonLogout);

            outbox = new ACLMessage();
            outbox.setSender(this.getAid());
            outbox.setReceiver(new AgentID(nombrePerceptor));
            outbox.setContent("logout");
            this.send(outbox);
            
            

        } catch (InterruptedException ex) {
            System.err.println("Error al hacer el logout"); 
        }
    }
    
    /**
     * Crea una imagen a partir de la traza
     * 
     * @author Manuel Ros Rodríguez
     * @param inJsonImagen
     */
    public void crearImagen(JsonObject inJsonImagen){
        
        try {
            JsonArray array = inJsonImagen.get("trace").asArray();
            byte data[] = new byte [array.size()];
            for (int i=0; i<data.length; i++){
                    data[i] = (byte) array.get(i).asInt();
            }
            FileOutputStream fos;
            fos = new FileOutputStream("traza"+this.mapa+".png");
            fos.write(data);
            fos.close();
            System.out.println("Imagen creada");
        } catch (IOException ex) {
            System.out.println("Fallo al crear la imagen");
        }
    }
    
    
    /**
    *
    * @author Adrian
    * @author Alejandro García
    */
    @Override
    public void finalize()  {    
        
        System.out.println("\nAgente("+this.getName()+") Terminando"); 
        super.finalize();
        
    }
}
