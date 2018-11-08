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
 * @author Adrian Martin
 * @author Alejandro García
 */
public class Coche extends SingleAgent {
    

    JsonObject percepcionJson = new JsonObject();
    boolean percepcionRecibida = false;
  
    ACLMessage outbox = new ACLMessage();
    
    String clave;
    String comando = "login";
    String mapa = "map8";
    String nombrePerceptor;
    
    double bateria = 0.0;
    
    int x;
    int y;
    int contador = 0;
    
    boolean metaEncontrada = false;
    
    ArrayList<ArrayList<Integer>> mapaMemoria = new ArrayList<>();
    ArrayList<ArrayList<Integer>> mapaPasos = new ArrayList<>();
    
    public Coche(AgentID aid, String nombrePerceptor) throws Exception  {
        super(aid);
        this.nombrePerceptor = nombrePerceptor;
    }
    
    /**
    *
    * @author Adrian Martin
    * @author Alejandro García
    */
    @Override
    public void init()  {
        
        System.out.println("\nAgente("+this.getName()+") Iniciando");
             
    }
    
    /**
    *
    * @author Adrian Martin
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
                
                if (contador%10 == 0)
                    System.out.println("Contador movimientos: "+contador);
                
                // Recibimos el mensaje del perceptor
                if (!percepcionRecibida){
                    ACLMessage inboxAccion = this.receiveACLMessage();
                    percepcionJson = Json.parse(inboxAccion.getContent()).asObject();
                } else {
                    percepcionRecibida = false;
                }
                 
                // Actualiza el mapa en memoria y el mapa de los pasos
                this.actualizarMapa(percepcionJson); 
                this.actualizarMapaPasos(percepcionJson); 
                
                // *** Comprobar si tiene que hacer refuel
                if(bateria <= 1.0){
                    if (this.comprobarMeta()) {
                        refuel();
                    }
                    else {
                        System.out.println("El objetivo no es alcanzable.");
                        salir = true;
                        this.logout();
                    }
                }else if (percepcionJson.get("radar").asArray().get(12).asInt() != 2){
                    // Algoritmo de cálculo de movimiento 
                    int minimo = Integer.MAX_VALUE;
                    
                    TreeMap<Float,String> casillas = new TreeMap<Float,String>();
                    
                    // Calculamos mínimo
                    if (percepcionJson.get("radar").asArray().get(6).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson,6)){
                            minimo = this.getValorPasos(percepcionJson,6);
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(7).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 7)){
                            minimo = this.getValorPasos(percepcionJson, 7);
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(8).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 8)){
                            minimo = this.getValorPasos(percepcionJson, 8);
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(11).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 11)){
                            minimo = this.getValorPasos(percepcionJson, 11);
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(13).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 13)){
                            minimo = this.getValorPasos(percepcionJson, 13);
                        } 
                    }
                    if (percepcionJson.get("radar").asArray().get(16).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 16)){
                            minimo = this.getValorPasos(percepcionJson, 16);
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(17).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 17)){
                            minimo = this.getValorPasos(percepcionJson, 17);
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(18).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 18)){
                            minimo = this.getValorPasos(percepcionJson, 18);
                        }
                    }
                    
                    // Añadir casillas
                    if (percepcionJson.get("radar").asArray().get(6).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson,6)){
                            casillas.put(percepcionJson.get("scanner").asArray().get(6).asFloat(), "NW");
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(7).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 7)){
                            casillas.put(percepcionJson.get("scanner").asArray().get(7).asFloat(), "N");
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(8).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 8)){
                            casillas.put(percepcionJson.get("scanner").asArray().get(8).asFloat(), "NE");
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(11).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 11)){
                            casillas.put(percepcionJson.get("scanner").asArray().get(11).asFloat(), "W");
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(13).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 13)){
                            casillas.put(percepcionJson.get("scanner").asArray().get(13).asFloat(), "E");
                        } 
                    }
                    if (percepcionJson.get("radar").asArray().get(16).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 16)){
                            casillas.put(percepcionJson.get("scanner").asArray().get(16).asFloat(), "SW");
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(17).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 17)){
                            casillas.put(percepcionJson.get("scanner").asArray().get(17).asFloat(), "S");
                        }
                    }
                    if (percepcionJson.get("radar").asArray().get(18).asInt() != 1){
                        if(minimo >= this.getValorPasos(percepcionJson, 18)){
                            casillas.put(percepcionJson.get("scanner").asArray().get(18).asFloat(), "SE");
                        }
                    }

                    Map.Entry<Float,String> casillaResultado = casillas.firstEntry();
                    
                    contador++;
                    
                    this.moverse("move"+casillaResultado.getValue());
                    
                    bateria--;
                } else {
                    // logout
                    System.out.println("Hemos llegado al objetivo.");
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
        
        this.send(outbox);
        
        try {
            
            ACLMessage inboxMover = this.receiveACLMessage();
            
            if (inboxMover.getContent().contains("perceptor")){
                percepcionRecibida = true;
                percepcionJson = Json.parse(inboxMover.getContent()).asObject();
                inboxMover = this.receiveACLMessage(); // ok
            } 
            JsonObject inJsonMover = null;
            inJsonMover = Json.parse(inboxMover.getContent()).asObject();
            
            if(!inJsonMover.get("result").asString().equals("BAD_KEY") && !inJsonMover.get("result").asString().equals("BAD_PROTOCOL") && !inJsonMover.get("result").asString().equals("BAD_COMMAND")){
                
                if(inJsonMover.get("result").asString().equals("CRASHED")){
                    
                    System.out.println("\nAgente("+this.getName()+") se ha chocado o se ha quedado sin bateria");
                    
                    // Desloguearse y avisar al Perceptor para que se cierre
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
    /*
    * @author Adrian Martin 
    * 
    */
    
