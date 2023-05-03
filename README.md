# Hubitat
Hubitat Drivers, Apps, and Dashboard Icons

This repository contains my personal drivers and simple apps I wrote to use on Hubitat. 

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
