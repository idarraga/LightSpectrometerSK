/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lightspectrometersk;

import java.io.IOException;
import java.lang.Integer;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author idarraga
 */
public class DACs extends VBox implements Initializable {

    private static volatile DACs instance = null;
    private SocketManager socketMan;
    private ComponentsMap comp;

    Map<String, TextField> textFieldsMap = new HashMap<>();
    Map<String, Slider> slidersMap = new HashMap<>();

    // Charts
    // axes
    final NumberAxis xAxis = new NumberAxis();
    final NumberAxis yAxis = new NumberAxis();
    final LineChart<Number, Number> lineChart
            = new LineChart<>(xAxis, yAxis);
    XYChart.Series<Number, Number> seriesRed = new XYChart.Series<>();
    XYChart.Series<Number, Number> seriesGreen = new XYChart.Series<>();
    XYChart.Series<Number, Number> seriesBlue = new XYChart.Series<>();
    
    // From XML
    @FXML
    private CheckBox fRCheckBox;
    @FXML
    private CheckBox fGCheckBox;
    @FXML
    private CheckBox fBCheckBox;
    @FXML
    private TextField fminScanTextField;
    @FXML
    private TextField fmaxScanTextField;
    @FXML
    private TextField fstepScanTextField;

    @FXML
    private GridPane fDACsMainGridPane;

    @FXML
    private Button fscanDACsButton;

    @FXML
    private Button frefreshADCReadButton;
    @FXML
    private Button fsetZeroButton;

    @FXML
    private Slider fRedLEDSlider;
    @FXML
    private TextField fRedLEDTextField;
    @FXML
    private Label fRedLED_ADCLabel;
    @FXML
    private Label fRedLED_ADCCurrentLabel;

    @FXML
    private Slider fGreenLEDSlider;
    @FXML
    private TextField fGreenLEDTextField;
    @FXML
    private Label fGreenLED_ADCLabel;
    @FXML
    private Label fGreenLED_ADCCurrentLabel;

    @FXML
    private Slider fBlueLEDSlider;
    @FXML
    private TextField fBlueLEDTextField;
    @FXML
    private Label fBlueLED_ADCLabel;
    @FXML
    private Label fBlueLED_ADCCurrentLabel;

    @FXML
    private void frefreshADCReadHandler(ActionEvent event) {

        refreshADC();

    }

    @FXML
    private void fscanDACsHandler(ActionEvent event) {

        
        // Clear previous data 
        seriesRed.getData().clear();
        seriesGreen.getData().clear();
        seriesBlue.getData().clear();
        
        double val;
        int min = Integer.valueOf( fminScanTextField.getText() );
        int max = Integer.valueOf( fmaxScanTextField.getText() );
        int step = Integer.valueOf( fstepScanTextField.getText() );
        
        if ( fRCheckBox.isSelected() ) {
            setZero();
            for (int itr = min; itr <= max; itr += step) {
                val = setOneDAC(itr, "RedLED");
                seriesRed.getData().add(new XYChart.Data(itr, val));
                //seriesRed.getData().notify();
            }
            setZero();
        }

        if ( fGCheckBox.isSelected() ) {
            setZero();
            for (int itr = min; itr <= max; itr += step) {
                val = setOneDAC(itr, "GreenLED");
                seriesGreen.getData().add(new XYChart.Data(itr, val));
            }
            setZero();
        }

        if ( fBCheckBox.isSelected() ) {
            setZero();
            for (int itr = min; itr <= max; itr += step) {
                val = setOneDAC(itr, "BlueLED");
                seriesBlue.getData().add(new XYChart.Data(itr, val));
            }
            setZero();
        }

    }

    @FXML
    private void fsetZeroHandler(ActionEvent event) {

        setDACs(0);

    }

