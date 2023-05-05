/*
    Virtual Humidistat

    Copyright 2023 -> C. Burgess

*/

metadata {
    definition (
            name: "Virtual Humidistat",
            namespace: "hubitat",
            author: "Chris B"
    ) {
        capability "Motion Sensor"    //"active", "inactive"
        capability "Light"
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"
        capability "Actuator"
        capability "Contact Sensor"  //"open", "closed"
        capability "Presence Sensor"
        capability "AudioVolume"
        capability "Lock"            //"locked", "unlocked"
        
        attribute "operatingState", "ENUM" // ["humidifying", "idle", "off"]
        attribute "hysteresis", "NUMBER"  // hysteresis
        attribute "humidity", "NUMBER"    // Room Humidity
        attribute "display", "STRING"     // Dashboard icon display status
        attribute "power", "ENUM"       // power metering outlet (set in sync app)
        attribute "presence", "ENUM"      // Water level description
        attribute "waterLevel", "NUMBER"  // water level %
        attribute "volume", "NUMBER"      // water level %
        attribute "mute", "ENUM"          // unused (comes with volume)
        attribute "motion", "ENUM"        // Sanitization alexa switch - active activates sanitization
        attribute "lock", "ENUM"          // switch for sanitize on/off when humidifier on 
        attribute "checkError", "ENUM"    // check power to see if humidifier is on/off when should/should not be

        // Commands needed to change internal attributes of virtual device.
        command "setHumidifyingSetpoint", ["NUMBER"]
        command "setOperatingState", ["ENUM"]
        command "setHumidity", ["NUMBER"]
        command "setHysteresis", ["NUMBER"]
        command "manageCycle"
        command "setDisplay", ["STRING"]
        command "setPower", ["ENUM"]
        command "setPresence", [[name:"waterLevel",type:"ENUM", description:"Set Water Level", constraints:["full","half","empty","fill"]]]
        command "setWaterLevel", ["NUMBER"]
        command "setVolume", ["NUMBER"]
        command "setMotion", [[name:"motion",type:"ENUM", description:"Set Sanitization", constraints:["active","inactive"]]]
        command "setCheckError", [[name:"motion",type:"ENUM", description:"Set Sanitization", constraints:["on","off"]]]
}

    preferences {
        input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
    }
}


def installed() {
    log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])
    initialize()
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
    initialize()
}

def initialize() {
    if (state?.lastRunningMode == null) {    
        sendEvent(name: "Operating State", value: (operatingState ?: "idle"))
        sendEvent(name: "Humidity", value: (humidity ?: 0).toBigDecimal())
        sendEvent(name: "Humidifying Setpoint", value: (humidifyingSetpoint ?: 45).toBigDecimal())   
               
        state.lastRunningMode = "humidify"
        updateDataValue("lastRunningMode", "humidify")
        setoOperatingState("idle")
        setHumidifyingSetpoint(45)
        setHumidiy(50)
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
    sendEvent(name: "operatingState", value: "humidifying", descriptionText: getDescriptionText("operatingState set to humidifying")) 
    sendEvent(name: "contact", value: "open", isStateChange: forceUpdate)
    if (device.currentValue("lock") == "locked") {
        sendEvent(name: "motion", value: "active", isStateChange: forceUpdate)
    }    
}

def off() {
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    eventSend("switch",verb,"off")
    sendEvent(name: "operatingState", value: "idle", descriptionText: getDescriptionText("operatingState set to idle")) 
    sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
    if (device.currentValue("lock") == "locked") {
        sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
    }  
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
    sendEvent(name: "humidifyingSetpoint", value: level, descriptionText: getDescriptionText("humidifyingSetpoint set to ${level}"))
    runIn(1, manageCycle)
    runIn(1, setDisplay)
}

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}

def setDisplay() {
    logDebug "setDisplay() was called"
    String display = "Humidity: "+ device.currentValue("humidity")+"%<br> Setpoint: "+ device.currentValue("humidifyingSetpoint")+"%<br> Water: "+device.currentValue("volume")+"%<br> "+device.currentValue("operatingState")
    sendEvent(name: "display", value: display, descriptionText: getDescriptionText("display set to ${display}"))
}

def setOperatingState (state) {
    logDebug "setOperatingState(${state}) was called"
    sendEvent(name: "operatingState", value: state, descriptionText: getDescriptionText("operatingState set to ${state}"))   
    runIn(1,setDisplay)
}

