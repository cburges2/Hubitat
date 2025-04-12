/*
	Greehouse Driver - The device will be created as a child device by installing the Greenhouse Controller App

	Copyright 2025 -> C. Burgess  

	This driver works with a companion app, "GreenHouse Fan Controller App", to control a greenHouse fan to vent for over temperature, a heater to heat when under temperature,
	heating pads for seedlings when under temperature, and a circulator fan with run rules.  All components can be enabled/disabled. 

	To use the thermostat template for dashboard controls on the dashboard (using thermostat tile):
		Mode: Mode changes automatically. Setpoint can be changed when greenhouse operatingState is in that mode.  When idle, just change mode to what setpoint you want to change, 
			and it will revert to "idle" (if state is idle) after ten seconds.  Changing the mode on the tile for a setpoint change does not change the operating mode for the greenhouse. 
		FanMode: "auto": sets vent fan to use auto speed.  "disable": disables the vent fan   "low", "medium", or "high": sets vent fan to manual, and sets the manual speed

	To use the buttons for dashboard controls (button tiles with push):
	1- Heater enable/disable
	2- Heat Pads enable/disable
	3- Vent Fan enable/disable
	4- Circulator enable/disable
	5- Heat Pads setpoint lower (layer button on top of heat pad setpoint display tile)
	6- Heat Pads setpoint higher (layer button on top of heat pad setpoint display tile)
	7. Toggle Circulate with Vent setting
	8. Toggle the Circulate with Heat setting

	To use the Command Variable String Tile to change settings (instead of using the up and down buttons for setpoint changes, and needed to set the circulator on temp):
		The "variable" attribute and capabililty allows adding a String Variable tile to the dashboard.  It will just display empty when not in use. 
		To send a command, fill in the map by using command:value.  The commands are coolsetpoint (or csp), heatsetpoint (or hsp), hysteresis (or hys), padsetpoint (hps) 
		circontemp (or cot), and autothreshold (or ath). example: circontemp:65 or cot:65.  Several commands can be combined, but must be comma separated. ex. csp:80,hsp:65 
		
	Build the Heat Pad Setpoint Tile:
		The presence attribute is to provide a "Heat pads On" background color for the Heat Pad Setpoint tile, when heat pads are activated.  "present" when on, "not present" when off
		Create the presence tile, remove the title and icon with css. Add the heatpad display attribute tile, and make it background transparent, and put it on top of the presence tile. 
		Set the presence colors in the dashboard template options.  Add the buttons for changing the settings on top of that tile, and make the buttons on top and background transparent.   

	Auto fan setting: When the fan is in auto mode, it will cycle speed when below the auto temp threshold.  When trend changes to falling, it will go down one speed, when trend
		changes to rising, it will go up one speed, IF temp is within the set threshold from setpoint. Above that threshold, fan will just stay on high. 
		In manual mode, the speed stays at fanSpeed set, and it does not auto cycle speeds based on temp trend and threshold. 

	v. 1.0 - 3/16/25  - Inital code to create a Green House controller using temperature setpoints to turn on a vent fan or turn on a heater.
	v. 1.2 - 3/19/25  - Temp setpoint changed to seperate heating and cooling setpoints. If heat and cool setpoints are the same, add at least 1 degree hysteresis. 
	v. 1.3 - 3/20/25  - Added heating pad control with its own temp setpoint, to turn off the seedling heating pads during the day when it is warm. Also added an iFrame attribute
						to pull up a URL (google chart)
	v. 1.4 - 3/21/25  - Added the ability to disable vent, heat, and pads.  Added a button controller for dashboards, to disable individual components, and a display for what's 
						enabled. Pushed 1 is heat, pushed 2 is heat pads, push 3 is ventalator. 
	v. 1.5 - 3/25/25  - Added a circulator fan.  It runs if enabled but turns off if the vent fan comes on. button push 4 is enables/disables the circulator fan. 
	v. 1.6 - 3/26/25  - Added thermostat attributes and commands to use a thermostat tile for control of fan, setting the cooling setpoint, and setting the heating setpoint. 
						Updated circulator fan to have a setpoint in settings, and made circulate with vent a setting option.  Added a circulate with heat option and setting. 
					    Added buttons 5 and 6, to set the heaterPads setpoint.  Button 5 is down temp, and button 6 is up temp for the heating pad setpoint.
	v. 2.0 - 4/3/25   - Converted this driver into a child device, created by Greenhouse Controller app.  Now driver calls methods in parent app instead of the app using subscriptions
						to monitor the driver states.
	v. 2.1 - 4/8/25   - Added buttons 7 and 8 to toggle the circulate vent and circulate heat settings from the dashboard. Added variable command circontemp (or cot) to set the 
						Circulator On Temp, and autothreshold (or ath) to set the fan auto threshold, to what can be set from the dashboard variable command tile.
*/

import java.text.SimpleDateFormat

