/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guglecar;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 *
 * @author Adrian
 */
public class Perceptor extends SingleAgent {
    
    public Perceptor(AgentID aid) throws Exception  {
        super(aid);
    }
    
    @Override
    public void init()  {
        
        System.out.println("Agente("+this.getName()+") Iniciando");
             
    }
    
    @Override
    public void execute()  {
       
        
    }
    
    @Override
    public void finalize()  {    
        
        System.out.println("Agente("+this.getName()+") Terminando"); 
        super.finalize();
        
    }
}
