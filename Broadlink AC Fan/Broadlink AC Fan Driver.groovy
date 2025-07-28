/*
	Broadlink AC Fan Driver 

	Copyright 2025 -> cburgess

    Virtural Fan Driver:
    Version 1.0 - 11/14/24
    Version 1.2 - 03/17/25 - updated to SwitchLevel, using thermostatFanMode range of three
    Version 1.3 - 03/16/25 - fixed level defaulting to zero with setThermostatFanModeLevel of "on"
    Version 1.4 - 03/19/25  - fixed not turning on for state.thermostatFanMode being set to off when setting thermostatFanMode after a switch on event

    Broadlink AC Fan Driver:
    Version 1.0 - 07/26/25 - Companion App: Broadlink AC Fan Connector (sends the broadlink commands when this driver's states change)
        Virtual Fan Driver wss forked to become a virtual ac fan switch. Switches an IR AC unit controlled with a Broadlink device between modes (off, fan cool) and fan speeds. 
        This uses thermostat capability atrributes and command to allow control from a thermostat tile on a dashboard, but this is NOT a thermostat. 
        For the sake of the dashboard tile, if a temperature sensor is selected in the app, it will update temperature, but only for the dashboard tile display
        Using a dashboard dimmer tile, this becaomes a three speed fan only. Speeds are controlled with the dimmer. Turning on the dimmer switch will turn on the AC in fand mode       
*/

metadata {
	definition (
        name: "Broadlink AC Fan Driver",
        namespace: "Hubitat",
        author: "cburgess"
	) {
        capability "Switch"
        capability "Actuator"
        capability "SwitchLevel"
        capability "Pushable Button"

        attribute "thermostatMode", "ENUM"
        attribute "broadlink", "ENUM"
        attribute "temperature", "STRING"

        attribute "thermostatFanMode", "ENUM"
        attribute "coolFanSpeed", "ENUM"
        attribute "fanOnlySpeed", "ENUM"
        attribute "switch", "ENUM"   
        attribute "supportedThermostatFanModes", "JSON_OBJECT"
        attribute "supportedThermostatModes", "JSON_OBJECT"
        attribute "thermostatOperatingState", "ENUM"
        attribute "level", "ENUM"
        attribute "pushed", "enum"

        command "setThermostatFanMode", [[name:"setThermostatFanMode",type:"ENUM", description:"Set Fan ThermostatFanMode", constraints:["fan low","fan medium","fan high","cool low","cool medium","cool high","off"]]]
        command "setThermostatMode", [[name:"setThermostatMode",type:"ENUM", description:"Set AC Mode", constraints:["fan","cooling","off"]]]
        command "setCoolFanSpeed", [[name:"setCoolSpeed",type:"ENUM", description:"Set Cool Fan Speed", constraints:["low","medium","high"]]]
        command "setFanOnlySpeed", [[name:"setFanOnlySpeed",type:"ENUM", description:"Set Fan Only Speed", constraints:["low","medium","high"]]]
        command "setTemperature", ["STRING"]
        command "sendBroadlink"
        command "setSupportedFanThermostatFanModes", ["JSON_OBJECT"]
        command "setSupportedThermostatFanThermostatFanModes", ["JSON_OBJECT"]
        command "on"
        command "off"
        command "setLevel", ["NUMBER"]
        command "initialize"
        command "push", ["NUMBER"]

        command "cool"
        command "fan"

}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "fanPrefix", type:"text", title: "Prefix for Fan Command",defaultValue: "AC Fan")
        input( name: "coolPrefix", type:"text", title: "Prefix for Cool Command",defaultValue: "AC Cool")
        input( name: "offPrefix", type:"text", title: "Prefix for Off Command",defaultValue: "AC Off")
        input( name: "lowCommand", type:"text", title: "Command speed for low",defaultValue: "Low")
        input( name: "medCommand", type:"text", title: "Command speed for medium",defaultValue: "Medium")
        input( name: "highCommand", type:"text", title: "Command speed for high",defaultValue: "High")
    }
}