metadata {
	definition (
		name: "Greenhouse Driver",
		namespace: "Hubitat",
		author: "C.Burgess"
	) {
        //capability "Light"
		capability "Actuator"
        capability "Presence Sensor"  				// changes presence with heat pads on (present) or pads off (not present)
		capability "Variable"
		capability "Pushable Button"
		capability "Temperature Measurement"
		capability "RelativeHumidityMeasurement"

		// attributes
		attribute "operatingState", "ENUM"  		// ["venting", "idle", "heating"]
		attribute "heatPadState", "ENUM"			// on, off
		attribute "circulatorState", "ENUM"			// on, off
		attribute "fanState", "ENUM"				// on, off
		attribute "humidity", "ENUM"	    		// set from app
		attribute "outsideHumidity", "ENUM"			// set from app
		attribute "hysteresis", "NUMBER"			// diff between heating and venting from setpoint when heat and cool setpoints are the same or close together
		attribute "displayEnabled", "STRING"       	// attribute for dashboard status tile showing enabled states 
        attribute "displaySetpoints", "STRING"		// attribute for dashboard status tile showing setpoints    
		attribute "displayStatus", "STRING"			// attribute for dashboard status tile showing all status and settings
		attribute "displayTemp", "STRING"			// attribute for dashboard status tile showing greenhouse temperature and trend arrow
		attribute "displayTemps", "STRING"			// attribute for dashboard status tile showing temps and humidity only
		attribute "displayPadSet", "String"			// attribute for the heat pad temp setting
        attribute "temperature", "NUMBER"        	// set from app
		attribute "tempTrend", "ENUM"				// rising, falling
		attribute "outTempTrend", "ENUM"			// rising, falling
        attribute "outsideTemp", "NUMBER"           // set from app
        attribute "heatingSetpoint", "ENUM"         // set from the thermostat tile
        attribute "coolingSetpoint", "ENUM"      	// set from the thermostat tile
		attribute "heatPadSetpoint", "ENUM"			// set using the buttons for heatpad temp up and temp down
		attribute "fanSpeed", "ENUM"   			    // low, medium, high, off - manually set as a speed reference, or auto set, based on threshold setting
		attribute "fanMode", "ENUM"					// auto or manual (auto adjusts speeds based on temp rise and fall below threshold
        attribute "presence", "ENUM"                // set to present when heat pads are on, not present when they are off
		attribute "icon", "STRING"                  // status icon attribute to be use on a dashboard tile (heating (heater), cooling (fan), or idle (OK checkmark))
		
		attribute "iconFile", "STRING"				// file name used for the icon attribute
		attribute "message", "STRING"				// a status message that describes the current states in english
		attribute "variable", "STRING"				// to send commands to the driver from a dashbaord variable tile
		attribute "heatPads", "String"				// heater pads enabled or dsiabled
		attribute "heater", "String"				// heater enabled or disabled
		attribute "vent", "String"					// vent enabled or disabled
		attribute "circulator", "String"			// circulator enabled or disabled
		attribute "chartLauncher", "text"			// launch a URL as an iFrame over the dashboard (google charts)
		attribute "chartLauncher2", "text"		    // launch another URL as an iFrame over the dashboard (google charts)
		attribute "pushed", "enum"
		
		// to use a thermostat tile on a dashboard for venting and heating, to change setpoints and show status, and to control the vent fan speeds/disable
		attribute "thermostatMode", "ENUM"
		attribute "thermostatOperatingState", "ENUM"
		attribute "thermostatFanMode", "ENUM"
		attribute "supportedThermostatFanModes", "JSON_OBJECT"
		attribute "supportedThermostatModes", "JSON_OBJECT"

		// Commands used to change internal attributes of virtual device from the commands page or app
        command "setOperatingState", [[name:"operatingState",type:"ENUM", description:"Set Operating State", constraints:["venting","idle","heating"]]]
		command "setHeatPadState", [[name:"heatPadState",type:"ENUM", description:"Set Heater Pad State", constraints:["on","off"]]]
		command "setCirculatorState", [[name:"circulatorState",type:"ENUM", description:"Set Circulator State", constraints:["on","off"]]]
		command "setFanState", [[name:"fanState",type:"ENUM", description:"Set Vent Fan State", constraints:["on","off"]]]
		command "setHeatPads", [[name:"heatPads",type:"ENUM", description:"Set Heater Pads Enabled/Disabled", constraints:["enabled","disabled"]]]
		command "setHeater", [[name:"heater",type:"ENUM", description:"Set Heater Enabled/Disabled", constraints:["enabled","disabled"]]]
		command "setVent", [[name:"vent",type:"ENUM", description:"Set Vent Fan Enabled/Disabled", constraints:["enabled","disabled"]]]
		command "setCirculator", [[name:"circulator",type:"ENUM", description:"Set Circulator Fan Enabled", constraints:["enabled","disabled"]]]	
		command "setFanMode", [[name:"fanMode",type:"ENUM", description:"Set Vent Fan Mode", constraints:["auto","manual","off"]]]	
		command "setHumidity", ["ENUM"]
		command "setOutsideHumidity", ["ENUM"]
        command "setHysteresis", ["NUMBER"]
        command "manageCycle"
        command "setDisplay", ["ENUM"]
        command "setTemperature", ["NUMBER"]
        command "setOutsideTemp", ["NUMBER"]
        command "setHeatingSetpoint", ["NUMBER"]
		command "setCoolingSetpoint", ["NUMBER"]
		command "setHeatPadSetpoint", ["NUMBER"]
        command "setFanSpeed", [[name:"fanSpeed",type:"ENUM", description:"Set Fan Speed", constraints:["low","medium","high"]]]
        command "setPresence", [[name:"presence",type:"ENUM", description:"Set Presence State", constraints:["present","not present"]]]
		command "setIcon", ["STRING"]
		command "setMessage", ["STRING"]
		command "setVariable", ["STRING"]  			// used to run commands from a dashboard string variable tile
		command "initialize"

		command "setThermostatFanMode", [[name:"thermostatFanMode",type:"ENUM", description:"Set Vent Fan Mode", constraints:["auto","low","medium","high","disabled"]]]
		command "setThermostatMode", [[name:"thermostatMode",type:"ENUM", description:"Set Thermostat Mode", constraints:["heat","cool","idle"]]]
		command "setChartFrame", [[type:"TEXT",description:"Set Button Text", defaultValue:"Temperature"],[type:"TEXT",description:"Set URL", defaultValue: "https://tinyurl.com/greenhousechart"],[type:"TEXT",description:"Set Height px", defaultValue:"600px"],[type:"TEXT",description:"Set Width px", defaultValue:"1230px"]
		,[type:"TEXT",description:"Set Left Pos %", defaultValue:"2%"],[type:"TEXT",description:"Set Top Pos %", defaultValue:"5%"]]
		command "setChartFrame2", [[type:"TEXT",description:"Set Button Text", defaultValue:"Energy"],[type:"TEXT",description:"Set URL", defaultValue: "https://tinyurl.com/greenhouseheater"],[type:"TEXT",description:"Set Height px", defaultValue:"475px"],[type:"TEXT",description:"Set Width px", defaultValue:"546px"]
		,[type:"TEXT",description:"Set Left Pos %", defaultValue:"2%"],[type:"TEXT",description:"Set Top Pos %", defaultValue:"5%"]]		
}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
		input( name: "circulateVent", type:"bool", title: "Circulate when Venting (do not turn off when venting)", defaultValue:false)
		input( name: "circulateHeat", type:"bool", title: "Circulate Only when Heating (turn off on idle)",defaultValue: false)
		input( name: "circulatorOnTemp", type:"enum", title: "Temp to keep circulator on when idle", defaultValue:"60", options:[50.0:"50",55.0:"55",60.0:"60",65.0:"65",70.0:"70"])
		
		input( name: "fanAutoThreshold", type:"enum", title: "Vent Fan Auto Speed Temp Theshold", defaultValue:"2", options:[1.0:"1",2.0:"2",3.0:"3",4.0:"4",5.0:"5"])
		//input( name: "fanMediumThreshold", type:"enum", title: "Vent Fan Medium Speed Difference", defaultValue:"3", options:[1.0:"1",2.0:"2",3.0:"3",4.0:"4",5.0:"5"])
		//input( name: "fanHighThreshold", type:"enum", title: "Vent Fan High Speed Difference", defaultValue:"6", options:[2.0:"2",3.0:"3",4.0:"4",5.0:"5",6.0:"6",7.0:"7",8.0:"8",9.0:"9",10.0:"10"])
		
		input( name: "iconPath", type: "string", description: "Address Path to icons", title: "Set Icon Path", defaultValue: "https://cburges2.github.io/ecowitt.github.io/Dashboard%20Icons/")
	    input( name: "tempFontSize", type:"enum", title: "Temp Display Font Size",defaultValue:"18", options:[10:"10",11:"11",12:"12",13:"13",14:"14",15:"15",16:"16",17:"17",18:"18"])	
		input( name: "tempsFontSize", type:"enum", title: "Temps Display Font Size",defaultValue:"17", options:[10:"10",11:"11",12:"12",13:"13",14:"14",15:"15",16:"16",17:"17",18:"18"])			
		input( name: "statusFontSize", type:"enum", title: "Status Display Font Size",defaultValue:"14", options:[10:"10",11:"11",12:"12",13:"13",14:"14",15:"15",16:"16",17:"17",18:"18"])			
		input( name: "padsSetFontSize", type:"enum", title: "Pad Setpoint Display Font Size",defaultValue:"14", options:[10:"10",11:"11",12:"12",13:"13",14:"14",15:"15",16:"16",17:"17",18:"18"])			
		input( name: "setpointsFontSize", type:"enum", title: "Setpoints Display Font Size",defaultValue:"16", options:[10:"10",11:"11",12:"12",13:"13",14:"14",15:"15",16:"16",17:"17",18:"18"])		
		input( name: "enabledFontSize", type:"enum", title: "Enabled Display Font Size",defaultValue:"14", options:[10:"10",11:"11",12:"12",13:"13",14:"14",15:"15",16:"16",17:"17",18:"18"])	
	}
}

