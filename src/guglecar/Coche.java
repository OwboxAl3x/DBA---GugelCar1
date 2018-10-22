/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guglecar;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 *
 * @author Adrian
 */
public class Coche extends SingleAgent {
    
    JsonObject inObjetoJSON = new JsonObject();
    JsonObject outObjetoJSON = new JsonObject();
    ACLMessage outbox = new ACLMessage();
    ACLMessage inbox = new ACLMessage();
    
    String clave;
    String comando = "login";
    String mapa = "";
    
    public Coche(AgentID aid) throws Exception  {
        super(aid);
    }
    
    /**
    *
    * @author Alejandro García
    */
    @Override
    public void init()  {
        
        System.out.println("\nAgente("+this.getName()+") Iniciando");
             
    }
    
    /**
    *
    * @author Alejandro García
    */
    @Override
    public void execute()  {
        
       System.out.println("\nAgente("+this.getName()+") haciendo el login en el servidor");
       this.logearse();
        
    }
    
    /**
    *
    * @author Alejandro García
    */
    public void logearse() {
        
        outObjetoJSON.add("command", comando);
        outObjetoJSON.add("world", mapa);
        outObjetoJSON.add("radar", "sensor");
        outObjetoJSON.add("scanner", "sensor");
        outObjetoJSON.add("gps", "sensor");
        
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID("Cerastes"));
        outbox.setContent(outObjetoJSON.toString());
        this.send(outbox);
        
        try {
            
            System.out.println("\nAgente("+this.getName()+") obteniendo respuesta del servidor");
            inbox = this.receiveACLMessage();
            inObjetoJSON = Json.parse(inbox.getContent()).asObject();
            
            if(!inObjetoJSON.get("result").asString().equals("BAD_MAP") && !inObjetoJSON.get("result").asString().equals("BAD_PROTOCOL")){
                
                this.clave = inObjetoJSON.get("result").asString();
                System.out.println("\nAgente("+this.getName()+") logueado");
                
            }
            
            System.err.println("Fallo en el mapa o en la estructura del mensaje");
            
            //Finalizar Perceptor
            
        } catch (InterruptedException ex) {
            
            System.err.println("Error al hacer el login");
            
        }
        
    }
    
    /**
    *
    * @author Alejandro García
    */
    @Override
    public void finalize()  {    
        
        System.out.println("\nAgente("+this.getName()+") Terminando"); 
        super.finalize();
        
    }
}
