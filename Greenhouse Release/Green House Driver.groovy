/*
	Greehouse Driver - The device will be created as a child device by installing the Greenhouse Controller App

	Copyright 2025 -> C. Burgess  

	This driver works with a companion app, "GreenHouse Fan Controller App", to control a greenHouse fan to vent for over temperature, a heater to heat when under temperature,
	heating pads for seedlings when under temperature, and a circulator fan.  All components can be enabled/disabled. 

	The presence attribute is to provide a "Heat pads On" background color for the Heat Pad Setpoint tile, when heat pads are activated.  "present" when on, "not present" when off 

	To use the thermostat template for dashboard controls on the dashboard (using thermostat tile):
	Mode: Mode changes automatically. Setpoint can be changed when greenhouse operatingState is in that mode.  When idle, just change mode to what setpoint you want to change, 
		and it will revert to "idle" (or current state) after ten seconds.  Changing the mode on the tile for a setpoint change does not change the operating mode for the greenhouse. 
	FanMode: "auto": sets vent fan to use auto speed.  "disable": disables the vent fan   "low", "medium", or "high": sets vent fan to manual, and sets the manual speed

	To use the buttons for dashboard controls (button tiles with push):
	1- Heater enable/disable
	2- Heat Pads enable/disable
	3- Vent Fan enable/disable
	4- Circulator enable/disable
	5- Heat Pads setpoint lower (layer on top of heat pad setpoint display tile)
	6- Heat Pads setpoint higher (layer on top of heat pad setpoint display tile)

	v. 1.0 - 3/16/25  - Inital code to create a Green House controller using temperature setpoints to turn on a vent fan or turn on a heater.
	v. 1.2 - 3/19/25  - Temp setpoint changed to seperate heating and cooling setpoints. If heat and cool setpoints are the same, add at least 1 degree hysteresis. 
	v. 1.3 - 3/20/25  - Added heating pad control with its own temp setpoint, to turn off the seedling heating pads during the day when it is warm. Also added an iFrame attribute
						to pull up a URL (google chart)
	v. 1.4 - 3/21/25  - Added the ability to disable vent, heat, and pads.  Added a button controller for dashboards, to disable individual components, and a display for what's 
						enabled. Pushed 1 is heat, pushed 2 is heat pads, push 3 is ventalator. 
	v. 1.5 - 3/25/25  - Added a circulator fan.  It runs if enabled but turns off if the vent fan comes on. button push 4 is enables/disables the circulator fan. 
	v. 1.6 - 3/26/25  - Added thermostat attributes and commands to use a thermostat tile for control of fan, setting cooling setpoint, and setting heating setpoint. 
						It also displays state. 
					    Added buttons 5 and 6, to set the heaterPads setpoint.  Button 5 is down temp, and button 6 is up temp for the heating pad setpoint.
	v. 2.0 - 4/3/25   - Turned this driver into a child device, created by Greenhouse Controller app.  Now driver calls methods in parent app instead of the app using subscriptions
						to monitor the driver states.

color picker with VSCode:  rgba(21,124,207,0.55)*/

import java.text.SimpleDateFormat