def installed() {
	log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])
	
	setOperatingState("idle")
	sendEvent(name: "heatPadState", value: "off")
	sendEvent(name: "circulatorState", value: "off")
	sendEvent(name: "temperature", value: 75.0)
	sendEvent(name: "fanSpeed", value: "off")
	sendEvent(name: "heatingSetpoint", value: 45)
	sendEvent(name: "coolingSetpoint", value: 80)
	sendEvent(name: "heatPadSetpoint", value: "70")
	sendEvent(name: "outsideTemp", value: 75.0)
	sendEvent(name: "outsideHumidity", value: "50")
	sendEvent(name: "humidity", value: 50)
	sendEvent(name: "tempTrend", value: "steady")
	sendEvent(name: "outTempTrend", value: "steady")
	sendEvent(name: "variable", value: "[]")
	sendEvent(name: "circulator", value: "")
	sendEvent(name: "fanState", value: "off")
	sendEvent(name: "message", value: "none")
    sendEvent(name: "heatPads", value: "enabled")
    sendEvent(name: "vent", value: "enabled")
    sendEvent(name: "circulator", value: "enabled")
    sendEvent(name: "heater", value: "enabled")
	setThermostatAttributes()
    setPresence("not present")
	setFanMode("auto")
	setHysteresis(0)
	setDisplay("all")
	setIcon("idle-icon.svg")
	updated()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	log.warn "description logging is: ${txtEnable == true}"
	if (logEnable) runIn(1800,logsOff)

	setDisplay("all")
	initialize()
}

def initialize() {
	if (!circulateHeat) {setCirculatorState("on")}     				// for updating state after a setting change
	circulatorOnWithTemp(device.currentValue("temperature"))	

	//if (circulateVent )
}	

def parse(String description) { noCommands("parse") }