    public int getValorPasos(JsonObject percepcion, int celda) {
        // Coordenadas de posición
        x = percepcion.get("gps").asObject().get("x").asInt();
        y = percepcion.get("gps").asObject().get("y").asInt();
        
        int valor = Integer.MAX_VALUE;
        switch (celda){
            case 6:
                valor = this.mapaPasos.get(y-1).get(x-1);
                break;
            case 7:
                valor = this.mapaPasos.get(y-1).get(x);
                break;
            case 8:
                valor = this.mapaPasos.get(y-1).get(x+1);
                break;
            case 11:
                valor = this.mapaPasos.get(y).get(x-1);
                break;
            case 13:
                valor = this.mapaPasos.get(y).get(x+1);
                break;
            case 16:
                valor = this.mapaPasos.get(y+1).get(x-1);
                break;
            case 17:
                valor = this.mapaPasos.get(y+1).get(x);
                break;
            case 18:
                valor = this.mapaPasos.get(y+1).get(x+1);
                break;
            
            
        }
        return valor;
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
                if (casilla_valor == 2)
                    metaEncontrada = true;
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
     * Comprueba si la meta es alcanzable, es decir, si hay algún camino entre
     * la meta y el coche.
     * 
     * @author Fernando Ruiz Hernández
     * @return true si la meta es alcanzable, false si no lo es
     */
    public boolean comprobarMeta() {
        if (!metaEncontrada)
            return true;
        
        ArrayList<ArrayList<Integer>> mapaMeta = new ArrayList<>();
        
        int size = mapaMemoria.size();
        int casilla_valor;
        
        // Añadir nuevas filas        
        ArrayList<Integer> fila;
        for (int i=0; i<size; i++) {
            fila = new ArrayList<>();
            for (int j=0; j<size; j++) {
                casilla_valor = mapaMemoria.get(i).get(j);
                if (casilla_valor == 1)
                    fila.add(1);
                else if (casilla_valor == 2)
                    fila.add(2);
                else
                    fila.add(0);
            }
            fila.add(0); // Columna adicional
            mapaMeta.add(fila);
        }
        // Fila adicional
        fila = new ArrayList<>();
        for (int j=0; j<size+1; j++)
            fila.add(0);
        mapaMeta.add(fila);
        
        // Actualizamos size por la columna y fila adicionales
        size = size + 1;
        
        // Valor del jugador en mapaMeta : 9
        if (mapaMemoria.get(y).get(x) == 2)
            return true;
        mapaMeta.get(y).set(x, 9);
        
        boolean salir = false;
        
        // casillaVecina
        int casillaV_valor;
        int casillaV_x;
        int casillaV_y;
        
        int expansiones;
        
        while (!salir) {
            for (int i=0; i<size; i++) {
                for (int j=0; j<size; j++) {
                    casilla_valor = mapaMeta.get(i).get(j);
                    if (casilla_valor == 2) {
                        for (int ci=-1; ci<=1; ci++) {
                            casillaV_y = i+ci;
                            for (int cj=-1; cj<=1; cj++) {
                                casillaV_x = j+cj;
                                if (casillaV_x >= 0 && casillaV_x < size && casillaV_y >= 0 && casillaV_y < size) {
                                    casillaV_valor = mapaMeta.get(casillaV_y).get(casillaV_x);
                                    if (casillaV_valor == 9)
                                        return true;
                                    if (casillaV_valor == 0)
                                        mapaMeta.get(casillaV_y).set(casillaV_x, 4);
                                }
                            }
                        }
                        mapaMeta.get(i).set(j, 3);
                    }
                }
            }
            
            expansiones = 0;
            for (int i=0; i<size; i++) {
                for (int j=0; j<size; j++) {
                    casilla_valor = mapaMeta.get(i).get(j);
                    if (casilla_valor == 4) {
                        mapaMeta.get(i).set(j, 2);
                        expansiones++;
                    }
                }
            }
            
            if (expansiones == 0)
                salir = true;
        }
        
        return false;
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
        System.out.println("Contador movimientos: "+contador);
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
            ACLMessage inboxLogout = this.receiveACLMessage();
            
            for (int i=0; i<3; i++){
                if (inboxLogout.getContent().contains("trace")){
                    inJsonLogout = Json.parse(inboxLogout.getContent()).asObject();
                } else if (!inboxLogout.getContent().contains("perceptor") && !inboxLogout.getContent().contains("OK")){
                    System.err.println("Error al recibir respuesta de logout.");
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
    * @author Adrian Martin
    * @author Alejandro García
    */
    @Override
    public void finalize()  {    
        
        System.out.println("\nAgente("+this.getName()+") Terminando"); 
        super.finalize();
        
    }
}
