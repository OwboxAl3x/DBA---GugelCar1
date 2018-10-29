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
    
    JsonObject inObjetoJSON = new JsonObject();
    JsonObject outObjetoJSON = new JsonObject();
    JsonObject percepcionJson = new JsonObject();
    boolean percepcionRecibida = false;
  
    ACLMessage outbox = new ACLMessage();
    ACLMessage inbox = new ACLMessage();
    
    String clave;
    String comando = "login";
    String mapa = "map1";
    String nombrePerceptor = "sensor7";
    
    double bateria = 0.0;
    
    int x;
    int y;
    int contador = 0;
    
    ArrayList<ArrayList<Integer>> mapaMemoria = new ArrayList<>();
    
    public Coche(AgentID aid) throws Exception  {
        super(aid);
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
        
        //outObjetoJSON.add("command", "asd"); // Para cuando devuelve traza el servidor
        outObjetoJSON.add("command", comando);
        outObjetoJSON.add("world", mapa);
        outObjetoJSON.add("radar", nombrePerceptor);
        outObjetoJSON.add("scanner", nombrePerceptor);
        outObjetoJSON.add("gps", nombrePerceptor);
        
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Cerastes"));
        outbox.setContent(outObjetoJSON.toString());
        this.send(outbox);
        
        try {
            
            System.out.println("\nAgente("+this.getName()+") obteniendo respuesta del servidor");
            
            inbox = this.receiveACLMessage();
            
            if (inbox.getContent().contains("trace")){
                inbox = this.receiveACLMessage();
            }
            
            inObjetoJSON = Json.parse(inbox.getContent()).asObject();  
            
            if(!inObjetoJSON.get("result").asString().equals("BAD_MAP") && !inObjetoJSON.get("result").asString().equals("BAD_PROTOCOL")){
                
                this.clave = inObjetoJSON.get("result").asString();
                System.out.println("\nAgente("+this.getName()+") logueado");
                this.calcularAccion();
                
            }
            
            System.err.println("Fallo en el mapa o en la estructura del mensaje");
            
            //Finalizar Perceptor
            
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
                
                if (!percepcionRecibida){
                    inbox = this.receiveACLMessage();
                    percepcionJson = Json.parse(inbox.getContent()).asObject();
                    contador++;
                } else {
                    percepcionRecibida = false;
                }
                 
                // Actualiza el mapa en memoria
                this.actualizarMapa(percepcionJson);
                
                /*outbox = new ACLMessage();
                outbox.setSender(this.getAid());
                outbox.setReceiver(new AgentID("sensor"));
                outbox.setContent("OK");
                this.send(outbox);*/
                
                
                // *** Comprobar si tiene que hacer refuel
                 if(bateria <= 1.0){
                    refuel();
                }
                // Algoritmo de cálculo de movimiento                
                if (percepcionJson.get("radar").asArray().get(12).asInt() != 2){
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

                    this.moverse("move"+casillaResultado.getValue());
                    bateria--;
                } else {
                    // logout
                    System.out.println("Hemos llegado al objetivo"+percepcionJson);
                    
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
        
        outObjetoJSON.add("command", aDonde);
        outObjetoJSON.add("key", this.clave);
        
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Cerastes"));
        outbox.setContent(outObjetoJSON.toString());
        
        System.out.println("\nAgente("+this.getName()+") enviando movimiento al servidor");
        this.send(outbox);
        
        try {
            
            System.out.println("\nAgente("+this.getName()+") obteniendo respuesta del servidor");
            inbox = this.receiveACLMessage();
            
            if (inbox.getContent().contains("perceptor")){
                percepcionRecibida = true;
                percepcionJson = Json.parse(inbox.getContent()).asObject();
                System.out.println("perceptor:"+inbox.getContent());
                inbox = this.receiveACLMessage(); // ok
                contador++;
            } 
            
            System.out.println("fueraPerceptor:"+inbox.getContent());
            inObjetoJSON = Json.parse(inbox.getContent()).asObject();
            
            if(!inObjetoJSON.get("result").asString().equals("BAD_KEY") && !inObjetoJSON.get("result").asString().equals("BAD_PROTOCOL") && !inObjetoJSON.get("result").asString().equals("BAD_COMMAND")){
                
                if(!inObjetoJSON.get("result").asString().equals("CRASHED")){
                    
                    System.out.println("\nAgente("+this.getName()+") se ha movido");
                    
                } else {
                    
                    System.out.println("\nAgente("+this.getName()+") se ha chocado o se ha quedado sin bateria");
                    
                    //Desloguearse y avisar al Perceptor para que se cierre
                    
                    
                }
                
            }
            
            System.err.println("Fallo en la estructura del mensaje");
            
            //Desloguearse y avisar al Perceptor para que se cierre
            
            
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

        try {
            
            System.out.println("\nAgente("+this.getName()+") obteniendo respuesta del servidor");
            inbox = this.receiveACLMessage();
            inObjetoJSON = Json.parse(inbox.getContent()).asObject();
            
            if(inObjetoJSON.get("result").asString().equals("OK")){
                
                System.out.println("\nAgente("+this.getName()+") a tope de bateria");
                this.bateria = 99.0;
            } else {
                System.err.println("Fallo en la estructura del mensaje");
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
        System.out.println("contador coche:"+contador);
        JsonObject jsonLogout = new JsonObject();
        
        jsonLogout.add("command", "logout");
        jsonLogout.add("key", this.clave);
            
        outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Cerastes"));
        outbox.setContent(jsonLogout.toString());
        this.send(outbox);

        try {
            
            System.out.println("\nAgente("+this.getName()+") obteniendo respuesta del servidor");
            inbox = this.receiveACLMessage();
            inObjetoJSON = Json.parse(inbox.getContent()).asObject();
            
            if(inObjetoJSON.get("result").asString().equals("OK")){
                
                System.out.println("\nAgente("+this.getName()+") deslogueado");
                
                inbox = this.receiveACLMessage();
                System.out.println("percepcionJson:"+percepcionJson);
                System.out.println("traza:"+inbox.getContent());
                inObjetoJSON = Json.parse(inbox.getContent()).asObject();
                
                System.out.println("\nAgente("+this.getName()+") traza recibida, creando imagen");
                this.crearImagen();
                
                outbox = new ACLMessage();
                outbox.setSender(this.getAid());
                outbox.setReceiver(new AgentID(nombrePerceptor));
                outbox.setContent("logout");
                this.send(outbox);
                
                /*inbox = this.receiveACLMessage();

                if (!inbox.getContent().equals("OK")){
                    // ni idea
                }*/
            }
            
            System.err.println("Fallo en la estructura del mensaje");

            
        } catch (InterruptedException ex) {
            
            System.err.println("Error al hacer el logout");
            
        }
    }
    
    /**
     * Crea una imagen a partir de la traza
     * 
     * @author Manuel Ros Rodríguez
     */
    public void crearImagen(){
        try {
            JsonArray array = inObjetoJSON.get("trace").asArray();
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
