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

        command "setSpeed", [[name:"setSpeed",type:"ENUM", description:"Set Fan Speed", constraints:["off","low","medium-low","medium","medium-high","high"]]]
        command "setSupportedFanSpeeds", ["JSON_OBJECT"]
        command "on"
        command "off"
        command "setLevel", ["NUMBER"]
        command "setOffMin", [[name:"offInMin",type:"ENUM", description:"Set Off Minutes", constraints:["X","5","10","15","20","25","30","35","40","45"]]]
        command "initialize"
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

    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch off")) 
    sendEvent(name: "speed", value: "off", descriptionText: getDescriptionText("speed set to off"))
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch set to off"))
    sendEvent(name: "offMinutes", value: "30", descriptionText: getDescriptionText("offMinutes set to 30"))
    sendEvent(name: "supportedFanSpeeds", value: '["low","medium-low","medium","medium-high","high","on","off"]', descriptionText: getDescriptionText("supportedFanSpeeds set"))
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
    if (lvl > 0 && lvl < 20) {setSpeed("low")}
    if (lvl >= 20 && lvl < 40) {setSpeed("medium-low")}
    if (lvl >= 40 && lvl < 60) {setSpeed("medium")}
    if (lvl >= 60 && lvl < 80) {setSpeed("medium-high")}
    if (lvl >= 80) {setSpeed("high")}
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
    else if (speed == "low" || speed == "medium-low" || speed == "medium" || speed == "medium-high" || speed == "high") {  
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
    if (speed == "low") {level = 20}
    if (speed == "medium-low") {level = 40}
    if (speed == "medium") {level = 60}
    if (speed == "medium-high") {level = 80}
    if (speed == "high") {level = 100}
    sendEvent(name: "level", value: level, descriptionText: getDescriptionText("Level set to ${level}"))
}

def setSupportedFanSpeeds(json) {
    sendEvent(name: "supportedFanSpeeds", value: json, descriptionText: getDescriptionText("supportedFanSpeeds set to ${json}")) 
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