// intialize thermostat attributes for the sake of the thermostat tile controls
def setThermostatAttributes() {
	sendEvent(name: "thermostatOperatingState", value: "off")
	sendEvent(name: "supportedThermostatFanModes", value: '["low","medium","high","auto","disable"]')
	sendEvent(name: "supportedThermostatModes", value: '["heat","cool","idle"]')
	sendEvent(name: "thermostatFanMode", value: "auto")
	sendEvent(name: "thermostatMode", value: "idle")
}
def setThermostatMode(mode) {
	sendEvent(name: "thermostatMode", value: mode)
	runIn(10,setThermostatModeBack)
}
def setThermostatModeBack() {
	def mode = device.currentValue("operatingState")
	def tMode = "idle"
	if (mode == "venting") {tMode = "cool"}
	if (mode == "heating") {tMode = "heat"}
	sendEvent(name: "thermostatMode", value: tMode)
}
def setThermostatFanMode(mode) {
	if (mode == "disable") {
		setVent("disabled")	
		runInMillis(100, setThermoFanModeDisabled)
	}   
	if (mode == "low" || mode == "medium" || mode == "high") {
		setVent("enabled")
		setFanMode("manual")
		setFanSpeed(mode)
	}
	if (mode == "auto") {
		setVent("enabled")
		setFanMode(mode)
	}

	setMessage("Vent Fan Mode set to ${mode}")
	sendEvent(name: "thermostatFanMode", value: mode)
	setDisplay("status")
}

// for change to the thermostat mode to display "disabled" instead of "disable", I wait a bit and call this after it is changed
def setThermoFanModeDisabled() {
	sendEvent(name: "thermostatFanMode", value: "disabled")
}

// Enabled toggle buttons and heat pad setpoint buttons
def push(button) {
	logDebug("button pushed is ${button}")
	if (button == "1") {
		if (device.currentValue("heater") == "enabled") {setHeater("disabled")}
		else if (device.currentValue("heater") == "disabled") {setHeater("enabled")}
	}
	else if (button == "2") {
		if (device.currentValue("heatPads") == "enabled") {setHeatPads("disabled")}
		else if (device.currentValue("heatPads") == "disabled") {setHeatPads("enabled")}
	}
	else if (button == "3") {
		if (device.currentValue("vent") == "enabled") {setVent("disabled");sendEvent(name: "thermostatFanMode", value: "disabled")}
		else if (device.currentValue("vent") == "disabled") {
			setVent("enabled");
			if (device.currentValue("fanMode") == "auto") {sendEvent(name: "thermostatFanMode", value: "auto")}}
		else {sendEvent(name: "thermostatFanMode", value: device.currentValue("fanSpeed"))}
		setDisplay("status")
	}
	else if (button == "4") {
		if (device.currentValue("circulator") == "enabled") {setCirculator("disabled"); setCirculatorState("off")}
		else if (device.currentValue("circulator") == "disabled") {setCirculator("enabled"); setCirculatorState("on")}
	}	
	else if (button == "5") {
		logDebug("Heat pad temp down")
		def temp = device.currentValue("heatPadSetpoint").toInteger()
		def newTemp = (temp - 1).toString()
		setHeatPadSetpoint(newTemp)
	}
	else if (button == "6") {
		logDebug("Heat pad temp up")
		def temp = device.currentValue("heatPadSetpoint").toInteger()
		def newTemp = (temp + 1).toString()
		setHeatPadSetpoint(newTemp)
	}
	else if (button == "7") {
		logDebug("Circulate with Vent button pushed")
		def circVent = circulateVent
		if (circVent) {device.updateSetting("circulateVent",[value: false,type:"bool"])}
		else {device.updateSetting("circulateVent",[value: true,type:"bool"])}
		setDisplay("status")
	}
	else if (button == "8") {
		logDebug("Circulate with Heat button pushed")
		def circHeat = circulateHeat
		if (circHeat) {device.updateSetting("circulateHeat",[value: false,type:"bool"])}
		else {device.updateSetting("circulateHeat",[value: true,type:"bool"])}
		setDisplay("status")
	}
	runIn(1,manageCycle)
}

// if using google charts, pulls up the chart in an iFrame over dashboard
def setChartFrame(text, src, height, width, left, top) {

	def button = getChartButton(text, src, height, width, left, top) 
	sendEvent(name: "chartLauncher", value: button)
}

// if using google charts, pulls up the 2md chart in an iFrame over dashboard
def setChartFrame2(text, src, height, width, left, top) {

	def button = getChartButton(text, src, height, width, left, top) 
	sendEvent(name: "chartLauncher2", value: button)
}

// create the button attribute for the dashboard to launch an iFrame
def getChartButton(text, src, height, width, left, top) {

	def id = "${device.displayName.replaceAll('\\s','')}${text.replaceAll('\\s','')}"
	def button = "<button style='height:100%;width:100%;' onclick='document.getElementById(`${id}`).style.display"+
	"=`block`;'>${text}</button><div id=${id} class='modal' style='display:none;position:fixed;"+
	"top:${top};left:${left};width:${width};height:${height};background-color:rgba(225,225,225.1); z-index:990 !important;'>"+
	"<button onclick=document.getElementById('${id}').style.display='none'; style='float:right;font-size:20px;color:black;margin-right:5px;'> [X] </button>"+
	"<button onclick=document.getElementById('${id}-iframe').src='${src}'; style='float:center;font-size:18px;color:black;'>"+text+"</button>"+
	"<button onclick=document.getElementById('${id}-iframe').src='${src}'; style='float:left;font-size:18px;color:black;margin-left:5px;'>Refresh</button>"+
	"<iframe id='${id}-iframe' src='${src}' style='height:100%;width:100%;border:none;'></iframe></div>"; 

	return button
}

