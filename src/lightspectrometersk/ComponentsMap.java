/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package lightspectrometersk;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author stellarkite
 */
public class ComponentsMap {
    
    private static volatile ComponentsMap instance = null;
    private final Map fcomponents = new HashMap();


    public ComponentsMap(){
    
        fcomponents.put("RedLED", "dacb");
        fcomponents.put("BlueLED", "daca");
        fcomponents.put("GreenLED", "dacc");
        fcomponents.put("ADC", "adc");
        
        // Relation between DAC and ADC
        fcomponents.put("adc0", "daca");
        fcomponents.put("adc1", "dacc");
        fcomponents.put("adc2", "dacb");

        
        // This class will function as a singleton
        instance = this;
    }
    
    public String GetUnitName(String alias) {
    
        return (String) fcomponents.get(alias);
    }
    
    public static ComponentsMap getInstance() {
        if (instance == null) {
            synchronized (ComponentsMap.class) {
                if (instance == null) {
                    instance = new ComponentsMap();
                }
            }
        }
        return instance;
    }
    
}