def setPower(power) {
    logDebug "setPower(${power}) was called"
    sendEvent(name: "power", value: power, descriptionText: getDescriptionText("power set to ${power}"))   
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def manageCycle(){

    def hysteresis = (device.currentValue("hysteresis")).toBigDecimal()

    def humidifyingSetpoint = (device.currentValue("humidifyingSetpoint"))
    def humidity = (device.currentValue("humidity"))
    def operatingState = (device.currentValue("operatingState"))
    
    def humidifyingOn = (humidity <= (humidifyingSetpoint - hysteresis))
    
    if ((humidifyingOn && operatingState != "humidifying")) {  
        setOperatingState("humidifying")
        on()
        if (device.currentValue("lock") == "locked") {
            sendEvent(name: "motion", value: "active", isStateChange: forceUpdate)
        }
    }
    else if ((!humidifyingOn && operatingState != "idle")){  
        setOperatingState("idle")
        off()
        if (device.currentValue("lock") == "locked") {
            sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
        }    
    }
    runIn(1,setDisplay)
}

/* Check power use to see if humidifier is not in sync with expected on-off status  */
def checkError() {
    logDebug "checkError() was called"
    def humidity = (device.currentValue("humidity"))
    def humidifyingSetpoint = (device.currentValue("humidifyingSetpoint"))
    def hysteresis = (device.currentValue("hysteresis")).toBigDecimal()    
    def power = (device.currentValue("power"))
    def humidifyingOn = (humidity <= (humidifyingSetpoint - hysteresis))
    if (!humidifyingOn && power.toInteger() > 10) {
        logDebug "ERROR: Humidifier on when it should be off - turning off"
        setOperatingState("idle")
        off()
        if (device.currentValue("lock") == "locked") {
            sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
        }  
        setOperatingState("idle")
    }   
    if (humidifyingOn && power.toInteger() < 10) {
        logDebug "ERROR: Humidifier off when it should be on - turning on"
        setOperatingState("humidifying")
        on()
        if (device.currentValue("lock") == "locked") {
            sendEvent(name: "motion", value: "active", isStateChange: forceUpdate)
        }  
    }       
    runIn(1,setDisplay)
    if (device.currentValue("checkError") == "on") {
        runIn(1,runCheckError)       // contine error checking every 10 min if checking is on
    }    
}

def runCheckError() { 
    runIn(600,checkError)
}

// Commands needed to change internal attributes of virtual device.
def setHumidity(humidity) {
    logDebug "setHumidity(${humidity}) was called"
    sendEvent(name: "humidity", value: humidity, unit: "%", descriptionText: getDescriptionText("humidity set to ${humidity}%"))
    runIn(1, manageCycle)
}

def setCheckError(state) {
    logDebug "setCheckError(${statae}) was called"
    sendEvent(name: "checkError", value: state, descriptionText: getDescriptionText("checkError set to ${state}"))
    if (state == "on") {
        runIn(1,checkError)
    }
}

def setHumidifyingSetpoint(setpoint) {
    logDebug "setHumidifyingSetpoint(${setpoint}) was called"
    runIn(1,setLevel(setpoint,null))
}

def setHysteresis(setpoint) {
    logDebug "setHysteresis(${setpoint}) was called"
    sendEvent(name: "hysteresis", value: setpoint, descriptionText: getDescriptionText("hysteresis set to ${setpoint}"))
    runIn(1, manageCycle)
}

def setPresence(setpoint) {
    logDebug "sePresence(${setpoint}) was called"
    sendEvent(name: "presence", value: setpoint, descriptionText: getDescriptionText("presence set to ${setpoint}"))
    runIn(1,setDisplay)
}

def setWaterLevel(setpoint) {
    logDebug "seWaterLevel(${setpoint}) was called"
    sendEvent(name: "waterLevel", value: setpoint, unit: "%", descriptionText: getDescriptionText("waterLevel set to ${setpoint}"))
    runIn(1,setDisplay)
}

def setVolume(setpoint) {
    logDebug "setVolume(${setpoint}) was called"
    sendEvent(name: "volume", value: setpoint, unit: "%", descriptionText: getDescriptionText("volume set to ${setpoint}"))
    runIn(1,setDisplay)
}

def mute() {
    logDebug "mute() was called"
    sendEvent(name: "mute", value: "muted", descriptionText: getDescriptionText("mute set to muted"))
}

def unmute() {
    logDebug "unmute() was called"
    sendEvent(name: "mute", value: "unmuted", descriptionText: getDescriptionText("mute set to unmuted"))
}

def setMotion(setpoint) {
    logDebug "setMotion(${setpoint}) was called"
    sendEvent(name: "motion", value: setpoint, descriptionText: getDescriptionText("motion set to ${setpoint}"))
}

def setMotionInactive() {
    logDebug "setMotionInactive() was called"
    sendEvent(name: "motion", value: "inactive", descriptionText: getDescriptionText("motion set to inactive"))
}

def lock(setpoint) {
    logDebug "lock() was called"
    sendEvent(name: "lock", value: "locked", descriptionText: getDescriptionText("lock set to locked"))
}

def unlock(setpoint) {
    logDebug "unlock() was called"
    sendEvent(name: "lock", value: "unlocked", descriptionText: getDescriptionText("lock set to unlocked"))
}

private logDebug(msg) {
    if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
    def descriptionText = "${device.displayName} ${msg}"
    if (settings?.txtEnable) log.info "${descriptionText}"
    return descriptionText
}