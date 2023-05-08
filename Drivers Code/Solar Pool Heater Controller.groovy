/*
	Solar Pool Heater Controller Driver

	Copyright 2023 -> C. Burgess

    This driver works with companion app Pool Heater Controller App to control the heater pump and update device attributes for manageCycle
    The app physically turns on or off the heater based on operatingState changing in this driver. 
    Rules: Turn on for illuminance level regardless of heater temp.  Always turn on for temperature, and turn off for temp only when illuminance is below threshold. 
*/

metadata {
	definition (
			name: "Solar Pool Heater Controller",
			namespace: "Hubitat",
			author: "Chris B"
	) {
		capability "Actuator"
        capability "Presence Sensor"
        capability "Lock"	
        capability "Light"
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"

        attribute "display", "STRING"
        attribute "display2", "STRING"
        attribute "displayAll", "STRING"
        attribute "illuminance", "ENUM"
        attribute "heaterTemp", "NUMBER"
        attribute "poolTemp", "NUMBER"
        attribute "operatingState","ENUM"
        attribute "onTemperature", "NUMBER"
        attribute "maxPoolTemp", "NUMBER"
        attribute "onIlluminance", "ENUM"
        attribute "presence", "ENUM"
        attribute "tempIcon", "STRING"
        attribute "presence", "ENUM"
        attribute "lock", "ENUM"
        attribute "icon", "STRING"                  // status icon attribute to be use on a dashboard tile
        
		// Commands needed to change internal attributes of virtual device.
        command "setDisplay"
        command "setOperatingState", [[name:"setOperatingState",type:"ENUM", description:"Set operatingState", constraints:["heating","idle"]]]
        command "setIlluminance", ["ENUM"]
        command "setHeaterTemp", ["NUMBER"]
        command "setPoolTemp", ["NUMBER"]
        command "setOnTemperature", ["NUMBER"]
        command "setMaxPoolTemp", ["NUMBER"]
        command "setOnIlluminance", ["ENUM"]
        command "setTempIcon", ["STRING"]
        command "manageCycle"
        command "setIcon", ["STRING"]
}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
        input( name: "cold", type: "number", description: "Temperature you consider to be cold water", title: "Set Water Comfort Cold", defaultValue: 65)
        input( name: "chilly", type: "number", description: "Temperature you consider to be chilly water", title: "Set Water Comfort Chilly", defaultValue: 69)
        input( name: "warm", type: "number", description: "Temperature you consider to be warm water", title: "Set Water Comfort Warm", defaultValue: 74)
        input( name: "hot", type: "number", description: "Temperature you consider to be hot water", title: "Set Water Comfort Hot", defaultValue: 80)
        input( name: "iconPath", type: "string", description: "Address Path to icons", title: "Set Icon Path", defaultValue: "https://cburges2.github.io/ecowitt.github.io/Dashboard%20Icons/")
    }
}

def installed() {
	log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])
    setOperatingState("idle")
    setIlluminance("0")
    setHeaterTemp(75)
    setOnTemperature(80)
    setOnIlluminance("20000")
	initialize()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	log.warn "description logging is: ${txtEnable == true}"
	if (logEnable) runIn(1800,logsOff)
    state?.lastRunningMode = "Running"
	initialize()
}

def initialize() {
	if (state?.lastRunningMode == null) {    
    
    }	
}

def parse(String description) { noCommands("parse") }

private eventSend(name,verb,value,unit = ""){
    String descriptionText = "${device.displayName} ${name} ${verb} ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    if (unit != "") sendEvent(name: name, value: value ,descriptionText: descriptionText, unit:unit)
    else  sendEvent(name: name, value: value ,descriptionText: descriptionText)   
}

def on() {
    String verb = (device.currentValue("switch") == "on") ? "is" : "was turned"
    eventSend("switch",verb,"on")  
}

def off() {
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    eventSend("switch",verb,"off")
}

def setLevel(value, rate = null) {
    if (value == null) return
    Integer level = limitIntegerRange(value,0,100)
    if (level == 0) {
        off()
        return
    }
    if (device.currentValue("switch") != "on") on()
    String verb = (device.currentValue("level") == level) ? "is" : "was set to"
    eventSend("level",verb,level,"%")
}

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}