def installed() {
	log.warn "installed..." 
    setSupportedFanThermostatFanModes()
    setSupportedThermostatModes()
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch off")) 
    sendEvent(name: "thermostatFanMode", value: "off", descriptionText: getDescriptionText("thermostatFanMode set to off"))
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch set to off"))
    sendEvent(name: "thermostatMode", value: "off", descriptionText: getDescriptionText("thermostatMode set to Off"))
    sendEvent(name: "broadlink", value: "idle", descriptionText: getDescriptionText("broadlink set to idle"))
    sendEvent(name: "temperature", value: " ", descriptionText: getDescriptionText("temperature set to blank"))
    sendEvent(name: "thermostatOperatingState", value: "off", descriptionText: getDescriptionText("thermostatOperatingState set to off"))
    sendEvent(name: "fanOnlySpeed", value: "low", descriptionText: getDescriptionText("fanOnlySpeed set to low"))
    sendEvent(name: "coolFanSpeed", value: "low", descriptionText: getDescriptionText("coolFanSpeed set to low"))
    sendEvent(name: "level", value: 25, descriptionText: getDescriptionText("level set to 25"))

	updated()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	if (logEnable) runIn(1800,logsOff)
    setSupportedThermostatModes()
	initialize()
}

def setTemperature(temp) {

    def temperature = temp.toString()
    sendEvent(name: "temperature", value: temperature, descriptionText: getDescriptionText("temperature set to ${temperature}"))
}

def initialize() {

    setSupportedThermostatFanModes()
    setSupportedThermostatModes()
}

// intialize thermostat attributes for the sake of the thermostat tile controls
def setSupportedThermostatFanModes() {
    sendEvent(name: "supportedThermostatFanModes", value: '["fan low","fan medium","fan high","cool low","cool medium","cool high","off"]')
}
def setSupportedThermostatModes() {
    sendEvent(name: "supportedThermostatModes", value: '["fan only","cooling","off"]')
}

def push(button) {
	logDebug("button pushed is ${button}")

    sendEvent(name: "pushed", value: button, descriptionText: getDescriptionText("pushed was ${button}"))

	if (button == 1) {setFan()}         // fan
	else if (button == 2) {setCool()}   // cool
	else if (button == 3) {off()}       // off
}

def setSupportedFanThermostatFanModes(thermostatFanModeMap) {
    sendEvent(name: "supportedFanThermostatFanModes", value: thermostatFanModeMap)
}

def setOffMin(min) {
    sendEvent(name: "offMinutes", value: min, descriptionText: getDescriptionText("offMinutes set to ${min}")) 
}

def parse(String description) { noCommands("parse") }

def on() {
    def speed = device.currentValue("fanOnlySpeed")
    sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on"))
    sendEvent(name: "thermostatFanMode", value: speed, descriptionText: getDescriptionText("thermostatFanMode set to ${speed}"))
    sendEvent(name: "thermostatMode", value: "fan", descriptionText: getDescriptionText("thermostatMode set to fan"))
    sendEvent(name: "thermostatOperatingState", value: "fan only", descriptionText: getDescriptionText("thermostatOperatingState set to fan only"))
    runIn(1,sendBroadlink)
}

def off() {
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch off")) 
    sendEvent(name: "thermostatFanMode", value: "off", descriptionText: getDescriptionText("thermostatFanMode set to off")) 
    state.lastThermostatMode = device.currentValue("thermostatMode")
    sendEvent(name: "thermostatMode", value: "off", descriptionText: getDescriptionText("thermostatMode set to off"))
    sendEvent(name: "thermostatOperatingState", value: "off", descriptionText: getDescriptionText("thermostatOperatingState set to off"))
    runIn(1,sendBroadlink)
}

