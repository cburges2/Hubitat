/*
    Virtual Alexa Trigger Switch 

    Copyright 2022 -> C. Burgess

*/

metadata {
    definition (
            name: "Virtual Alexa Trigger Switch",
            namespace: "hubitat",
            author: "Chris B"
    ) {
        capability "Light"
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"
        capability "Actuator"

        attribute "command", "ENUM"

}

    preferences {
        input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
        //input name: "forceUpdate", type: "bool", title: "Force State Update", description: "Send event everytime, regardless of current status. ie Send/Do On even if already On.",  defaultValue: false
        input name: "returnToLevel", type: "bool", title: "Return to Level", description: "Enable to return to set level after return seconds", defaultValue: true
        input name: "returnLevel", type: "number", title: "Return Level", description: "Set Level to return to after trigger", defaultValue: 99
        input name: "returnSeconds", type: "number", title: "Return to Level Delay Seconds", description: "Set Seconds before level return after trigger", defaultValue: 3
    }
}

def installed() {
    log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])

    state.key = [[value:"0", command:"Never Used"],[value:"99", command:"Reset"]]   
    def i = ""
    for (x=1; x<98; x++) {
        i = x.toString()
        state.key.add([value:i, command:""])
    }
    state.commands = [[value:"99", command:"Reset"]]
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

    }   
} 

def parse(String description) { noCommands("parse") }

private eventSend(name,verb,value,unit = ""){
    String descriptionText = "${device.displayName} ${name} ${verb} ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    if (unit != "") sendEvent(name: name, value: value ,descriptionText: descriptionText, unit:unit)
    else  sendEvent(name: name, value: value ,descriptionText: descriptionText)
}

def off() {
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    close()
    eventSend("switch",verb,"off") 
}

def on() {
    String verb = (device.currentValue("switch") == "on") ? "is" : "was turned"
    open()
    eventSend("switch",verb,"on")
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
    if (settings?.returnToLevel) runIn(settings?.returnSeconds.toInteger(), returnToLevel)
    setCommand(value)
}

def returnToLevel() {
    Integer level = limitIntegerRange(settings?.returnLevel.toInteger(),0,100)
    if (device.currentValue("switch") != "on") on()
    String verb = (device.currentValue("level") == level) ? "is" : "was set to"
    eventSend("level",verb,level,"%") 
}

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}

def setCommand(value) {
	logDebug "setCommand(${value}) was called"
    def level = value.toInteger()
    def action = ""

    // bedroom
    if (value == 1) {action = "Day Time"}
    else if (value == 2) {action = "Dim Lights"}
    else if (value == 3) {action = ""}   // evening
    else if (value == 4) {action = ""}  
    else if (value == 5) {action = "Good Morning"}
    else if (value == 6) {action = "Good Night"}
    else if (value == 7) {action = "TV Time"}
    else if (value == 8) {action = "Nap Time"}
    else if (value == 9) {action = "Bedroom Scheduled Mode"}
    else if (value == 10) {action = "Watch the Back Door"}
    else if (value == 11) {action = "Reading Light"}
    else if (value == 12) {action = "Cable TV "}
    else if (value == 13) {action = "Streaming TV"}
    // Heating cooling
    else if (value == 20) {action = ""}
    else if (value == 21) {action = ""}
    else if (value == 22) {action = ""}
    else if (value == 23) {action = ""}
    // theater
    else if (value == 27) {action = "Projector On"}
    else if (value == 28) {action = "Projector Off"}
    else if (value == 29) {action = "Screen Mode"}
    else if (value == 30) {action = "Work Mode"}
    else if (value == 31) {action = "Room Mode"}
    // Sensors
    else if (value == 35) {action = "Bedroom Sensor Off"}
    else if (value == 36) {action = "Bedroom Sensor On"}
    else if (value == 37) {action = "Front Sensor Off"}
    else if (value == 38) {action = "Front Sensor On"}
    // Basement
    else if (value == 42) {action = ""}
    // Bedroom Heating Cooling
    else if (value == 50) {action = "I am cold"}
    else if (value == 51) {action = "I am warm"}
    else if (value == 52) {action = "I am hot"}
    // Front Scenes
    else if (value == 60) {action = "Lights On"}   // front lights on
    else if (value == 61) {action = "Lights Off"}  // front lights off
    else if (value == 62) {action = "Dinner Time"}
    else if (value == 63) {action = "Dinner is Over"}
    else if (value == 64) {action = "Dru is Home"}
    else if (value == 65) {action = "Dim Front Lights"}
    else if (value == 66) {action = ""}
    else if (value == 67) {action = "Front Scheduled Mode"}
    else if (value == 68) {action = "I Want to Read"}
    else if (value == 69) {action = "Do we have Mail?"}
    else if (value == 72) {action = "Front Lights Bright"}
    // Testing
    else if (value == 95) {action = ""}
    else {action = "Unused Code"}

    if (action == "") {action = "Empty Code"}
    sendEvent(name: "command", value: action ,descriptionText: "${action} Triggered")

    state?.commands = [value: "${value}", command: "${action}"]
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