/*
    Virtual Dehumidistat Fan Control

    Copyright 2022 -> C. Burgess

*/

metadata {
    definition (
            name: "Virtual Dehumidistat Fan Control",
            namespace: "hubitat",
            author: "Chris B"
    ) {
        capability "Light"
        capability "Switch"
        capability "Actuator"
        //capability "Contact Sensor" //"open", "closed"
        capability "Fan Control"
        capability "Motion Sensor"  //"active", "inactive"

        attribute "dehumidifyingSetpoint", "NUMBER"
        attribute "operatingState", "ENUM" // ["dehumidifying", "idle", "off"]
        attribute "hysteresis", "NUMBER"
        attribute "humidity", "NUMBER"
        attribute "display", "STRING"
        attribute "speed", "ENUM"
        attribute "motion", "ENUM"
        attribute "setpointOffset", "ENUM"
        attribute "minSetpoint", "ENUM"
        

        // Commands needed to change internal attributes of virtual device.
        command "setDehumidifyingSetpoint", ["NUMBER"]
        command "setOperatingState", [[name:"operatingState",type:"ENUM", description:"Set Operating State", constraints:["dehumidifying","idle"]]]
        command "setHumidity", ["NUMBER"]
        command "setHysteresis", ["NUMBER"]
        command "manageCycle"
        command "setDisplay", ["STRING"]
        command "setSpeed", [[name:"setSpeed",type:"ENUM", description:"Set Fan Speed", constraints:["off","low","medium","high"]]]
        command "setActive"
        command "setInactive"
        command "setOffInMin", [[name:"offInMin",type:"ENUM", description:"Set Off in Minutes", constraints:["5","10","15","20","25","30","35","40","45"]]]
        command "setSetpointOffset", [[name:"setpointOffset",type:"ENUM", description:"Set Offset from room Humidity", constraints:["1","2","3","4","5","6","7","8","9","10"]]]
        command "setMinSetpoint", ["ENUM"]
}

    preferences {
        input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
        input name: "autoOff", type: "enum", description: "Automatically sets motion inactive after selected time.", title: "Enable Auto-inactive", options: [[0:"Disabled"],[1:"1s"],[2:"2s"],[5:"5s"],[10:"10s"],[20:"20s"],[30:"30s"],[60:"1m"],[120:"2m"],[300:"5m"],[600:"10m"],[1800:"30m"],[3200:"60m"]], defaultValue: 0
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
        sendEvent(name: "Dehumidifying Setpoint", value: (dehumidifyingSetpoint ?: 45).toBigDecimal())   
               
        state.lastRunningMode = "humidify"
        updateDataValue("lastRunningMode", "humidify")
        setOperatingState("idle")
        setDehumidifyingSetpoint(45)
        setHumidity(50)
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
    sendEvent(name: "operatingState", value: "dehumidifying", descriptionText: getDescriptionText("operatingState set to dehumidifying")) 
    sendEvent(name: "contact", value: "open", isStateChange: forceUpdate)
}

def off() {
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    eventSend("switch",verb,"off")
    sendEvent(name: "operatingState", value: "idle", descriptionText: getDescriptionText("operatingState set to idle")) 
    sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
}

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}

def setDisplay() {
    logDebug "setDisplay() was called"
    String display = "Humidity: "+(device.currentValue("humidity"))+"%<br> Setpoint: "+(device.currentValue("dehumidifyingSetpoint"))+"%<br> "+(device.currentValue("operatingState"))+" "+(device.currentValue("motion")) 
    sendEvent(name: "display", value: display, descriptionText: getDescriptionText("display set to ${display}"))
}