// update driver states and settings using a variable string tile with a map input
def setVariable(value) {
    logDebug("setVariable was called with ${value}")

    if (value == " ") {sendEvent(name: "variable", value: "NO DATA SENT",); runIn(3,resetVariableValue)}  
    else {
        try {
		def map = "["+value+"]"
        Map settingsMap = evaluate(map)

        // update attribute states from abbreviated map
		if (value.contains("csp")) {setCoolingSetpoint(settingsMap.csp.toString()); setDisplay("setpoints")}	// Cooling setpoint
        if (value.contains("hsp")) {setHeatingSetpoint(settingsMap.hsp.toString())}; setDisplay("setpoints")	// Heating setpoint
        if (value.contains("hys")) {setHysteresis(settingsMap.hys.toString())}		// hysteresis
		if (value.contains("hps")) {setHeatPadSetpoint(settingsMap.hps.toString()); setDisplay("pads")}	// heating pad setpoint
		if (value.contains("cot")) {device.updateSetting("circulatorOnTemp",[value: settingsMap.cot.toString(),type:"enum"]); setDisplay("status")} // circulator on temp
		if (value.contains("ath")) {device.updateSetting("fanAutoThreshold",[value: settingsMap.ath.toString(),type:"enum"]); setDisplay("status")} // vent auto threshold

        // update using names in map intead of using the abbreviations above
        if (value.contains("coolsetpoint")) {setCoolingSetpoint(settingsMap.coolsetpoint.toString()); setDisplay("setpoints")}
        if (value.contains("heatsetpoint")) {setHeatingSetpoint(settingsMap.heatsetpoint.toString()); setDisplay("setpoints")}
        if (value.contains("hysteresis")) {setHysteresis(settingsMap.hysteresis.toString())}
		if (value.contains("padsetpoint")) {setHeatPadSetpoint(settingsMap.padsetpoint.toString()); setDisplay("pads")}
		if (value.contains("circontemp")) {device.updateSetting("circulatorOnTemp",[value: settingsMap.circontemp.toString(),type:"enum"]); setDisplay("status")}
		if (value.contains("autothreshold")) {device.updateSetting("fanAutoThreshold",[value: settingsMap.autothreshold.toString(),type:"enum"]); setDisplay("status")}
        runIn(1,resetVariableValue)

        } catch (Exception ex) {
            sendEvent(name: "variable", value: "MAP ERROR", )
			runIn(3,resetVariableValue)
            logDebug("Error updating from variable Map: ${ex}")
        }
    }
}

// default variable value is a space for command tile
def resetVariableValue() {
    sendEvent(name: "variable", value: " ", )
}

// *** set the type of display after 100ms to allow time for states to update first ***
def setDisplay(type) {
	logDebug "setDisplay(${type}) was called"                             

	if (type == "enabled"  || type == "all") {runInMillis(100, setEnabledDisplay)}
	if (type == "setpoints" || type == "all") {runInMillis(100, setSetpointsDisplay)}
	if (type == "status" || type == "all") {runInMillis(100, setStatusDisplay)}
	if (type == "temps" || type == "all") {runInMillis(100, setTempsDisplay)}
	if (type == "pads" || type == "all") {runInMillis(100, setPadsDisplay)}
}
def setEnabledDisplay() {
	def heaterCSS = "<font style='font-weight: bold;color:#bd3824'>"+device.currentValue("heater")+"</font>"
	if (device.currentValue("heater") == "enabled") {heaterCSS = "<font style='font-weight: bold;color:#53c24e'>"+device.currentValue("heater")+"</font>"}
	def heatPadsCSS = "<font style='font-weight: bold;color:#bd3824'>"+device.currentValue("heatPads")+"</font>"
	if (device.currentValue("heatPads") == "enabled") {heatPadsCSS = "<font style='font-weight: bold;color:#53c24e'>"+device.currentValue("heatPads")+"</font>"}
	def ventCSS = "<font style='font-weight: bold;color:#bd3824'>"+device.currentValue("vent")+"</font>"
	if (device.currentValue("vent") == "enabled") {ventCSS = "<font style='font-weight: bold; color:#53c24e'>"+device.currentValue("vent")+"</font>"}
	def circCSS = "<font style='font-weight: bold;color:#bd3824'>"+device.currentValue("circulator")+"</font>"
	if (device.currentValue("circulator") == "enabled") {circCSS = "<font style='font-weight: bold; color:#53c24e'>"+device.currentValue("circulator")+"</font>"}	

	String displayEnabled = "<p style='line-height:1.4;font-size:"+settings?.enabledFontSize+"px;text-align:left;margin-left:25px;'>"+
		"Vent: &nbsp;&nbsp;&nbsp;&nbsp;&emsp;&emsp;"+ventCSS+"<br>"+
		"Heater: &nbsp;&nbsp;&nbsp;&nbsp;&emsp;"+heaterCSS+"<br>"+
		"Heat Pads: &nbsp;&nbsp;"+heatPadsCSS+"<br>"+
		"Circulator: &emsp;"+circCSS+"</p>"
	sendEvent(name: "displayEnabled", value: displayEnabled, descriptionText: getDescriptionText("display set to ${displayEnabled}"))
}
def setStatusDisplay() {
	def autoFan = device.currentValue("fanMode")

	String displayStatus = "<p style='line-height:1.4;font-size:"+settings?.statusFontSize+"px;text-align:left;margin-left:25px;'>"+
		"Vent Speed Control: "+autoFan+"<br>"+
		"Vent Fan Speed: &nbsp;&nbsp;&emsp;"+device.currentValue("fanSpeed")+"<br><br>"+

		"Circulate with Vent: &nbsp;"+settings?.circulateVent+"<br>"+
		"Circulate Heat only: &nbsp;"+settings?.circulateHeat+"<br>"+
		"Circulate On Temp: &nbsp;&nbsp;"+settings?.circulatorOnTemp+"°F<br><br>"+

		//"Med Speed Diff: "+settings?.fanMediumThreshold+"°F<br>"+
		"Fan Auto Threshold: "+settings?.fanAutoThreshold+"°F"+
		"</p>"
	sendEvent(name: "displayStatus", value: displayStatus, descriptionText: getDescriptionText("displayStatus set to ${displayStatus}"))
}
def setTempsDisplay() {
	def trend = ""
	if (device.currentValue("tempTrend") == "rising") {trend = "↑"} 
	if (device.currentValue("tempTrend") == "falling") {trend = "↓"}
	def outTrend = ""
	if (device.currentValue("outTempTrend") == "rising") {outTrend = "↑"} 
	if (device.currentValue("outTempTrend") == "falling") {outTrend = "↓"}		

	String displayTemp = "<p style='font-size:"+settings?.tempFontSize+"px;text-align:center;'>"+
		(device.currentValue("temperature"))+"°"+trend+"</p>"
	sendEvent(name: "displayTemp", value: displayTemp, descriptionText: getDescriptionText("displayTemp set to ${displayTemp}"))	

	String displayTemps = "<p style='line-height:1.2;font-size:"+settings?.tempsFontSize+"px;text-align:left;margin-left:25px;'>"+
	"<br>Temperature: "+(device.currentValue("temperature"))+"°F "+trend+"<br>"+
	"Humidity: &emsp; &nbsp;&nbsp;"+(device.currentValue("humidity"))+"%<br><br>"+

	"Outside Temp: &nbsp"+(device.currentValue("outsideTemp"))+"°F "+outTrend+"<br>"+
	"Out Humidity: &nbsp&nbsp"+(device.currentValue("outsideHumidity"))+"%<br><br>"+
	"</p>"
	sendEvent(name: "displayTemps", value: displayTemps, descriptionText: getDescriptionText("displayTemps set to ${displayTemps}"))
}
def setSetpointsDisplay() {
	String displaySetpoints = "<p style='line-height:1.75;font-size:"+settings?.setpointsFontSize+"px;text-align:center;margin-top:4px'>"+
		"&nbsp;&nbsp;Heat: "+(device.currentValue("heatingSetpoint"))+"°F &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+		// <br>  Setpoint
		"Cool: "+(device.currentValue("coolingSetpoint"))+"°F"+
		"</p>" 
	sendEvent(name: "displaySetpoints", value: displaySetpoints, descriptionText: getDescriptionText("displaySetpoints set to ${displaySetpoints}"))	
}
def setPadsDisplay() {
	String heatPadSet = "<p style='line-height:1.4;font-size:"+settings?.padsSetFontSize+"px;text-align:center;'>"+
		" &nbsp;&nbsp;"+(device.currentValue("heatPadSetpoint"))+"°F<br>"+
		"</p>"
	sendEvent(name: "displayPadSet", value: heatPadSet, descriptionText: getDescriptionText("heatPadSet set to ${heatPadSet}"))	
}

