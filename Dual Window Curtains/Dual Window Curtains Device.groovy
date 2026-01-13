/*
    Dual Window Curtains Device

    Copyright 2025 -> Chris B

    This app will combine two curtain motors into one device using the community Zemismart Blind Driver
*/

metadata {
    definition (
            name: "Dual Window Curtains Device",
            namespace: "Hubitat",
            author: "Chris B"
    ) {
        capability "Actuator"
        capability "WindowShade"        
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"

		attribute "speed", "integer"
        attribute "positionLeft", "integer"
        attribute "positionRight", "integer"

		command "stepClose", [[name: "step", type: "NUMBER", description: "Amount to change position towards close."]]
		command "stepOpen", [[name: "step",	type: "NUMBER",	description: "Amount to change position towards open."]]
		command "setSpeed", [[name: "speed*", type: "NUMBER", description: "Motor speed (0 to 100). Values below 5 may not work."]]
        command "eventSend", [[name:"name",type:"STRING", description:"Attribute Name"], [name:"value",type:"STRING", description:"Attribute Value"]]

}
    preferences {
        input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
    }
}

def installed() {
    log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])

    initAttributes()
    updated()
}

def initAttributes() {
    eventSend("switch", "off")
    eventSend("level", 0)
    eventSend("position", 0)
    eventSend("speed", 100)
    eventSend("windowShade", "closed") 
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

def eventSend(name, value) {
    sendEvent(name: name, value: value)
}

def off() {
    parent?.on()
}

def on() {
    parent?.off()
}

def open() {
    parent?.open()
}

def close() {
    parent?.close()
}

def stepOpen(num) {
    parent?.stepOpen(num)
}

def stepClose(num) {
    parent?.stepClose(num)
}

def setPosition(position) {
    parent?.setPosition(position)
}

def startPositionChange(direction) {
    parent?.startPositionChange(direction)
}

def stopPositionChange() {
    parent?.stopPositionChange()
}

def setLevel(value, rate = null) {
    parent?.setLevel(value, rate)
}

def setSpeed(speed) {
    parent?.setspeed(speed)
}

private logDebug(msg) {
    if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
    def descriptionText = "${device.displayName} ${msg}"
    if (settings?.txtEnable) log.info "${descriptionText}"
    return descriptionText
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}