metadata {
	definition (
		name: "Greenhouse Driver",
		namespace: "Hubitat",
		author: "C.Burgess"
	) {
        //capability "Light"
		capability "Actuator"
        capability "Presence Sensor"  				// overidden presence as "cooling", "heating" or "none" (for custom dashboard background colors, set manually in Dahsboard Layout)
		capability "Variable"
		capability "Pushable Button"
		capability "Temperature Measurement"
		capability "RelativeHumidityMeasurement"

		// attributes
		attribute "operatingState", "ENUM"  		// ["venting", "idle", "heating"]
		attribute "heatPadState", "ENUM"			// on, off
		attribute "circulatorState", "ENUM"			// on, off
		attribute "fanState", "ENUM"
		attribute "humidity", "ENUM"	    		// set from app
		attribute "outsideHumidity", "ENUM"			// set from app
		attribute "hysteresis", "NUMBER"			// diff between heating and venting from setpoint when heat and cool setpoints are the same
		attribute "displayEnabled", "STRING"       	// attribute for dashboard status tile showing enabled states 
        attribute "displaySetpoints", "STRING"		// attribute for dashboard status tile showing setpoints    
		attribute "displayStatus", "STRING"			// attribute for dashboard status tile showing all status and settings
		attribute "displayTemp", "STRING"
		attribute "displayTemps", "STRING"
		attribute "displayPadSet", "String"
        attribute "temperature", "NUMBER"        	// set from app
		attribute "tempTrend", "ENUM"
		attribute "outTempTrend", "ENUM"
        attribute "outsideTemp", "NUMBER"           // set from app
        attribute "heatingSetpoint", "ENUM"      
        attribute "coolingSetpoint", "ENUM"      
		attribute "heatPadSetpoint", "ENUM"
		attribute "fanSpeed", "ENUM"   			    // low, medium, high - manually set or auto set fan speed, based on pref setting
		attribute "fanMode", "ENUM"	
        attribute "presence", "ENUM"                // venting or heating or none
		attribute "icon", "STRING"                  // status icon attribute to be use on a dashboard tile (heating (heater), cooling (fan), or idle (OK checkmark))
		
		attribute "iconFile", "STRING"
		attribute "message", "STRING"
		attribute "variable", "STRING"				// to send commands to the driver from a dashbaord variable tile
		attribute "heatPads", "String"
		attribute "heater", "String"
		attribute "vent", "String"
		attribute "circulator", "String"
		attribute "chartLauncher", "text"	
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
		command "setChartFrame", [[type:"TEXT",description:"Set Button Text", defaultValue:"Chart"],[type:"TEXT",description:"Set URL", defaultValue: "https://docs.google.com/spreadsheets/d/e/2PACX-1vSBmvmekq-AijFpKatQfQWHW6yhs4wdpH97cxfi4ySl9_4SY1ARiO8JPY0DOBZl8yolMsw8rE-6PkN_/pubchart?oid=1718999599&format=interactive"],[type:"TEXT",description:"Set Height px", defaultValue:"600px"],[type:"TEXT",description:"Set Width px", defaultValue:"1230px"]
		,[type:"TEXT",description:"Set Left Pos %", defaultValue:"2%"],[type:"TEXT",description:"Set Top Pos %", defaultValue:"5%"]]
}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
		input( name: "circulateVent", type:"bool", title: "Circulate when Venting (do not turn off when venting)", defaultValue:false)
		input( name: "circulateHeat", type:"bool", title: "Circulate Only when Heating (turn off on idle)",defaultValue: false)
		input( name: "circulatorOnTemp", type:"enum", title: "Temp to keep circulator on when idle", defaultValue:"60", options:[50.0:"50",55.0:"55",60.0:"60",65.0:"65",70.0:"70"])
		input( name: "fanMediumThreshold", type:"enum", title: "Vent Fan Medium Speed Difference", defaultValue:"3", options:[1.0:"1",2.0:"2",3.0:"3",4.0:"4",5.0:"5"])
		input( name: "fanHighThreshold", type:"enum", title: "Vent Fan High Speed Difference", defaultValue:"6", options:[2.0:"2",3.0:"3",4.0:"4",5.0:"5",6.0:"6",7.0:"7",8.0:"8",9.0:"9",10.0:"10"])
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
	if (state?.lastRunningMode == null) {                 
		state.lastRunningMode = device.currentValue("operatingState")
    }	
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
	if (mode == "venting") {tmode = "cool"}
	if (mode == "heating") {tmode = "heat"}
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

// to change mode to land on disabled instead of disable, wait a bit and call this
def setThermoFanModeDisabled() {
	sendEvent(name: "thermostatFanMode", value: "disabled")
}

// Toggle enabled buttons and heat pad setpoint buttons
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

	runIn(1,manageCycle)
}

// if using google charts, pulls up the chart in an iFrame over dashboard
def setChartFrame(text, src, height, width, left, top) {
	
	def chart = "<button style='height:100%;width:100%;' onclick='document.getElementById(`${device.displayName.replaceAll('\\s','')}`).style.display"+
	"=`block`;'>${text}</button><div id=${device.displayName.replaceAll('\\s','')} class='modal' style='display:none;position:fixed;"+
	"top:${top};left:${left};width:${width};height:${height};background-color:rgba(225,225,225.75); z-index:990 !important;'>"+
	"<button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none'; style='float:right;font-size:20px;margin-right:50px;'> Close </button>"+
	"<button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src='${src}'; style='float:center;font-size:20px;'>Refresh</button>"+
	"<iframe id='${device.displayName.replaceAll('\\s','')}-iframe' src='${src}' style='height:100%;width:100%;border:none;'></iframe></div>";
	sendEvent(name: "chartLauncher", value: chart)
}