// **** Update attribute States ****
def setHeatPadState(hpstate) {
	logDebug "setHeatPadState(${hpstate}) was called"
	setMessage("Heat Pads set to ${hpstate}")
    sendEvent(name: "heatPadState", value: hpstate, descriptionText: getDescriptionText("heatPadState set to ${hpstate}")) 
    parent.heatPadsStateController(hpstate)  
	setDisplay("pads")
	runInMillis(100, setStatusMessage)
}
def setCirculatorState(cstate) {
	logDebug "setCirculatorState(${cstate}) was called"
    sendEvent(name: "circulatorState", value: cstate, descriptionText: getDescriptionText("circulatorState set to ${cstate}")) 
    parent.circulatorStateController(cstate)
	runInMillis(100, setStatusMessage)
}
def setFanState(fstate) {
	logDebug "setFanState(${fstate}) was called"
    sendEvent(name: "fanState", value: fstate, descriptionText: getDescriptionText("fanState set to ${fstate}")) 
    if (fstate == "enabled") setThermostatFanMode(device.currentValue("fanSpeed"))
    if (fstate == "disabled") setThermostatFanMode("disabled")
}

// ***** Enable Disable components *******
// Enable/Disable Heat pads
def setHeatPads(value) {
	logDebug "setHeatPads(${value}) was called"
    sendEvent(name: "heatPads", value: value, descriptionText: getDescriptionText("heatPads set to ${value}"))   
	setDisplay("enabled")
	setMessage("Heat pads set to ${value}")
	runInMillis(100, setStatusMessage)
}
// Enable/Disable heater
def setHeater(value) {
	logDebug "setHeater(${value}) was called"
	setMessage("Heater set to ${value}")
    sendEvent(name: "heater", value: value, descriptionText: getDescriptionText("heater set to ${value}"))
	setDisplay("enabled")
	runInMillis(100, setStatusMessage)
}
// Enable/Disable cooling vent
def setVent(value) {
	logDebug "setVent(${value}) was called"
	setMessage("Vent set to ${value}")
    sendEvent(name: "vent", value: value, descriptionText: getDescriptionText("vent set to ${value}"))   
	if (value == "disabled") {setFanMode("disabled")}
	if (value == "enabled") {
		if (device.currentValue("fanMode") == manual) {setFanMode(device.currentValue("fanSpeed"))}
		else {setFanMode(device.currentValue("fanMode"))}
	}
	setDisplay("enabled")
	runInMillis(100, setStatusMessage)
}
// Enable/Disable circulator fan
def setCirculator(value) {
	logDebug "setCirculator(${value}) was called"
	setMessage("Circulator set to ${value}")
    sendEvent(name: "circulator", value: value, descriptionText: getDescriptionText("circulator set to ${value}"))   
	setDisplay("enabled")
	runInMillis(100, setStatusMessage)
}

