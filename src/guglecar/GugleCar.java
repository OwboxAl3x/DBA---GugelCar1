/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guglecar;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adri
 */
public class GugleCar {

    /**
     * @author Alejandro García
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        Coche car = null;
        Perceptor sensor = null;
        
        AgentsConnection.connect("isg2.ugr.es", 6000, "Cerastes", "Boyero", "Carducci", false);
        
        try {
            
            car = new Coche(new AgentID("car"));
            sensor = new Perceptor(new AgentID("sensor"));
            
        } catch (Exception ex) {
            
            System.err.println("Fallo al crear al agente coche/sensor");
            System.exit(1);
            
        }
        
        car.start();
        sensor.start();
        
    }
    
}