// update driver states and run commands using the variable string tile with a map input
def setVariable(value) {
    logDebug("Variable value is ${value}")

    if (value == "[]" || value == "") {sendEvent(name: "variable", value: "[]",  isStateChange: false)}   
    else {
        try {
        Map settingsMap = evaluate(value)

        // update attribute states from abbreviated map - map numerical values do not need to be in quotes [CSP:70]
		if (value.contains("CSP")) {setCoolingSetpoint(settingsMap.CSP.toString())}	// Cooling setpoint
        if (value.contains("HSP")) {setHeatingSetpoint(settingsMap.HSP.toString())}	// Heating setpoint
        if (value.contains("HYS")) {setHysteresis(settingsMap.HYS.toString())}		// hysteresis
		if (value.contains("HPS")) {setHeatPadSetpoint(settingsMap.HPS.toString())}	// heating pad setpoint
		if (value.contains("SDP")) {setDisplay(settingsMap.SDP.toString())}

		// press command buttons (INIT CYCL DISP) - the command used must be in quotes in the map [CMD:"INIT"]
		if (value.contains("CMD")) {
			def cmd = settingsMap.CMD.toString()
			if (cmd == "INIT") {initialize()}
			if (cmd == "CYCL") {manageCycle()}
		}

        // update from names in map [coolSetpoint:70]
        if (value.contains("coolSetpoint")) {setCoolingSetpoint(settingsMap.coolSetpoint.toString())}
        if (value.contains("heatSetpoint")) {setHeatingSetpoint(settingsMap.heatSetpoint.toString())}
        if (value.contains("hysteresis")) {setHysteresis(settingsMap.hysteresis.toString())}
		if (value.contains("padSetpoint")) {setHeatPadSetpoint(settingsMap.padSetpoint.toString())}

        runIn(1,setSettingsMap)

        } catch (Exception ex) {
            sendEvent(name: "variable", value: "[ERROR]",  isStateChange: false)
            logDebug("Error updating from variable Map: ${ex}")
        }
    }
}

// default variable value is empty map (reset)
def setSettingsMap() {
    sendEvent(name: "variable", value: "[]",  isStateChange: false)
}

// *** set the display type after 100ms to allow time for states to update first ***
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

		"Circulate with Vent: &nbsp;&nbsp;"+settings?.circulateVent+"<br>"+
		"Circulate Heat only: &nbsp;&nbsp;"+settings?.circulateHeat+"<br>"+
		"Circulate On Temp: "+settings?.circulatorOnTemp+"°F<br><br>"+

		"Med Speed Diff: "+settings?.fanMediumThreshold+"°F<br>"+
		"High Speed Diff: "+settings?.fanHighThreshold+"°F"+
		"</p>"
	sendEvent(name: "displayStatus", value: displayStatus, descriptionText: getDescriptionText("displayStatus set to ${displayStatus}"))
	//setSetpointsDisplay()
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