// ********************* Manage Cycle on a temperature change ***********************
def manageCycle() {

	def heatingSetpoint = (device.currentValue("heatingSetpoint")).toBigDecimal()
	def coolingSetpoint = (device.currentValue("coolingSetpoint")).toBigDecimal()
	def heatPadSetpoint = (device.currentValue("heatPadSetpoint")).toBigDecimal()
	def hysteresis = (device.currentValue("hysteresis")).toBigDecimal()

	def temperature = (device.currentValue("temperature")).toBigDecimal()
	def ventSetpoint = coolingSetpoint + hysteresis
	def heatSetpoint = heatingSetpoint - hysteresis

	def operatingState = device.currentValue("operatingState")
	def fanMode = device.currentValue("fanMode")
	def trend = device.currentValue("tempTrend")
    
	// venting
	def onVent = (temperature >= ventSetpoint && device.currentValue("vent") == "enabled")
	logDebug("onVent is ${onVent}")

    // heating
    def onHeat = (temperature <= heatSetpoint && device.currentValue("heater") == "enabled") 
	logDebug "onHeat is ${onHeat}"

	// check Operating State
	def newState = operatingState
    if (onVent && operatingState != "venting") {
		newState = "venting"	
        logDebug "venting on"
		trend = "steady"   // so fan won't jump a speed at start
		setIcon("greenhouse-fan-icon.svg")	
		sendEvent(name: "thermostatMode", value: "cool")
		sendEvent(name: "fanState", value: "on")

    }
    else if (onHeat && operatingState !="heating") {
        newState = "heating"
    	logDebug "heating on"
		setIcon("heater-on-icon.svg")	
		sendEvent(name: "thermostatMode", value: "heat")
		sendEvent(name: "heaterState", value: "on")
    }		
    else if (!onVent && !onHeat && operatingState !="idle") {
        newState = "idle"
    	logDebug "venting and heating off"
		setIcon("idle-icon.svg")		
		sendEvent(name: "thermostatMode", value: "idle")
		sendEvent(name: "heaterState", value: "off")
		sendEvent(name: "fanState", value: "off")
    }

	// heat pads
    def onPad = (temperature <= heatPadSetpoint && device.currentValue("heatPads") == "enabled")
	logDebug "onPad is ${onPad}"

	if (onPad && device.currentValue("heatPadState") == "off") {setHeatPadState("on"); setPresence("present")}
	if (!onPad && device.currentValue("heatPadState") == "on") {setHeatPadState("off"); setPresence("not present")}
 
	// update operatingState if it changed
	if (newState != operatingState) {setOperatingState(newState)}
	logDebug "Finshed manageCycle()"
}

// Set Operating State
def setOperatingState(opState) {
	logDebug "setOperatingState(${opState}) was called"
	if (opState == "venting") {sendEvent(name: "thermostatOperatingState", value: "cooling")}
	else {sendEvent(name: "thermostatOperatingState", value: opState)}   
	sendEvent(name: "operatingState", value: opState, descriptionText: getDescriptionText("operatingState set to ${opState}"))
    parent.operatingStateController(opState)

	updateCirculatorState(opState)

	state.lastOperatingState = opState
	runIn(1,setStatusMessage)
}

// update circulator state based on prefences
def updateCirculatorState(opState) {
	logDebug("setting circulator prefs")

	def circState = device.currentValue("circulatorState")
	logDebug("circState is ${circState} and circEnabled is ${circEnabled}")
	//def circulateVent = settings?.circulateVent
	//def circulateHeat = settings?.circulateHeat
	def last = state?.lastHeatCoolState

	if (device.currentValue("circulator") == "enabled") {
		if (opState == "heating" && circulateHeat && circState == "off") {logDebug("Setting circulator State on"); setCirculatorState("on")}
		else if (opState == "idle" && circulateHeat && last == "heating" && circState == "on") {logDebug("Setting circulator State off for idle");setCirculatorState("off")}
		else if (opState == "idle" && last == "venting" && circState == "off") {logDebug("Setting circulator State on"); setCirculatorState("on")}
		else if (!circulateVent && opState == "venting" && circState == "on") {logDebug("Setting circulator State off"); setCirculatorState("off")}
		else if (circulateVent && opState == "venting" && circState == "off") {logDebug("Setting circulator State on"); setCirculatorState("on")}
	} else {logDebug("Setting Circulator State off on else"); setCirculatorState("off")}
	
	if (opState == "heating" || opState == "venting") {state.lastHeatCoolState = opState;}  // last state before idle
}

// ****** methods to set states from Greenhouse App ************
def setHumidity(humidity) {
	logDebug "setHumidity(${humidity}) was called"
    sendEvent(name: "humidity", value: humidity, unit: "%", descriptionText: getDescriptionText("humidity set to ${humidity}%"))
	setDisplay("temps")
}

def setOutsideHumidity(setpoint) {
	logDebug "setOutsideHumidity(${setpoint}) was called"
    sendEvent(name: "outsideHumidity", value: setpoint, descriptionText: getDescriptionText("outsideHumidity set to ${setpoint}"))
	setDisplay("temps")
}

// Set Temperature and apply the circulator on with temp if it applied.  All triggers an auto fan speed check when venting 
def setTemperature(temp) {
	logDebug "setTemperature(${temp}) was called"
	def current = device.currentValue("temperature").toBigDecimal()
	def trend = "steady"
	if (temp > current) trend = "rising"
	else if (temp < current) {trend = "falling"}	
    sendEvent(name: "temperature", value: temp, descriptionText: getDescriptionText("temperature set to ${temp}"))
	sendEvent(name: "tempTrend", value: trend, descriptionText: getDescriptionText("tempTrend set to ${trend}"))

	setDisplay("temps")
	if (device.currentValue("fanMode") == "auto") {runIn(1,checkAutoFanSpeed)}
	circulatorOnWithTemp(temp)
	runIn(1, manageCycle)
	runIn(1,setStatusMessage)
}

def setOutsideTemp(temp) {
	logDebug "setOutsideTemp(${temp}) was called"
	def current = device.currentValue("outsideTemp").toBigDecimal()
	def trend = "steady"
	if (temp > current) trend = "rising"	
	else if (temp < current) {trend = "falling"}	
    sendEvent(name: "outsideTemp", value: temp, descriptionText: getDescriptionText("outsideTemp set to ${temp}"))
	sendEvent(name: "outTempTrend", value: trend, descriptionText: getDescriptionText("outTempTrend set to ${trend}"))
    runIn(1, manageCycle)
	setDisplay("temps")
}