def manageCycle() {
    def temp = device.currentValue("heaterTemp").toBigDecimal()
    def illum = device.currentValue("illuminance").toInteger()
    def onTemp = device.currentValue("onTemperature").toBigDecimal()
    def offTemp = device.currentValue("maxPoolTemp").toBigDecimal()
    def poolTemp = device.currentValue("poolTemp").toBigDecimal()
    def onIllum = device.currentValue("onIlluminance").toInteger()
    def operatingState = device.currentValue("operatingState")

    // Checks
    def turnOnIllum = (illum >= onIllum) && (temp >= poolTemp)
    def turnOffIllum = (illum < onIllum)
    def turnOnTemp = (temp >= onTemp ) && (poolTemp < offTemp) 
    def turnOffTemp = (temp < onTemp) || (poolTemp >= offTemp) || (temp < poolTemp)

    if (turnOnIllum && !turnOnTemp) {
        setPresence("Solar")
        setIcon("heater-solar.svg")
    }
    if (turnOnTemp && !turnOnSolar) {
        setPresence("Temperature")
        setIcon("heater-temp.svg")
    }
    if (turnOffIllum && turnOffTemp) {
        setPresence("None")
        setIcon("heater-none.svg")
    }
    if (turnOnIllum && turnOnTemp) {
        setPresence("Both")
        setIcon("heater-both.svg")
    }

    // Actions
    if ((turnOnIllum || turnOnTemp) && operatingState != "heating") {
        setOperatingState("heating")
    }

    if ((turnOffTemp && turnOffIllum) && operatingState != "idle") {
        setOperatingState("idle")
    }
        
    runIn(1, setDisplay)    
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

// Commands needed to change internal attributes of virtual device.
def setDisplay() {
	logDebug "setDisplay() was called"
    String display = "Pool Temp: "+(device.currentValue("poolTemp"))+"°<br>Heat Temp: "+(device.currentValue("heaterTemp"))+"°<br>Solar: "+(device.currentValue("illuminance"))+" lux<br>State: "+(device.currentValue("operatingState"))
    String display2 = "Max Temp: "+(device.currentValue("maxPoolTemp"))+"°<br>On Temp: "+(device.currentValue("onTemperature"))+"°<br>On Solar: "+(device.currentValue("onIlluminance"))
    String displayAll = "Pool Temperature: "+(device.currentValue("poolTemp"))+"°<br><br>Heater Temperature: "+(device.currentValue("heaterTemp"))+"°<br>On by Temperature: "+(device.currentValue("onTemperature"))+"°<br>Max Pool Temp: "+(device.currentValue("maxPoolTemp"))+"°<br><br>Operating State: "+(device.currentValue("operatingState"))+"<br>Water Comfort: "+(device.currentValue("lock"))+"<br><br>Solar Illuminance: "+(device.currentValue("illuminance"))+" lux<br>On by Illumination: "+(device.currentValue("onIlluminance"))+ " lux"
    sendEvent(name: "display", value: display, descriptionText: getDescriptionText("display set to ${display}"))
    sendEvent(name: "display2", value: display2, descriptionText: getDescriptionText("display2 set to ${display2}"))
    sendEvent(name: "displayAll", value: displayAll, descriptionText: getDescriptionText("displayA;; set to ${display2}"))
}

def setHeaterTemp(setpoint) {    
	logDebug "setHeaterTemp(${setpoint}) was called"
    sendEvent(name: "heaterTemp", value: setpoint, descriptionText: getDescriptionText("heaterTemp set to ${setpoint}"))
    runIn(1, manageCycle)
}

def setPoolTemp(setpoint) {    
    def temp = setpoint.toBigDecimal()
	logDebug "setPoolTemp(${setpoint}) was called"
    sendEvent(name: "poolTemp", value: setpoint, descriptionText: getDescriptionText("poolTemp set to ${setpoint}"))
    if (temp < settings?.cold.toBigDecimal()) {
        setTempIcon("pool-freezing.svg")
        sendEvent(name: "lock", value: "freezing", descriptionText: getDescriptionText("lock set to freezing"))
    }
    else if (temp > settings?.cold.toBigDecimal() && temp <= settings?.chilly.toBigDecimal()) {
        setTempIcon("pool-cold.svg")
        sendEvent(name: "lock", value: "cold", descriptionText: getDescriptionText("lock set to cold"))
    }
    else if (temp > settings?.chilly.toBigDecimal() && temp <= settings?.warm.toBigDecimal()) {
        setTempIcon("pool-chilly.svg")
        sendEvent(name: "lock", value: "chilly", descriptionText: getDescriptionText("lock set to chilly"))
    }
    else if (temp > settings?.warm.toBigDecimal() && temp <= settings?.hot.toBigDecimal()) {
        setTempIcon("pool-warm.svg")
        sendEvent(name: "lock", value: "warm", descriptionText: getDescriptionText("lock set to warm"))
    }
    else if (temp > settings?.hot.toBigDecimal()) {
        setTempIcon("pool-hot.svg")
        sendEvent(name: "lock", value: "hot", descriptionText: getDescriptionText("lock set to hot"))
    }
    runIn(1, manageCycle) 
}

def setIlluminance(setpoint) {
	logDebug "setIlluminance(${setpoint}) was called"
    sendEvent(name: "illuminance", value: setpoint, descriptionText: getDescriptionText("illuminance set to ${setpoint}"))
    runIn(1, manageCycle)
}

def setOnTemperature(setpoint) {    
	logDebug "setOnTemperature(${setpoint}) was called"
    sendEvent(name: "onTemperature", value: setpoint, descriptionText: getDescriptionText("onTemperature set to ${setpoint}"))
    runIn(1, manageCycle)
}

def setMaxPoolTemp(setpoint) {    
	logDebug "setMaxPoolTemp(${setpoint}) was called"
    sendEvent(name: "maxPoolTemp", value: setpoint, descriptionText: getDescriptionText("maxPoolTemp set to ${setpoint}"))
    runIn(1, manageCycle)
}

def setOnIlluminance(setpoint) {
	logDebug "setOnIlluminance(${setpoint}) was called"
    sendEvent(name: "onIlluminance", value: setpoint, descriptionText: getDescriptionText("OnIlluminance set to ${setpoint}"))
    runIn(1, manageCycle)
}

def setOperatingState(setpoint) {
	logDebug "setOperatingState(${setpoint}) was called"
    sendEvent(name: "operatingState", value: setpoint, descriptionText: getDescriptionText("operatingState set to ${setpoint}"))
    if (setpoint == "heating") on()
    if (setpoint == "idle") off()
}

def setPresence(setpoint) {
	logDebug "setpresence(${setpoint}) was called"
    sendEvent(name: "presence", value: setpoint, descriptionText: getDescriptionText("presence set to ${setpoint}"))
}

def setTempIcon(img) {
    if (state.tempIcon != img) {
        sendEvent(name: "tempIcon", value: "<img class='icon' src='${settings?.iconPath}${img}' />")
        state.tempicon = img
    }
}

def setIcon(img) {
    if (state.icon != img) {
        sendEvent(name: "icon", value: "<img class='icon' src='${settings?.iconPath}${img}' />")
        state.icon = img
    }
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}