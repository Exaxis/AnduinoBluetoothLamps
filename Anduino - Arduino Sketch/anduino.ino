#include <HSBColor.h>

// PINS -----
int redPin = 11;
int greenPin = 10;
int bluePin = 9;

int hueInput = 1;
int satInput = 0;

// CONSTANTS -----
int RED = 0;
int GREEN = 1;
int BLUE = 2;

const int HW_INPUT_BUFFER_SIZE = 10;  // Size of the old hw color values buffer

const int ANALOG_DEADZONE_SIZE = 20;  // Deadzone size of analog input.
                               // The potentiometers will send values that slightly range. To keep
                               // this natural fluctuation from tricking us into thinking the user has
                               // made a hardware adjustment, we won't swap from bluetooth to hardware
                               // mode unless the difference between the lowest and highest values in the
                               // hardware value buffers exceeds this.

// VARIABLES -----
int colorRGB[3]; // Color values to send to LEDs

int hue;     //
int sat;     // Latest HSB values from active source
int bright;  //

int oldHWHues[HW_INPUT_BUFFER_SIZE];    //
int oldHWSats[HW_INPUT_BUFFER_SIZE];    // Old hardware value buffers
int oldHWBrights[HW_INPUT_BUFFER_SIZE]; //


int hwHue;    //
int hwSat;    // Latest HSB values from hardware
int hwBright; //

int btHue;     //
int btSat;     // Latest HSB values from Bluetooth
int btBright;  //

bool bluetoothMode; // Whether we're actively reading from bluetooth or hardware

void setup() {
  // put your setup code here, to run once:
  // NOTE: The LED pins will be used with AnalogWrite, 
  // which does not require them to be set up.
  
  Serial.begin(9600);
  
  // Set bluetooth mode to off
  bluetoothMode = false;
  
  // Initialize color array 
   colorRGB[RED] = 0;   
   colorRGB[GREEN] = 0; 
   colorRGB[BLUE] = 0;  
   
   hue = 0;      
   sat = 0;     
   bright = 99;  
   
   hwHue = 0;     
   hwSat = 0;    
   hwBright = 99; 
   
   btHue = 0;      
   btSat = 0;     
   btBright = 99;  
   
   // Initialize hw input buffers
   for(int i = 0; i < HW_INPUT_BUFFER_SIZE; i++){
     oldHWHues[i] = 0;
     oldHWSats[i] = 0;
     oldHWBrights[i] = 0;
   }
   
}

void loop() {
  // put your main code here, to run repeatedly:
  
  
  // Read latest hue and saturation from analog input.
  hwHue = analogRead(hueInput);
  hwSat = analogRead(satInput);
  hwBright = 99; 
  
  pushOldHWInputs(hwHue, hwSat, hwBright);

  if(bluetoothMode == true){
    if(isHWInputDeadzoneExceeded()){
      bluetoothMode = false;
    } 
  }

  if(!bluetoothMode){
    Serial.println(hue);
    hue = map(hwHue, 
              0, 
              1023,
              0,
              359);
  
    sat = map(hwSat, 
              0, 
              1023,
              0,
              99);
              
    bright = 99;
  }
  
  if(Serial.available() > 0){
      String data = Serial.readStringUntil('\n');
      processSerialData(data);
  }
  
  // Convert HSB to RGB values. 
  H2R_HSBtoRGB(hue, sat, bright, colorRGB);
  
  // Turn on light.
  analogWrite(redPin, colorRGB[RED]);
  analogWrite(greenPin, colorRGB[GREEN]);
  analogWrite(bluePin, colorRGB[BLUE]);
  
}

void pushOldHWInputs(int hue, int sat, int bright){
   for(int i = 0; i < HW_INPUT_BUFFER_SIZE-1; i++){
      oldHWHues[i] = oldHWHues[i+1];
      oldHWSats[i] = oldHWSats[i+1];
      oldHWBrights[i] = oldHWBrights[i+1];
   }
   
   oldHWHues[HW_INPUT_BUFFER_SIZE-1] = hue;
   oldHWSats[HW_INPUT_BUFFER_SIZE-1] = sat;
   oldHWBrights[HW_INPUT_BUFFER_SIZE-1] = bright;
}

bool isHWInputDeadzoneExceeded(){
  int minHue = oldHWHues[0];
  int minSat = oldHWSats[0];
  int minBright = oldHWBrights[0];
  int maxHue = oldHWHues[0];
  int maxSat = oldHWSats[0];
  int maxBright = oldHWBrights[0];
  
  for(int i = 1; i < HW_INPUT_BUFFER_SIZE; i++){
     if(oldHWHues[i] < minHue){
       minHue = oldHWHues[i]; 
     }
     if(oldHWHues[i] > maxHue){
       maxHue = oldHWHues[i]; 
     }
     
     if(oldHWSats[i] < minSat){
       minSat = oldHWSats[i]; 
     }
     if(oldHWSats[i] > maxSat){
       maxSat = oldHWSats[i]; 
     }
     
     if(oldHWBrights[i] < minBright){
       minBright = oldHWBrights[i]; 
     }
     if(oldHWBrights[i] > maxBright){
       maxBright = oldHWBrights[i]; 
     }
  }
  
  if(maxHue - minHue > ANALOG_DEADZONE_SIZE){
    return true; 
  }
  
  if(maxSat - minSat > ANALOG_DEADZONE_SIZE){
    return true; 
  }
  
  if(maxBright - minBright > ANALOG_DEADZONE_SIZE){
    return true; 
  }

  return false;
}

void processSerialData(String data){
  int length = data.length();
  Serial.print(data);
  if(data[0] == 'C'){
     // First char of 'C' means this is a color change command
     bluetoothMode = true;
     // Parse color data, in the format: HUE,SAT,BRIGHT
     int firstCommaIndex = data.indexOf(',');
     int secondCommaIndex = data.indexOf(',', firstCommaIndex+1);
     
     String hueString = data.substring(1, firstCommaIndex);
     String satString = data.substring(firstCommaIndex+1, secondCommaIndex);
     String brightString = data.substring(secondCommaIndex+1);
     
     hue = hueString.toInt();
     sat = satString.toInt();
     bright = brightString.toInt();
  } else if(data[0] == 'R'){
     //This is the Request color data command.
     // TODO: Write out color HSB
  }
}