def setLevel(level) {

    def lvl = level.toInteger()
    if (lvl == 0) {setThermostatFanMode("off")}
    if (lvl > 0 && lvl < 33) {setThermostatFanMode("low"); setFanOnlySpeed("low")}
    if (lvl >= 33 && lvl < 66) {setThermostatFanMode("medium"); setFanOnlySpeed("medium")}
    if (lvl >= 66) {setThermostatFanMode("high"); setFanOnlySpeed("high")}
    sendEvent(name: "level", value: level, descriptionText: getDescriptionText("Level set to ${level}")) 
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def setThermostatFanMode(thermostatFanMode) {
    logDebug "setThermostatFanMode(${thermostatFanMode}) was called"
    
    if (thermostatFanMode == "off") {
        off()
        sendEvent(name: "thermostatFanMode", value: "off", descriptionText: getDescriptionText("coolFanSpeed set to off"))    
    }
    else if (thermostatFanMode == "fan low") {sendEvent(name: "fanOnlySpeed", value: "low", descriptionText: getDescriptionText("fanOnlySpeed set to low")); setThermostatFanModeLevel("low") }
    else if (thermostatFanMode == "fan medium") {sendEvent(name: "fanOnlySpeed", value: "medium", descriptionText: getDescriptionText("fanOnlySpeed set to medium")); setThermostatFanModeLevel("medium") }
    else if (thermostatFanMode == "fan high") {sendEvent(name: "fanOnlySpeed", value: "high", descriptionText: getDescriptionText("fanOnlySpeed set to high")); setThermostatFanModeLevel("high") }  
    else if (thermostatFanMode == "cool low") {sendEvent(name: "coolFanSpeed", value: "low", descriptionText: getDescriptionText("coolFanSpeed set to low")) }
    else if (thermostatFanMode == "cool medium") {sendEvent(name: "coolFanSpeed", value: "medium", descriptionText: getDescriptionText("coolFanSpeed set to medium")) }
    else if (thermostatFanMode == "cool high") {sendEvent(name: "coolFanSpeed", value: "high", descriptionText: getDescriptionText("coolFanSpeed set to high")) }  

    if (thermostatFanMode.contains("low")) {sendEvent(name: "thermostatFanMode", value: "low", descriptionText: getDescriptionText("thermostatFanMode set to low"))}
    if (thermostatFanMode.contains("medium")) {sendEvent(name: "thermostatFanMode", value: "medium", descriptionText: getDescriptionText("thermostatFanMode set to medium"))}
    if (thermostatFanMode.contains("high")) {sendEvent(name: "thermostatFanMode", value: "high", descriptionText: getDescriptionText("thermostatFanMode set to high"))}

    if (thermostatFanMode != "off") {
        def thermostatMode = device.currentValue("thermostatMode")
        if (thermostatFanMode.contains("fan") && thermostatMode != "fan") {
            sendEvent(name: "thermostatMode", value: "fan", descriptionText: getDescriptionText("thermostatMode set to fan"))
            sendEvent(name: "thermostatOperatingState", value: "fan only", descriptionText: getDescriptionText("thermostatOperatingState set to fan only"))
        }
        if (thermostatFanMode.contains("cool") && thermostatMode != "cooling") {
            sendEvent(name: "thermostatMode", value: "cooling", descriptionText: getDescriptionText("thermostatMode set to cooling"))
            sendEvent(name: "thermostatOperatingState", value: "cooling", descriptionText: getDescriptionText("thermostatOperatingState set to cooling"))
        }
        if (device.currentValue("switch") == "off") {on()}
    }

    if (device.currentValue("broadlink") == "idle") {runIn(1,sendBroadlink)}  // send new settings to broadlink
}

def setCoolFanSpeed(speed) {
    sendEvent(name: "coolFanSpeed", value: speed, descriptionText: getDescriptionText("coolFanSpeed set to ${speed}"))
    if (device.currentValue("thermostatMode") == "cooling") {sendEvent(name: "thermostatFanMode", value: speed, descriptionText: getDescriptionText("thermostatFanMode set to ${speed}"))}
    runIn(1,sendBroadlink)
}

def setFanOnlySpeed(speed) {
    sendEvent(name: "fanOnlySpeed", value: speed, descriptionText: getDescriptionText("fanOnlySpeed set to ${speed}"))
    if (device.currentValue("thermostatMode") == "fan") {sendEvent(name: "thermostatFanMode", value: speed, descriptionText: getDescriptionText("thermostatFanMode set to ${speed}"))}
    runIn(1,sendBroadlink)
}

def setThermostatFanModeLevel(thermostatFanMode) {

    def level = device.currentValue("level")
    
    if (thermostatFanMode == "low") {level = 33}
    if (thermostatFanMode == "medium") {level = 66}
    if (thermostatFanMode == "high") {level = 100}
    if (thermostatFanMode != "off") sendEvent(name: "level", value: level, descriptionText: getDescriptionText("Level set to ${level}")) 

    state.lastThermostatFanMode = thermostatFanMode
}

def setThermostatMode(newState) {
    sendEvent(name: "thermostatMode", value: newState, descriptionText: getDescriptionText("thermostatMode set to ${newState}"))
    def opState = newState
    if (newState == "fan") {opState = "fan only"}
    sendEvent(name: "thermostatOperatingState", value: opState, descriptionText: getDescriptionText("thermostatOperatingState set to ${opState}"))
    if (newState == "fan") {
        on()
        def speed = device.currentValue("fanOnlySpeed")
        sendEvent(name: "thermostatFanMode", value: speed, descriptionText: getDescriptionText("thermostatFanMode set to ${speed}"))
    }
    if (newState == "cooling") {
        def speed = device.currentValue("coolFanSpeed")
        sendEvent(name: "thermostatFanMode", value: speed, descriptionText: getDescriptionText("thermostatFanMode set to ${speed}"))
        sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on"))
        //sendEvent(name: "temperature", value: "on", descriptionText: getDescriptionText("temperature set to on"))
    }
    if (newState == "off") {off()}
    runIn(1,sendBroadlink)
}

def fan() {
    setThermostatMode("fan")
}

def cool() {
    setThermostatMode("cooling")
}

def sendBroadlink() {

    def notSending = device.currentValue("broadlink") == "idle"

    if (notSending) {
        sendEvent(name: "broadlink", value: "sending", descriptionText: getDescriptionText("broadlink set to sending"))
        //pauseExecution(500)
        
        def acState = device.currentValue("thermostatMode")

        def command
        if (acState == "fan") {
            def speed = getFanSpeed()
            logDebug("speed is ${speed}")
            command = "AC Fan " + speed
        }
        if (acState == "cooling") {
            def speed = getCoolSpeed()
            logDebug("speed is ${speed}")
            command = "AC Cool " + speed
        }
        if (acState == "off") {
            command = "AC Off"
        }

        logDebug("Sending ${command} to Connector App")
        parent.sendBroadlinkCommand(command)
        runIn(2,resetBroadlink)
    }
}

String getFanSpeed() {
    def speed = device.currentValue("fanOnlySpeed")
    if (speed == "off") {speed = acFanSwitch.currentValue("lastSpeed")}
    def capSpeed = capitalFanSpeed(speed)
    return capSpeed
}

String getCoolSpeed() {
    def speed = device.currentValue("coolFanSpeed")
    def capSpeed = capitalFanSpeed(speed)
    return capSpeed
}

String capitalFanSpeed(speed) {
    def capSpeed = "Off"
    if (speed == "low") {capSpeed = "Low"}
    if (speed == "medium") {capSpeed = "Medium"}
    if (speed == "high") {capSpeed = "High"}

    return capSpeed
}

def resetBroadlink() {
    sendEvent(name: "broadlink", value: "idle", descriptionText: getDescriptionText("broadlink set to idle"))
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}