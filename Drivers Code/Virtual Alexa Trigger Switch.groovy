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