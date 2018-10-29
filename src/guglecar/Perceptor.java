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
 * @author Adrian
 * @author Alejandro García
 * @author Manuel Ros Rodríguez
 */
public class Perceptor extends SingleAgent {
    
    JsonObject inObjetoJSON;
    JsonObject outObjetoJSON;
    ACLMessage inbox;
    ACLMessage outbox;
    
    float scanner[];
    int radar[];
    int posX, posY;
    int contador = 0;
    int contadorP = 0;
    boolean fin;
    
    String nombreCoche;
    
    public Perceptor(AgentID aid, String nombreCoche) throws Exception  {
        super(aid);
        this.nombreCoche = nombreCoche;
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
    * @author Adrian
    * @author Alejandro García
    */
    @Override
    public void execute()  {
       
        System.out.println("\nAgente("+this.getName()+") Percibiendo");
        this.percibiendo();
        
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
                System.out.println("fin vale:"+fin+" y perceptor contador:"+contador+ " y el mensaje:"+inbox.getContent());
                System.out.println("sender:"+inbox.getSender()+" y yo:"+this.getAid());
                
                if (!inbox.getContent().contains("logout")){
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
                        outObjetoJSON.add("perceptor","si");
                        outbox.setSender(this.getAid());
                        outbox.setReceiver(new AgentID(nombreCoche));
                        outbox.setContent(outObjetoJSON.toString());
                        this.send(outbox);
                        outObjetoJSON = new JsonObject();
                        contador++;
                        System.out.println("contador perceptor"+contador);
                        /*inbox = this.receiveACLMessage(); // aqui recibe mensaje de percepcion, y acaba provocando crasheo más adelante
                        System.out.println("valor de fin:"+this.fin+"valor inbox: "+inbox.getContent());
                        if (!inbox.getContent().contains("OK")){

                            // no estoy seguro de que hacer ***

                        }*/

                    }  
                } else {
                    /*System.out.println("\nAgente("+this.getName()+") obteniendo logout del coche");
                    outbox.setSender(this.getAid());
                    outbox.setReceiver(new AgentID(nombreCoche));
                    outbox.setContent("OK");
                    this.send(outbox);*/
                    System.out.println("contador perceptor"+contador);
                    this.fin = true;
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
