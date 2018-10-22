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
 * @author Adrian
 */
public class Perceptor extends SingleAgent {
    
    JsonObject inObjetoJSON;
    ACLMessage inbox;
    
    float scanner[];
    int radar[];
    int posX, posY;
    boolean fin;
    
    public Perceptor(AgentID aid) throws Exception  {
        super(aid);
    }
    
    /**
    *
    * @author Alejandro García
    */
    @Override
    public void init()  {
        
        System.out.println("\nAgente("+this.getName()+") Iniciando");
        
        inObjetoJSON = new JsonObject();
        inbox = new ACLMessage();
        
        scanner = new float[25];
        radar = new int[25];
        fin = false;
             
    }
    
    /**
    *
    * @author Alejandro García
    */
    @Override
    public void execute()  {
       
        System.out.println("\nAgente("+this.getName()+") Percibiendo");
        
    }
    
    /**
    *
    * @author Alejandro García
    */
    public void percibiendo() {
        
        while(!fin){
            
            try {
            
                System.out.println("\nAgente("+this.getName()+") obteniendo percepción del servidor");
                inbox = this.receiveACLMessage();
                inObjetoJSON = Json.parse(inbox.getContent()).asObject();

                if(inbox.getContent().contains("scanner")){

                    int i = 0;

                    for(JsonValue j : inObjetoJSON.get("scanner").asArray()){

                        this.scanner[i] = j.asFloat();

                        i++;

                    }

                } else if(inbox.getContent().contains("radar")){

                    int i = 0;

                    for(JsonValue j : inObjetoJSON.get("radar").asArray()){

                        this.radar[i] = j.asInt();

                        i++;

                    }

                } else if(inbox.getContent().contains("gps")){

                    this.posX = inObjetoJSON.get("gps").asObject().get("x").asInt();
                    this.posY = inObjetoJSON.get("gps").asObject().get("y").asInt();

                }

            } catch (InterruptedException ex) {

                System.err.println("Error al hacer el login");

            }
            
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
