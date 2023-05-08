# Hubitat
Hubitat Drivers, Apps, and Dashboard Icons

This repository contains my personal drivers and simple apps I wrote, as well as other people's drivers and apps that I use with Hubitat. 

Dashboard Icons are hosted here: https://github.com/cburges2/ecowitt.github.io/tree/main/Dashboard%20Icons

Attic Fan Controller

A driver and app combo to run an attic fan. Vents for humidity and temperature.
Required Sensors: Fan Controller or Switch, Outside humidity and temperature, Attic humidity and temperature. 
Features: Vents based on setpoints and outside/attic sensor differences. Use with a switch or Fan Speed Controller (ex. GE Ceiling Fan Controller) If used With a speed controller, auto speed or manual option available. 
App: Attic Fan Controller App - Updates the driver attributes from the sensors. Controls the physical fan device, on/off and/or speed by subscribing to driver    attributes operatingState and fanSpeed. 
Driver: Attic Fan Controller - Controls all cycle logic and settings. 
Install: Add the driver code and create a virtual Attic Fan Controller device.  Add the app code, then install it as a user app. Choose your devices in the app.   
Rules: Vent for humidity if over setpoint and outside humidity is less than attic humidity. Vent for temperature if above setpoint and outside temperature is less than attic temperature, and outside humidity less than humidity-temp setpoint. 
Dashboard Icons: Presence is used for a dasboard status tile.  Colors can be set in the dashboard json Advanced settings for the different states, and the custom icons can be set by presence state in the Advanced settings CSS. (states: temperature, humidity, both, none)
Another companion app, Attic Fan Controller Sync, lets you use Variable Connectors (number) to update setpoints from the Dashboard. The app syncs the setpoints in the driver to the Variable Connectors (which are virtual devices). Create a Temperature setpoint and a humdity setpoint variable connectors, as numbers, and choose them in the sync app at install. Changes in the driver will show in the connectors, and changes in the connectors will update the driver attributes. 

Google App Scripts

Scripts I use to log Hubitat Data to Google Sheets.  One script is used for the logging, and the other is used to mangage log size based on days to retain. I call the scripts from Webcore to send the data. 

Solar Pool Heater Controller

A driver and app combo to run a solar pool heater pump.  
Required Sensors: Pump Switch, Temperature Sensor for heater box, Temperature Sensor for Pool, Illumination Sensor for outside. 
Features:  Turns on the heater pump when illumination is over setpoint or heater box temperature is over setpoint. 
App: Solar Pool Heater Controller App - updates the driver attributes. Turns on the pump device based on driver operationalState. 
Driver: Solar Pool Heater Controller - controls the cycles and logic using the attributes updated by the app. 
Install: Add the driver code and create a Solar Heater Controller device.   Add the app code, then istall it as a user app.  Choose you devices in the app. 
Rules: Turn on heater when solar over setpoint or Temp over setpoint.  Turn off if pool temp is at max setpoint. Do not turn on if heater temp is less than pool temp.
Dashbaord Icons: An icon is set from the web for each state, on for illumination, on for heat, on for both, or idle. A pool temperature confort icon is set based on pool temp.  The limits for water confort temperatures are set in Prefrences.  
Another Companion app, Solar Pool Heater Sync, lets you use Variable Connectors (number) to update setpoints from the Dashboard. The app syncs the setpoints in the driver to the Variable Connectors (which are virtual devices). Create an Illumination setpoint and a Temperature setpoint variable connectors, as numbers, and choose them in the sync app at install. 


                
