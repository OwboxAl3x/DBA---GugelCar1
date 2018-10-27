/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guglecar;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 * 
 * Clase que hereda de SingleAgent, que controla al agente Perceptor
 *
 * @author Adrian and Alejandro García and Manuel Ros Rodríguez
 */
public class Perceptor extends SingleAgent {
    
    JsonObject inObjetoJSON;
    JsonObject outObjetoJSON;
    ACLMessage inbox;
    ACLMessage outbox;
    
    float scanner[];
    int radar[];
    int posX, posY;
    boolean fin;
    
    public Perceptor(AgentID aid) throws Exception  {
        super(aid);
    }
    
    /**
    *
    * @author Adrian
    * @author Alejandro García
    * @author Manuel Ros Rodríguez
    */
    @Override
    public void init()  {
        
        System.out.println("\nAgente("+this.getName()+") Iniciando");
        
        inObjetoJSON = new JsonObject();
        outObjetoJSON = new JsonObject();
        inbox = new ACLMessage();
        outbox = new ACLMessage();
        
        scanner = new float[25];
        radar = new int[25];
        fin = false;
             
    }
    
    /**
    *
    * @author Adrian and Alejandro García
    */
    @Override
    public void execute()  {
       
        System.out.println("\nAgente("+this.getName()+") Percibiendo");
        //this.percibiendo();
        
    }
    
    /**
    * 
    * Método que pone al Perceptor a la espera de los mensajes de los sensores.
    *
    * @author Alejandro García
    * @author Manuel Ros Rodríguez
    */
    public void percibiendo() {
        
        while(!fin){
            
            try {
            
                // Suponiendo que se recibe un mensaje distinto con cada percepción, por tanto 3 mensajes
                // Falta tener en cuenta el caso donde recibe un mensaje de logout del coche
                System.out.println("\nAgente("+this.getName()+") obteniendo percepción del servidor");
                
                // Recibimos las percepciones y combinamos los JSON
                inbox = this.receiveACLMessage();
                inObjetoJSON = Json.parse(inbox.getContent()).asObject();
                outObjetoJSON.merge(inObjetoJSON);
                
                inbox = this.receiveACLMessage();
                inObjetoJSON = Json.parse(inbox.getContent()).asObject();
                outObjetoJSON.merge(inObjetoJSON);
                
                inbox = this.receiveACLMessage();
                inObjetoJSON = Json.parse(inbox.getContent()).asObject();
                outObjetoJSON.merge(inObjetoJSON);

                if(inbox.getContent().contains("CRASHED")){
                    
                    System.err.println("El vehiculo ha chocado");  
                    this.fin = true;
                    
                } else {
                    
                    outbox.setSender(this.getAid());
                    outbox.setReceiver(new AgentID("car"));
                    outbox.setContent(outObjetoJSON.toString());
                    this.send(outbox);
                    
                    inbox = this.receiveACLMessage();
                    
                    if (!inbox.getContent().contains("OK")){
                        
                        // no estoy seguro de que hacer ***
                        
                    }
                    
                }  

            } catch (InterruptedException ex) {

                System.err.println("Error al hacer el login");

            }
            
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
