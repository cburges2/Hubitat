/*
	Virtual Stove Fan Hood Driver - Works in conjunction with the Fan Hood Controller App.  This driver allows access to fan conroller app as if it were a multi-speed fan device

	Copyright 2024 -> ChrisB 

    Version 1.0 - 11/14/24

*/

metadata {
	definition (
			name: "Fan Hood Driver",
			namespace: "chrisb",
			author: "Chris B"
	) {
        capability "Fan Control"
        capability "Switch"
		capability "Actuator"

        attribute "speed", "ENUM"
        attribute "switch", "ENUM"   
        attribute "offMinutes", "ENUM"

        command "setSpeed", [[name:"setSpeed",type:"ENUM", description:"Set Fan Speed", constraints:["off","low","medium","high"]]]
        command "on"
        command "off"
        command "setOffMin", [[name:"offMinutes",type:"ENUM", description:"Set Off in Minutes", constraints:["5","10","15","20","25","30","35","40","45","60","90","120"]]]
}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input name: "autoOff", type: "bool", description: "Automatically turn fan off after selected time.", title: "Enable Auto-Off",  defaultValue: false
    }
}


def installed() {
	log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])
    state.speed = "off"
	updated()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	if (logEnable) runIn(1800,logsOff)
	initialize()
}

def initialize() {


}

def setOffMin(min) {
    sendEvent(name: "offMinutes", value: min, descriptionText: getDescriptionText("offMinutes set to ${min}")) 
}

def parse(String description) { noCommands("parse") }

def on() {
    sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on")) 
    setSpeed(state?.speed)
    if (autoOff) {
        def secs = device.currentValue("offMinutes").toInteger() * 60
        runIn(secs, off)
    }
}

def off() {
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch off")) 
    sendEvent(name: "speed", value: "off", descriptionText: getDescriptionText("speed set to off"))
    unschedule()
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def setSpeed(setpoint) {
    logDebug "setSpeed(${setpoint}) was called"
    if (setpoint == "off") {
        state?.speed = device.currentValue("speed")
        sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch off")) 
        sendEvent(name: "speed", value: "off", descriptionText: getDescriptionText("speed set to off"))
    }
    else if (setpoint == "on") {
        sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on")) 
        sendEvent(name: "speed", value: state?.speed, descriptionText: getDescriptionText("speed set to ${state?.speed}")) 
    } else {
        sendEvent(name: "speed", value: setpoint, descriptionText: getDescriptionText("speed set to ${setpoint}"))
        state.speed = setpoint
        if (device.currentValue("switch") == "off") {
            sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on"))
        }
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