def setOperatingState (state) {
    logDebug "setOperatingState(${state}) was called"
    sendEvent(name: "operatingState", value: state, descriptionText: getDescriptionText("operatingState set to ${state}"))   
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def manageCycle(){

    def hysteresis = (device.currentValue("hysteresis")).toBigDecimal()

    def dehumidifyingSetpoint = (device.currentValue("dehumidifyingSetpoint"))
    def humidity = (device.currentValue("humidity"))
    def operatingState = (device.currentValue("operatingState"))
    def minSetpoint = device.currentValue("minSetpoint").toInteger()
    
    if (dehumidifyingSetpoint < minSetpoint) {
        dehumidifyingSetpoint = minSetpoint
        setDehumidifyingSetpoint(minSetpoint)
    }

    def dehumidifyingOn = (humidity >= (dehumidifyingSetpoint + hysteresis))
    //if (operatingState == "dehumidifying") dehumidifyingOn = (humidity >= (dehumidifyingSetpoint - hysteresis))
    
    if ((dehumidifyingOn && operatingState != "dehumidifying")) {
        setOperatingState("dehumidifying")
        String verb = (device.currentValue("switch") == "on") ? "is" : "was turned"
        eventSend("switch",verb,"on")    
        //sendEvent(name: "contact", value: "open", isStateChange: forceUpdate)
    }
    else if ((!dehumidifyingOn && operatingState != "idle")){
        if (state.switch == false) {
            setOperatingState("idle")
            String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
            eventSend("switch",verb,"off")
            //sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
        }
    }
    runIn(1,setDisplay)
}

// Commands needed to change internal attributes of virtual device.
def setHumidity(humidity) {
    logDebug "setHumidity(${humidity}) was called"
    sendEvent(name: "humidity", value: humidity, unit: "%", descriptionText: getDescriptionText("humidity set to ${humidity}%"))
    runIn(1, manageCycle)
}

def setDehumidifyingSetpoint(setpoint) {
    logDebug "setDehumidifyingSetpoint(${setpoint}) was called"
    sendEvent(name: "dehumidifyingSetpoint", value: setpoint, descriptionText: getDescriptionText("dehumidifyingSetpoint set to ${setpoint}"))
    runIn(1, manageCycle)
    runIn(1,setDisplay)
}

def setHysteresis(setpoint) {
    logDebug "setHysteresis(${setpoint}) was called"
    sendEvent(name: "hysteresis", value: setpoint, descriptionText: getDescriptionText("hysteresis set to ${setpoint}"))
    runIn(1, manageCycle)
}

def setSpeed(setpoint) {
    logDebug "setSpeed(${setpoint}) was called"
    sendEvent(name: "speed", value: setpoint, descriptionText: getDescriptionText("speed set to ${setpoint}"))
}

def setSetpointOffset(setpoint) {
    logDebug "setSetpointOffset(${setpoint}) was called"
    sendEvent(name: "setpointOffset", value: setpoint, descriptionText: getDescriptionText("setpointOffset set to ${setpoint}"))
}

def setMinSetpoint(setpoint) {
    logDebug "setMinSetpoint(${setpoint}) was called"
    sendEvent(name: "minSetpoint", value: setpoint, descriptionText: getDescriptionText("minSetpoint set to ${setpoint}"))
}

def setActive() {
    logDebug "setActive was called"
    sendEvent(name: "motion", value: "active", descriptionText: getDescriptionText("motion set to active"))
    runIn(autoOff.toInteger(),setInactive)
    runIn(1,setDisplay)
}

def setInactive() {
    logDebug "setInactive was called"
    sendEvent(name: "motion", value: "inactive", descriptionText: getDescriptionText("motion set to inactive"))
    runIn(1,setDisplay)
}

def setOffInMin(min) {
    logDebug "setOffInMin(${min}) was called"
    on()
    state.switch = true
    def sec = (min * 60).toInteger()
    logDebug "Off in ${sec} seconds"
    runIn(sec, autoFanOff)
}

def autoFanOff() {
    logDebug "autoFanOff() was called"
    if (device.currentValue("operatingState") != "dehumidifying") {
        off()
    }
    state.switch = false
    runIn(1,setDisplay)
}

private logDebug(msg) {
    if (settings?.logEnable) log.debug "${msg}"
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

private getDescriptionText(msg) {
    def descriptionText = "${device.displayName} ${msg}"
    if (settings?.txtEnable) log.info "${descriptionText}"
    return descriptionText
}