// ********************* Manage Cycle ***********************
def manageCycle() {

	def outsideTemp = (device.currentValue("outsideTemp")).toBigDecimal()
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
	def onVent = (temperature >= ventSetpoint) //(temperature > outsideTemp) && 
	def offVent = (temperature < ventSetpoint)
	logDebug("onVent is ${onVent}")
	logDebug("offVent is ${offVent}")

    // heating
    def onHeat = (temperature <= heatSetpoint) // && (outsideTemp < temperatureSetpoint)
	def offHeat = (temperature > heatSetpoint) // || (outsideTemp > temperatureSetpoint)
	logDebug "onHeat is (${onHeat})"
	logDebug "offHeat is (${offHeat})"

	// heat pads
    def onPad = (temperature <= heatPadSetpoint) 
	def offPad = (temperature > heatPadSetpoint) 

	// Turn off components that are disabled
	if (device.currentValue("heatPads") == "false") {
		onPad = false	
		offPad = true	
	}
	if (device.currentValue("vent") == "false") {
		onVent = false
		offVent = true
	}
	if (device.currentValue("heater") == "false") {
		onHeat = false
		offHeat = true
	}

	def ventEnabled = device.currentValue("vent") == "enabled"
	def heatEnabled = device.currentValue("heater") == "enabled"
	def padsEnabled = device.currentValue("heatPads") == "enabled"
	def circEnabled = device.currentValue("circulator") == "enabled"

	// set Operating State
	def newState = operatingState
    if (onVent && operatingState != "venting" && ventEnabled) {
		newState = "venting"	
        logDebug "venting on"
		trend = "steady"   // so fan won't jump a speed at start
		setIcon("greenhouse-fan-icon.svg")	
		sendEvent(name: "thermostatMode", value: "cool")
		sendEvent(name: "fanState", value: "on")
    }
    else if (onHeat && operatingState !="heating" && heatEnabled) {
        newState = "heating"
    	logDebug "heating on"
		setIcon("heater-on-icon.svg")	
		sendEvent(name: "thermostatMode", value: "heat")
		sendEvent(name: "heaterState", value: "on")
    }		
    else if (offVent && offHeat && operatingState !="idle") {
        newState = "idle"
    	logDebug "venting and heating off"
		setIcon("idle-icon.svg")		
		sendEvent(name: "thermostatMode", value: "idle")
		sendEvent(name: "heaterState", value: "off")
		sendEvent(name: "fanState", value: "off")
    }

	// set heat pads
	if (onPad && padsEnabled && device.currentValue("heatPadState") == "off") {setHeatPadState("on"); setPresence("present")}
	if (offPad && device.currentValue("heatPadState") == "on") {setHeatPadState("off"); setPresence("not present")}
 
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

	// Apply Circulator State prefrences when operatingState Changes
	logDebug("setting circulator prefs")

	def circState = device.currentValue("circulatorState")
	def circEnabled = device.currentValue("circulator") == "enabled"
	logDebug("circState is ${circState} and circEnabled is ${circEnabled}")
	logDebug("Last opState was ${state?.lastOperatingState}")
	def circulateVent = settings?.circulateVent
	def circulateHeat = settings?.circulateHeat
	def last = state?.lastHeatCoolState

	if (circEnabled) {
		if (opState == "heating" && circulateHeat && circState == "off") {logDebug("Setting circulator State on"); setCirculatorState("on")}
		else if (opState == "idle" && circulateHeat && last == "heating" && circState == "on") {logDebug("Setting circulator State off");setCirculatorState("off")}
		else if (opState == "idle" && last == "cooling" && circState == "off") {logDebug("Setting circulator State on"); setCirculatorState("on")}
		else if (!settings?.circulateVent && opState == "venting" && circState == "on") {logDebug("Setting circulator State off"); setCirculatorState("off")}
		else if (settings?.circulateVent && opState == "venting" && circState == "off") {logDebug("Setting circulator State on"); setCirculatorState("on")}
	} else {logDebug("Setting Circulator State off on else"); setCirculatorState("off")}
	
	if (opState == "heating" || opState == "cooling") {state.lastHeatCoolState = opState;}
	state.lastOperatingState = opState
	runIn(1,setStatusMessage)
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
	if (device.currentValue("operatingState") == "venting" && device.currentValue("fanMode") == "auto") {runIn(1,setAutoFanSpeed)}
	circulatorOnWithTemp(temp)
	runIn(1, manageCycle)
}


// Auto set vent fan speed when temperature changes and auto fan enabled
def setAutoFanSpeed() {
	logDebug("setAutoFanSpeed() Called")

	def temp = device.currentValue("temperature").toBigDecimal()
	//def intTemp = Math.round(temp).toInteger()
	def coolingSetpoint = device.currentValue("coolingSetpoint").toBigDecimal()
	def diff = temp - coolingSetpoint

	//def diff = intTemp - coolingSetpoint
	def medDiff = settings?.fanMediumThreshold
	def highDiff = settings?.fanHighThreshold
	def speed = device.currentValue("fanSpeed")
	def trend = device.currentValue("tempTrend")

	if (trend == "rising") {
		if (speed == "low" && diff >= medDiff) {setFanSpeed("medium")}
		else if (speed == "medium" >= highDiff) {setFanSpeed("high")}
	}
	if (trend == "falling") {

		if (speed == "high" && diff <= highDiff) setFanSpeed("medium")
		else if (speed == "medium" && diff <= medDiff) setFanSpeed("low")
	}	
}

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

			logDebug("Turning Circulator Off")
			setCirculatorState("off")
		} else {logDebug("No match for circulator On/Off")}
	}
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
	setDisplay(status)
}

def setFanMode(mode) {
	logDebug "setFanMode(${mode}) was called"
    sendEvent(name: "fanMode", value: mode, descriptionText: getDescriptionText("fanMode set to ${mode}"))
	sendEvent(name: "thermostatFanMode", value: mode)
	setDisplay(status)
}

def setIcon(img) {
	def current = device.currentValue("iconFile")
    logDebug "setIcon(${img}) was called"
	if (current != img) {
		sendEvent(name: "icon", value: "<img class='icon' src='${settings?.iconPath}${img}' />")
		sendEvent(name: "iconFile", value: img)
	}
}

def setStatusMessage() {
	def circ = device.currentValue("circulatorState") == "on"
	def opState = device.currentValue("operatingState")
	def pads = device.currentValue("heatPadState") == "on"
	def message = "Greenhouse is "+opState

	if (circ) {message = message + " and circulating"}
	if (pads) {message = message + " with heat pads on"}
	setMessage(message)
}

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