    private int[] refreshADC() {

        String ADCres = socketMan.Send(comp.GetUnitName("ADC") + " read\r");
        int[] values = socketMan.ExtractADCRead(ADCres, 8, 8); // 8 bytes to read, 8 extracted int values to retreived

        Map<Integer, Label> ledsMapping = new HashMap<>();
        ledsMapping.put(0, fBlueLED_ADCLabel); // ADC0 --> Blue
        ledsMapping.put(1, fGreenLED_ADCLabel); // ADC1 --> Green
        ledsMapping.put(2, fRedLED_ADCLabel); // ADC2 --> Red

        // I get the correct mapping using the indexes 0,1,2.  Bring the information of the port number
        ledsMapping.get(values[0]).setText(String.valueOf(values[1]));
        ledsMapping.get(values[2]).setText(String.valueOf(values[3]));
        ledsMapping.get(values[4]).setText(String.valueOf(values[5]));
        // port 4 not used

        Map<Integer, Label> ledsMappingCurrent = new HashMap<>();
        ledsMappingCurrent.put(0, fBlueLED_ADCCurrentLabel); // ADC0 --> Blue
        ledsMappingCurrent.put(1, fGreenLED_ADCCurrentLabel); // ADC1 --> Green
        ledsMappingCurrent.put(2, fRedLED_ADCCurrentLabel); // ADC1 --> Red

        // I get the correct mapping using the indexes 0,1,2.  Bring the information of the port number
        String curr;
        double val;
        val = ADCToVoltage((double) values[1]);
        curr = String.format("%.3f", val);
        ledsMappingCurrent.get(values[0]).setText(curr);

        val = ADCToVoltage((double) values[3]);
        curr = String.format("%.3f", val);
        ledsMappingCurrent.get(values[2]).setText(curr);

        val = ADCToVoltage((double) values[5]);
        curr = String.format("%.3f", val);
        ledsMappingCurrent.get(values[4]).setText(curr);

        val = ADCToVoltage((double) values[7]);
        //curr = String.format("%.3f", val);
        System.out.printf("Photodiode = 0x%x %.3f\n", values[7], val);
        
        return values;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Initializing the DACs tab
        System.err.println("DACs tab.");
        // Connect to the socket manager
        socketMan = SocketManager.getInstance();
        comp = ComponentsMap.getInstance();
        // Set the DAC behaviour
        setupDacControl();

        // Fill maps
        textFieldsMap.put("RedLED", fRedLEDTextField);
        textFieldsMap.put("GreenLED", fGreenLEDTextField);
        textFieldsMap.put("BlueLED", fBlueLEDTextField);

        slidersMap.put("RedLED", fRedLEDSlider);
        slidersMap.put("GreenLED", fGreenLEDSlider);
        slidersMap.put("BlueLED", fBlueLEDSlider);

        // Set the chart
        xAxis.setLabel("DAC value");
        yAxis.setLabel("Volts");

        seriesRed.setName("R");
        seriesGreen.setName("G");
        seriesBlue.setName("B");
        
        // Add series
        lineChart.getData().addAll(seriesRed, seriesGreen, seriesBlue);

        // Assign a class style to these nodes in order to be able to
        //  use the css file for styling
        seriesRed.getNode().getStyleClass().add("series-" + seriesRed.getName() );
        seriesGreen.getNode().getStyleClass().add("series-" + seriesGreen.getName() );
        seriesBlue.getNode().getStyleClass().add("series-" + seriesBlue.getName() );

        // Add it in the proper location
        fDACsMainGridPane.add(lineChart, 1, 0);

    }

    public double ADCToVoltage(double adcread) {

        double val;
        val = -1 * socketMan.__LED_VRef / (double) socketMan.__ADC_MaxCounts;
        val = val * adcread;
        val = val + socketMan.__LED_VRef; // This is the ADC read in Volts

        return val;
    }


