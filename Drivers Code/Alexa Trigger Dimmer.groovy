/*
    Virtual Alexa Trigger Dimmer

    Copyright 2022 -> C. Burgess

*/

metadata {
    definition (
            name: "Alexa Trigger Dimmer",
            namespace: "hubitat",
            author: "Chris B"
    ) {

        capability "Switch"
        capability "Switch Level"

        attribute "level", "NUMBER"
        attribute "switch", "ENUM"

}

    preferences {
        input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
        input name: "returnToLevel", type: "bool", title: "Return to Level", description: "Enable to return to set level after return seconds", defaultValue: true
        input name: "returnLevel", type: "number", title: "Return Level", description: "Set Level to return to after trigger", defaultValue: 99
        input name: "returnSeconds", type: "number", title: "Return to Level Delay Seconds", description: "Set Seconds before level return after trigger", defaultValue: 3
    }
}

def installed() {
    log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "level", value: "99")

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
  
} 

def parse(String description) { noCommands("parse") }

private eventSend(name,verb,value,unit = ""){
    String descriptionText = "${device.displayName} ${name} ${verb} ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    if (unit != "") sendEvent(name: name, value: value ,descriptionText: descriptionText, unit:unit)
    else  sendEvent(name: name, value: value ,descriptionText: descriptionText)
}

def off() {
    sendEvent(name: "switch", value: "off")
}

def on() {
    sendEvent(name: "switch", value: "on")
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