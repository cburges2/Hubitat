/*
	Virtual Fan Hood Driver 

	Copyright 2025 -> ChrisB 

    Version 1.0 - 11/14/24

*/

metadata {
	definition (
        name: "Virtual Fan Driver",
        namespace: "chrisb",
        author: "Chris B"
	) {
        capability "Fan Control"
        capability "Switch"
        capability "Actuator"
        capability "SwitchLevel"

        attribute "speed", "ENUM"
        attribute "switch", "ENUM"   
        attribute "offMinutes", "ENUM"
        attribute "supportedFanSpeeds", "JSON_OBJECT"
        attribute "level", "ENUM"

        command "setSpeed", [[name:"setSpeed",type:"ENUM", description:"Set Fan Speed", constraints:["off","low","medium","high"]]]
        command "setSupportedFanSpeeds", ["JSON_OBJECT"]
        command "on"
        command "off"
        command "setLevel", ["NUMBER"]
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
    sendEvent(name: "speed", value: state?.speed, descriptionText: getDescriptionText("speed set to ${state?.speed}"))
    autoTurnOff()
}

def off() {
    state.speed = device.currentValue("speed")
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch off")) 
    sendEvent(name: "speed", value: "off", descriptionText: getDescriptionText("speed set to off"))
    unschedule()
}

def setLevel(level) {

    def lvl = level.toInteger()
    if (lvl == 0) {setSpeed("off")}
    if (lvl > 0 && lvl < 34) {setSpeed("low")}
    if (lvl >= 34 && lvl < 67) {setSpeed("medium")}
    if (lvl >= 67) {setSpeed("high")}
    sendEvent(name: "level", value: level, descriptionText: getDescriptionText("Level set to ${level}")) 
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
        if (device.currentValue("switch") == "off") {
            sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on"))
            autoTurnOff()
        }          
        state.speed = speed
    }
    setSpeedLevel(speed)
}

def setSpeedLevel(speed) {

    def level = 0
    if (speed == "low") {level = 33}
    if (speed == "medium") {level = 66}
    if (speed == "high") {level = 100}
    sendEvent(name: "level", value: level, descriptionText: getDescriptionText("Level set to ${level}"))
}

def setSupportedFanSpeeds(json) {
    sendEvent(name: "supportedFanSpeeds", value: json, descriptionText: getDescriptionText("supportedFanSpeeds set to ${json}")) 
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