// Check and update vent fan speed when temperature changes, and auto fan is enabled
def checkAutoFanSpeed() {
	logDebug("checkAutoFanSpeed() Called")

	if (device.currentValue("operatingState") == "venting") {
		logDebug("Greenhouse is venting - checking auto speed")
		def temp = device.currentValue("temperature")
		def coolingSetpoint = Double.parseDouble(device.currentValue("coolingSetpoint"))
		def diff = temp - coolingSetpoint
		logDebug("diff is ${diff}")

		def autoDiff =  Double.parseDouble(settings?.fanAutoThreshold)
		logDebug("autoDiff is ${autoDiff}")
		def speed = device.currentValue("fanSpeed")
		def trend = device.currentValue("tempTrend")
		
		// change speeds if under threshold and not on high or low based on trend
		if (diff <= autoDiff) {
			if (trend == "rising" && speed != "high") {
				logDebug("Trend it rising under auto threshold and speed is ${speed}")
				if (speed == "low") {setFanSpeed("medium"); setDisplay("status"); logDebug("fan speed set to medium")}
				else if (speed == "medium") {setFanSpeed("high"); setDisplay("status"); logDebug("fan speed set to high")}
			}
			else if (trend == "falling" && speed != "low") {
				logDebug("Trend it falling under auto threshold and speed is ${speed}")
				if (speed == "high" && diff >= highDiff) {setFanSpeed("medium"); setDisplay("status"); logDebug("fan speed set to medium")}
				else if (speed == "medium") {setFanSpeed("low"); setDisplay("status"); logDebug("fan speed set to low")}
			} 
		}
	} else {logDebug("No auto speed adjust - Not venting")}
}

// turn the circulator fan on or off when idle based on the temp setpoint in settings 
def circulatorOnWithTemp(temp) {
	logDebug("circulatorOnwithTemp called with ${temp}")
	def set = settings?.circulatorOnTemp.toBigDecimal()
	logDebug("temp > set is ${temp > set}")

	if (device.currentValue("operatingState") == "idle"){
		if (temp > set && device.currentValue("circulator") == "enabled" && device.currentValue("circulatorState") == "off") {	
			logDebug("Turning Circulator On")
			setCirculatorState("on")
		}
		else if (temp <= set && device.currentValue("circulator") == "enabled" && device.currentValue("circulatorState") == "on"){
			if (circulateHeat) {
				logDebug("Turning Circulator Off, circulateHeat is ${circulateHeat}")
				setCirculatorState("off")
			} 
		} else {logDebug("No match for circulator On/Off")}
	}
}

def setHeatingSetpoint(temp) {
	logDebug "setHeatingSetpoint(${temp}) was called"
    sendEvent(name: "heatingSetpoint", value: temp, descriptionText: getDescriptionText("heatingSetpoint set to ${temp}"))
	sendEvent(name: "thermostatHeatingSetpoint", value: temp)
	setDisplay("setpoints")
    runIn(1, manageCycle)
}

def setCoolingSetpoint(temp) {
	logDebug "setCoolingSetpoint(${temp}) was called"
    sendEvent(name: "coolingSetpoint", value: temp, descriptionText: getDescriptionText("coolingSetpoint set to ${temp}"))
	sendEvent(name: "thermostatCoolingSetpoint", value: temp)
    runIn(1, manageCycle)
	setDisplay("setpoints")
}

def setHeatPadSetpoint(temp) {
	logDebug "setHeatPadSetpoint(${temp}) was called"
    sendEvent(name: "heatPadSetpoint", value: temp, descriptionText: getDescriptionText("heatPadSetpoint set to ${temp}"))
	setDisplay("pads")
    runIn(1, manageCycle)	
}

def setPresence(state) {
	logDebug "setPresence(${state}) was called"
    sendEvent(name: "presence", value: state, descriptionText: getDescriptionText("presence set to ${state}"))
}

def setHysteresis(value) {
	logDebug "setHysteresis(${value}) was called"
    sendEvent(name: "hysteresis", value: value, descriptionText: getDescriptionText("hysteresis set to ${value}"))
}

def setFanSpeed(speed) {
	logDebug "setFanSpeed(${speed}) was called"
	setMessage("Vent Speed set to ${speed}")
    sendEvent(name: "fanSpeed", value: speed, descriptionText: getDescriptionText("fanSpeed set to ${speed}"))
    parent.fanSpeedController(speed)
	setDisplay("status")
	runIn(5,setStatusMessage)   
}

def setFanMode(mode) {
	logDebug "setFanMode(${mode}) was called"
    sendEvent(name: "fanMode", value: mode, descriptionText: getDescriptionText("fanMode set to ${mode}"))
	sendEvent(name: "thermostatFanMode", value: mode)
	setDisplay("status")
}

def setIcon(img) {
	def current = device.currentValue("iconFile")
    logDebug "setIcon(${img}) was called"
	if (current != img) {
		sendEvent(name: "icon", value: "<img class='icon' src='${settings?.iconPath}${img}' />")
		sendEvent(name: "iconFile", value: img)
	}
}

// sets the status message that is normally displayed
def setStatusMessage() {
	def circ = device.currentValue("circulatorState") == "on"
	def opState = device.currentValue("operatingState")
	def pads = device.currentValue("heatPadState") == "on"
	def message = "Greenhouse is "+opState

	if (circ) {message = message + " and circulating"}
	if (pads) {message = message + " with heat pads on"}
	setMessage(message)
}

// Set the message attribute
def setMessage(message) {
	logDebug "setMessage(${message}) was called"

	def todayDate = new Date()
    def parsedDate = todayDate.format("hh:mm a")
    logDebug("${parsedDate}")
	
	def send = parsedDate + ": " + device.currentValue("temperature") + "°F - "+ message
    sendEvent(name: "message", value: send, descriptionText: getDescriptionText("message set to ${send}"))
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}
