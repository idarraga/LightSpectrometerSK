/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package photodiodevis;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lightspectrometersk.ComponentsMap;
import lightspectrometersk.SocketManager;

/**
 *
 * @author stellarkite
 */
public class PhotodiodeVis extends VBox implements Initializable {
    
    private static volatile PhotodiodeVis instance = null;
    private SocketManager socketMan;
    private ComponentsMap comp;
    
    // Graph
    private static final int MAX_DATA_POINTS = 50;
    private XYChart.Series series1;
    private XYChart.Series series2;
    private XYChart.Series series3;
    private int xSeriesData = 0;
    private ExecutorService executor;
    private AddToQueue addToQueue;

    private NumberAxis xAxis;

    @FXML
    private GridPane fPhotodiodeVisMainGridPane;
    @FXML
    private Button fscanButton;
    
    @FXML
    private void fscanHandler(ActionEvent event) {

        // When the executor.execute is called the plot updates
        executor.execute(addToQueue);

    }
    
    public PhotodiodeVis() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PhotodiodeVis.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // This class will function as a singleton
        instance = this;
        
    }
    
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        // Initializing the Photodiode tab
        System.err.println("Photodiode tab.");
        // Connect to the socket manager
        socketMan = SocketManager.getInstance();
        comp = ComponentsMap.getInstance();
        
        // Graph
        xAxis = new NumberAxis(0, socketMan.__ADC_MaxCounts, 100);
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(true);
        xAxis.setLabel("DAC count");
                
        //xAxis.setTickLabelsVisible(false);
        //xAxis.setTickMarkVisible(false);
        //xAxis.setMinorTickVisible(false);
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        
        final LineChart<Number, Number> sc = new LineChart<Number, Number>(xAxis, yAxis) {
            // Override to remove symbols on each data point
            @Override protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {}
        };
        sc.setAnimated(false);
        sc.setId("liveLineeChart");
        sc.setTitle("Photodiode response");

        //-- Chart Series
        series1 = new XYChart.Series<Number, Number>();
        series2 = new XYChart.Series<Number, Number>();
        series3 = new XYChart.Series<Number, Number>();
        sc.getData().addAll(series1, series2, series3);
        
        // Add it in the proper location
        fPhotodiodeVisMainGridPane.add(sc, 1, 0);
        
        executor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
        });
        
        // This will be run later with the executor which implements
        //  a separate thread
        addToQueue = new AddToQueue();
        
    }
    
    private class AddToQueue implements Runnable {
        
        public void run() {
            
            // Clear old data
            series1.getData().clear();
            series2.getData().clear();
            series3.getData().clear();
            
            
            double val;
            for (int itr = 500; itr <= 2000; itr += 100) {
                val = setOneDACReadPhotodiode(itr, "RedLED");
                //dataQ1.add( val );
                series1.getData().add( new AreaChart.Data(itr, val) );
            }
            setOneDACReadPhotodiode(0, "RedLED");

            val = 0;
            for (int itr = 500; itr <= 2000; itr += 100) {
                val = setOneDACReadPhotodiode(itr, "GreenLED");
                //dataQ1.add( val );
                series2.getData().add( new AreaChart.Data(itr, val) );
            }
            setOneDACReadPhotodiode(0, "GreenLED");
            
            val = 0;
            for (int itr = 500; itr <= 2000; itr += 100) {
                val = setOneDACReadPhotodiode(itr, "BlueLED");
                //dataQ1.add( val );
                series3.getData().add( new AreaChart.Data(itr, val) );
            }
            setOneDACReadPhotodiode(0, "BlueLED");

            
        }
    }
    
    public double setOneDACReadPhotodiode(int val, String dac_name) {

        String command = comp.GetUnitName(dac_name) + " set " + val + "\r";
        socketMan.Send(command);

        //textFieldsMap.get(dac_name).setText(String.valueOf(val));
        //slidersMap.get(dac_name).setValue((double) val);

        // Get DACs
        int[] values = refreshADC();
        // select the one to return
        /*
        switch (dac_name) {
            case "BlueLED":
                return ADCToVoltage( (double) values[1] );
            case "GreenLED":
                return ADCToVoltage( (double) values[3] );
            case "RedLED":
                return ADCToVoltage( (double) values[5] );
        }
        */
        
        return (double) values[7];

        //return 0.0;
    }
    
    public double ADCToVoltage(double adcread) {

        double val;
        val = -1 * socketMan.__LED_VRef / (double) socketMan.__ADC_MaxCounts;
        val = val * adcread;
        val = val + socketMan.__LED_VRef; // This is the ADC read in Volts

        return val;
    }
    
    private int[] refreshADC() {

        String ADCres = socketMan.Send(comp.GetUnitName("ADC") + " read\r");
        int[] values = socketMan.ExtractADCRead(ADCres, 8, 8); // 8 bytes to read, 8 extracted int values to retreived

        //Map<Integer, Label> ledsMapping = new HashMap<>();
        //ledsMapping.put(0, fBlueLED_ADCLabel); // ADC0 --> Blue
        //ledsMapping.put(1, fGreenLED_ADCLabel); // ADC1 --> Green
        //ledsMapping.put(2, fRedLED_ADCLabel); // ADC2 --> Red

        // I get the correct mapping using the indexes 0,1,2.  Bring the information of the port number
        //ledsMapping.get(values[0]).setText(String.valueOf(values[1]));
        //ledsMapping.get(values[2]).setText(String.valueOf(values[3]));
        //ledsMapping.get(values[4]).setText(String.valueOf(values[5]));
        // port 4 not used

        //Map<Integer, Label> ledsMappingCurrent = new HashMap<>();
        //ledsMappingCurrent.put(0, fBlueLED_ADCCurrentLabel); // ADC0 --> Blue
        //ledsMappingCurrent.put(1, fGreenLED_ADCCurrentLabel); // ADC1 --> Green
        //ledsMappingCurrent.put(2, fRedLED_ADCCurrentLabel); // ADC1 --> Red

        // I get the correct mapping using the indexes 0,1,2.  Bring the information of the port number
        String curr;
        double val;
        val = ADCToVoltage((double) values[1]);
        curr = String.format("%.3f", val);
        
        val = ADCToVoltage((double) values[3]);
        curr = String.format("%.3f", val);
        
        val = ADCToVoltage((double) values[5]);
        curr = String.format("%.3f", val);
        
        val = ADCToVoltage((double) values[7]);
        //curr = String.format("%.3f", val);
        System.out.printf("Photodiode = 0x%x %.3f\n", values[7], val);
        
        return values;
    }

}
