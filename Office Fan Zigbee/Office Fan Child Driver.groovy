/*
	Office Fan Child Driver - Works in conjunction with the Office Fan Parent App.  This driver allows access to fan conroller app as if it were a multi-speed fan device

	Copyright 2025 -> ChrisB 

    Version 1.0 - 12/19/25 - Forked from Fan Hood Child Driver

*/

metadata {
	definition (
        name: "Office Fan Child Driver",
        namespace: "Hubitat",
        author: "Chris B"
	) {
        capability "Fan Control"
        capability "Switch"
        capability "Actuator"

        attribute "speed", "ENUM"
        attribute "switch", "ENUM"   
        attribute "offMinutes", "ENUM"
        attribute "supportedFanSpeeds", "JSON_OBJECT"
        attribute "oscillate", "ENUM"

        command "setSpeed", [[name:"setSpeed",type:"ENUM", description:"Set Fan Speed", constraints:["off","low","medium","high"]]]
        command "setSpeedAttribute", [[name:"setSpeedAttribute",type:"ENUM", description:"Set Fan Speed Attribute Only", constraints:["off","low","medium","high"]]]
        command "setOscillate", [[name:"setOscillate",type:"ENUM", description:"Set Fan Oscillate", constraints:["off","on"]]]
        command "setOscillateAttribute", [[name:"setOscillateAttribute",type:"ENUM", description:"Set Fan Oscillate Attribute", constraints:["off","on"]]]       
        command "on"
        command "off"
        command "setOffMin", [[name:"offMinutes",type:"ENUM", description:"Set Off in Minutes", constraints:["5","10","15","20","25","30","35","40","45","60","90","120"]]]
        command "setOffInMin", [[name:"offInMin",type:"ENUM", description:"Set Off in Minutes", constraints:["X","5","10","15","20","25","30","35","40","45"]]]
}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input name: "autoOff", type: "bool", description: "Automatically turn fan off after selected time.", title: "Enable Auto-Off",  defaultValue: false
    }
}


def installed() {
	log.warn "installed..." 
    state.speed = "off"

    sendEvent(name: "speed", value: "off")
    sendEvent(name: "switch", value: "off")   
    sendEvent(name: "offMinutes", value: "60" )
    sendEvent(name: "supportedFanSpeeds", value: '["off","low","medium","high"]')
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

def setOscillate(status) {
    sendEvent(name: "oscillate", value: status, descriptionText: getDescriptionText("oscillate set to ${status}")) 
    parent?.setOscillate(status)
}

def parse(String description) { noCommands("parse") }

def on() {
    sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on")) 
    sendEvent(name: "speed", value: state?.speed, descriptionText: getDescriptionText("speed set to ${state?.speed}"))
    parent?.fanSpeedHandler(state?.speed)  //, state?.speed)
    //autoTurnOff()
}

def off() {
    //if (device.currentValue("speed") != "off") state.speed = device.currentValue("speed")
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch off")) 
    sendEvent(name: "speed", value: "off", descriptionText: getDescriptionText("speed set to off"))
    parent?.fanSpeedHandler("off")
    unschedule()
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def setSpeed(speed) {
    logDebug "setSpeed(${speed}) was called"
    if (speed == "off") {off()}
    else if (speed == "on") {on()}
    else if (speed == "low" || speed == "medium" || speed == "high") {  
        sendEvent(name: "speed", value: speed, descriptionText: getDescriptionText("speed set to ${speed}"))   
        state.speed = speed 
        parent?.fanSpeedHandler(speed) 
        if (device.currentValue("switch") == "off") {
            sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on"))
            parent?.fanSpeedHandler("on")
            //autoTurnOff()
        }          
        state.speed = speed
    } else if (speed == "auto") {
        def osc = device.currentValue("oscillate")
        if (osc == "on") {setOscillate("off")}
        if (osc == "off") {setOscillate("on")}
    }
}

def setSpeedAttribute(speed) {
 
    if (speed != "Off") {     
        if (device.currentValue("speed") != speed) {
            sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on"))
            sendEvent(name: "speed", value: speed, descriptionText: getDescriptionText("speed set to ${speed}"))
        }
    } else if (speed == "off") {
        off()
    }  
}

def setOscillateAttribute(osc) {
    if (osc == "off") {sendEvent(name: "oscillate", value: "off", descriptionText: getDescriptionText("oscillate off"))}
    if (osc == "on") {sendEvent(name: "oscillate", value: "on", descriptionText: getDescriptionText("oscillate on"))}
}

def setOffInMin(min) {
    logDebug "setOffInMin(${min}) was called"

    if (min == "X") {
        unschedule("autoTimerOff")
        unschedule("off")
        runIn(10800, off)
    } else if (device.currentValue("speed") != "off") {
        def sec = (min.toInteger() * 60)
        logDebug "Off in ${sec} seconds"
        runIn(sec, autoTimerOff)
    }
}

def autoTimerOff() {
    logDebug "autoFanOff() was called"
    if (device.currentValue("speed") != "off") {
        off()
    }
}

def autoTurnOff() {
    if (settings?.autoOff) {
        def secs = device.currentValue("offMinutes").toInteger() * 60
        runIn(secs, off)
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