# Hubitat
Hubitat Drivers, Apps, and Dashboard Icons

This repository contains my personal drivers and simple apps I wrote, as well as other people's drivers and apps that I use with Hubitat. 

Dashboard Icons are hosted here: https://github.com/cburges2/ecowitt.github.io/tree/main/Dashboard%20Icons

Most of my drivers that control switches also set a contact sensor and/or lock, and/or motion sensor attributes with on/off. If your devices are Alexa controlled, you can use this to directly trigger an Alexa routine to control a device based on contact open/close, lock locked/unlocked, or motion active/inactive. In these cases, do not set up the optional switch device in the app. 

I also commonly use on overloaded presence sensor attribute for status.  This allows setting dashboard icon background colors by setting colors in the Dashboard Advanced JSON by presence template type (template tile colors at top of file).  I also update status icon attributes in the drivers for use as dashbaord attributes icons. By layering dashboard tiles using transparency, you can create status tiles that change colors and icons with the states. The icons have a path in prefrences that point to my icon repository here, however that can be changed to use another location, such as the hub local files. 

I also set display attributes in my drivers for use on Dashbaords.  These provide summary information about the current states and setpoints to make a dashboard status tiles. 

Attic Fan Controller

A driver and app combo to run an attic fan. Vents for humidity and temperature.
Required Sensors: Outside humidity and temperature, Attic humidity and temperature. 
Required Devices: A Switch or a Fan Controller for the fan. 
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
Required Sensors: Temperature Sensor for heater box, Temperature Sensor for Pool, Illumination Sensor for outside. 
Required Devices: Pump Heater Switch to run the pump
Features:  Turns on the heater pump when illumination is over setpoint or heater box temperature is over setpoint. 
App: Solar Pool Heater Controller App - updates the driver attributes. Turns on the pump device based on driver operationalState. 
Driver: Solar Pool Heater Controller - controls the cycles and logic using the attributes updated by the app. 
Install: Add the driver code and create a Solar Heater Controller device.   Add the app code, then istall it as a user app.  Choose you devices in the app. 
Rules: Turn on heater when solar over setpoint or Temp over setpoint.  Turn off if pool temp is at max setpoint. Do not turn on if heater temp is less than pool temp.
Dashbaord Icons: An icon is set from the web for each state, on for illumination, on for heat, on for both, or idle. A pool temperature confort icon is set based on pool temp.  The limits for water confort temperatures are set in Prefrences.  
Another Companion app, Solar Pool Heater Sync, lets you use Variable Connectors (number) to update setpoints from the Dashboard. The app syncs the setpoints in the driver to the Variable Connectors (which are virtual devices). Create an Illumination setpoint and a Temperature setpoint variable connectors, as numbers, and choose them in the sync app at install. 

Irrigation Controller (with Fertilize)

A driver and app combo to automatically water a garden or plants.  
Required Sensors: Soil Moisture Sensor
Required Devices: Water Pump switch or Water Valve Switch.  Optional: Fertilizer pump or valve switch
Features: Tracks soil moisture and waters at set moisture level.  Setpoints include minutes at start moisture before waterering starts, Minutes at target moisture when watering, max minutes to stop watering (if stop target limit not reached).  The Max moisture reacheed becomes the stop target for next water.  Optional Fertilize option, with a setpoint for how many minutes to fertilize.  Fertilize can be turned off. 
App: Irrigation Copntroller Sync - updates the moisture attribute in the app. Turns on and off the pumps. 
Driver: Controls all the logic for watering, and setpoints, updated by the app. 
Install: Add the driver code and create an irrigation controller device. Set you setpoints using the driver attributes in the device page. Add the app code, and install the app as a user app.  Choose your devices in the app. 
Rules: When moisture drops to target, wait setpoint minutes and confirm moisture stays at or below target.  If it rises in this time watering is cancelled.  Once watering, a timer is started for Max Minutes.  Watering will end at max minutes if not already off.  When soil moisture reaches target moisture, the minutes at Target timer starts, and watering will end after it is at target for setpoint minutes.  Max moisture reached is target for next watering.  If using Fertilize option, it will fertilize every other watering for setpoint minutes. Fertilize attribute will flip between true and false between waterings. Set the fertilize attribute to off to not use fertilize. 

Timer Irrigation Controller

A driver and app combo to water plants on a timer.  If using Alexa triggered devices, the app is not needed to run a switch. 
Required Sensors: none
Required Devices: Water valve or pump switch
Features: Runs a pump or valve to water plants ever X minutes for X seconds.  
App: Used if you need to control a Hubitat connected switch.  Otherwise, use the sensor attributes to make an Alexa routine for on/off.
Driver: Controls the timer for the pump. 
Install: Install the driver code and create a Timer Irrigation Controller device. If using a switch, install the Irrigation Sync app. 
Rules: If time is active, water for set seconds every set period of time.  Cycle will continue until timer is stopped by changing the driver attribute to off. 

Virtual T6 Thermostat Controller APP

A driver and an app combo.  THe driver is the Honeywell T6 Pro Virtual Thermostat driver. This is a companion app and driver to go with the Advanced Honeywell T6 Pro Thermostat driver. 
The T6 pro is a great thermostat, but the hysteresis is large and cannot be set, producting large temperature swings.  This app/driver solves that issue with a more percise hysteresis setting. 
The other problem is the temperature sensor.  It is C converted to F and rounded to a whole degree. This means it can never really display 70 degrees F, for example.  The app uses a remote temp sensor with more accuracy. 
All setting are still done through the physical thermostat, in software or from the wall unit, and they are synced to the virtual thermostat.  Heat/Cool Setpoints, etc.  
The virtual thermostat takes advantage of the sensor calibration function of the T6 pro.  By changing the calibration, the thermostat can be forced to turn on or off.

Additional Features:

A stop hysteresis that can be set different than start hysteresis - good for hot water heat that will continue to push up the temperature after heating stops
Operational Brightness and Idle Brightness set in the virtual thermostat, are separate settings for display brightness when heating/cooling and idle. To make display brighter when operational. 
The app allows for up to two other devices to be turned on/off with heat/cool and fan/idle.  Such as an electic fireplace on when heating. 
The virtual thermostat driver also sets a contact attribute to open when heating, and it sets a motion attribute to active when cooling. (for linking Alexa devices via contact or motion attribute triggers when virtual thermostat is shared with Alexa)
There is an option in the app to sync the humidity display on the T6 thermostat to a remote humidity sensor by using the sensorCal feature.  