    public DACs() {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DACs.fxml"));
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

    public static DACs getInstance() {
        if (instance == null) {
            synchronized (DACs.class) {
                if (instance == null) {
                    instance = new DACs();
                }
            }
        }
        return instance;
    }

    public void setupDacControl() {

        // Scans
        fRCheckBox.setVisible(true);
        fRCheckBox.setSelected(true);
        fGCheckBox.setVisible(true);
        fGCheckBox.setSelected(true);
        fBCheckBox.setVisible(true);
        fBCheckBox.setSelected(true);
        fminScanTextField.setText(String.valueOf(0));
        fmaxScanTextField.setText(String.valueOf(socketMan.__ADC_MaxCounts));
        fstepScanTextField.setText(String.valueOf(100));

        
        ////////////////////////////////////////////////////////////////////////////////////////
        // RedLED
        fRedLEDSlider.setMin(0);
        fRedLEDSlider.setMax(4095);

        // While Dragging change the value but only set DACs when releasing the mouse click
        fRedLEDSlider.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent m) {
                fRedLEDTextField.setText(String.valueOf((int) fRedLEDSlider.getValue()));
                // Don set DAC nor read ADC while dragging.  Generated too much traffic.
            }
        });
        fRedLEDSlider.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent m) {
                // Set the Dac
                int val = (int) fRedLEDSlider.getValue();
                String command = comp.GetUnitName("RedLED") + " set ";
                command += val + "\r";
                socketMan.Send(command);
                fRedLEDTextField.setText(String.valueOf(val));
                refreshADC();
            }
        });
        fRedLEDTextField.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                if (t.getCode() == KeyCode.ENTER) {
                    // Extract the value from TextField
                    String text = fRedLEDTextField.getText();
                    if (DacStringValid(text, fRedLEDSlider)) {
                        int val = Integer.parseInt(text);
                        // move the slider first
                        fRedLEDSlider.setValue(val);
                        // Set the Dac
                        String command = comp.GetUnitName("RedLED") + " set ";
                        command += val + "\r";
                        socketMan.Send(command);
                    } else {
                        // Set the Dac to zero if the user enters something wrong
                        setZero();
                        fRedLEDTextField.setText(String.valueOf(0));
                    }
                    refreshADC();
                }
            }
        });
        ////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////
        // GreenLED
        fGreenLEDSlider.setMin(0);
        fGreenLEDSlider.setMax(4095);

        // While Dragging change the value but only set DACs when releasing the mouse click
        fGreenLEDSlider.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent m) {
                fGreenLEDTextField.setText(String.valueOf((int) fGreenLEDSlider.getValue()));
                // Don set DAC nor read ADC while dragging.  Generated too much traffic.
            }
        });
        fGreenLEDSlider.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent m) {
                // Set the Dac
                int val = (int) fGreenLEDSlider.getValue();
                String command = comp.GetUnitName("GreenLED") + " set ";
                command += val + "\r";
                socketMan.Send(command);
                fGreenLEDTextField.setText(String.valueOf(val));
                refreshADC();
            }
        });
        fGreenLEDTextField.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                if (t.getCode() == KeyCode.ENTER) {
                    // Extract the value from TextField
                    String text = fGreenLEDTextField.getText();
                    if (DacStringValid(text, fGreenLEDSlider)) {
                        int val = Integer.parseInt(text);
                        // move the slider first
                        fGreenLEDSlider.setValue(val);
                        // Set the Dac
                        String command = comp.GetUnitName("GreenLED") + " set ";
                        command += val + "\r";
                        socketMan.Send(command);
                    } else {
                        // Set the Dac to zero if the user enters something wrong
                        setZero();
                        fGreenLEDTextField.setText(String.valueOf(0));
                    }
                    refreshADC();
                }
            }
        });
        ////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////
        // BlueLED
        fBlueLEDSlider.setMin(0);
        fBlueLEDSlider.setMax(4095);

        // While Dragging change the value but only set DACs when releasing the mouse click
        fBlueLEDSlider.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent m) {
                fBlueLEDTextField.setText(String.valueOf((int) fBlueLEDSlider.getValue()));
                // Don set DAC nor read ADC while dragging.  Generated too much traffic.
            }
        });
        fBlueLEDSlider.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent m) {
                // Set the Dac
                int val = (int) fBlueLEDSlider.getValue();
                String command = comp.GetUnitName("BlueLED") + " set ";
                command += val + "\r";
                socketMan.Send(command);
                fBlueLEDTextField.setText(String.valueOf(val));
                refreshADC();
            }
        });
        fBlueLEDTextField.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                if (t.getCode() == KeyCode.ENTER) {
                    // Extract the value from TextField
                    String text = fBlueLEDTextField.getText();
                    if (DacStringValid(text, fBlueLEDSlider)) {
                        int val = Integer.parseInt(text);
                        // move the slider first
                        fBlueLEDSlider.setValue(val);
                        // Set the Dac
                        String command = comp.GetUnitName("BlueLED") + " set ";
                        command += val + "\r";
                        socketMan.Send(command);
                    } else {
                        // Set the Dac to zero if the user enters something wrong
                        setZero();
                        fBlueLEDTextField.setText(String.valueOf(0));
                    }
                    refreshADC();
                }
            }
        });
        ////////////////////////////////////////////////////////////////////////////////////////

        //lineChart.setOn
        
    }

    public double setOneDAC(int val, String dac_name) {

        String command = comp.GetUnitName(dac_name) + " set " + val + "\r";
        socketMan.Send(command);

        textFieldsMap.get(dac_name).setText(String.valueOf(val));

        slidersMap.get(dac_name).setValue((double) val);

        // Get DACs
        int[] values = refreshADC();
        // select the one to return
        if (dac_name == "BlueLED") {
            return ADCToVoltage((double) values[1]);
        } else if (dac_name == "GreenLED") {
            return ADCToVoltage((double) values[3]);
        } else if (dac_name == "RedLED") {
            return ADCToVoltage((double) values[5]);
        }

        return 0.0;
    }

    public void setDACs(int R, int G, int B) {

        String command = comp.GetUnitName("RedLED") + " set " + R + "\r";
        socketMan.Send(command);
        command = comp.GetUnitName("GreenLED") + " set " + B + "\r";
        socketMan.Send(command);
        command = comp.GetUnitName("BlueLED") + " set " + B + "\r";
        socketMan.Send(command);

        fRedLEDTextField.setText(String.valueOf(R));
        fGreenLEDTextField.setText(String.valueOf(G));
        fBlueLEDTextField.setText(String.valueOf(B));

        fRedLEDSlider.setValue(R);
        fGreenLEDSlider.setValue(G);
        fBlueLEDSlider.setValue(B);

        // And read ADC
        refreshADC();

    }

    public void setDACs(int initVal) {

        setDACs(initVal, initVal, initVal);

    }

    public void setZero() {
        setDACs(0);
    }

    public boolean DacStringValid(String s, Slider sl) {

        if (s.isEmpty()) {
            return false;
        }

        try {
            double d = Double.parseDouble(s);
            System.out.println(String.valueOf(d));
        } catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }

}
