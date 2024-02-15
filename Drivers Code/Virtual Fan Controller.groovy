/*
	Virtual Fan Controller

	Copyright 2023 -> C. Burgess

*/

metadata {
	definition (
			name: "Virtual Fan Controller",
			namespace: "chrisb",
			author: "Chris B"
	) {

        capability "FanControl"	   
        capability "Switch"
        
        attribute "speed", "ENUM"
        attribute "switch", "ENUM"
        attribute "oscillate", "ENUM"
        attribute "mode", "ENUM"
        attribute "lastSpeed", "ENUM"
        attribute "html", "STRING"

		// Commands needed to change internal attributes of virtual device
        command "setOscillate", [[name:"oscillate",type:"ENUM", description:"Oscillate", constraints:["on","off"]]]
        command "on"
        command "off"
        command "setMode", [[name:"mode",type:"ENUM", description:"mode", constraints:["normal","breeze","night"]]]
        command "setHtml"

}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
        input name: "autoOff", type: "enum", description: "Automatically sets contact closed after selected time.", title: "Enable Auto-inactive", options: [[0:"Disabled"],[1:"1s"],[2:"2s"],[5:"5s"],[10:"10s"],[20:"20s"],[30:"30s"],[60:"1m"],[120:"2m"],[300:"5m"],[600:"10m"],[1800:"30m"],[3200:"60m"]], defaultValue: 0
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

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def setSpeed(speed) {
    state.speed = device.currentValue("speed")
    if (speed == "medium-low") sendEvent(name: "mode", value: "normal", descriptionText: getDescriptionText("mode set to normal"))
    if (speed == "medium-high") sendEvent(name: "mode", value: "breeze", descriptionText: getDescriptionText("mode set to breeze"))
    if (speed == "off") off()
    if (speed == "on") on()
    if (speed != "off" && speed != "on" && speed !="medium-low" && speed !="medium-high" ) {
        sendEvent(name: "speed", value: speed, descriptionText: getDescriptionText("speed set to ${speed}"))              
        sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("swtich turned on"))  
    }    
    if (speed == "auto") {
        logDebug("Sending oscillate")      
        runIn(1, resetFanSpeed)
    }
    runIn(1,setHtml)
}

def resetFanSpeed() {
    def speed = state?.speed
    sendEvent(name: "speed", value: speed, descriptionText: getDescriptionText("speed reset to ${speed}"))
}

def off() {
    sendEvent(name: "lastSpeed", value: device.currentValue("speed"), descriptionText: getDescriptionText("lastSpeed set to ${speed}")) 
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("swtich turned off"))
    sendEvent(name: "speed", value: "off", descriptionText: getDescriptionText("speed set to off")) 
    runIn(1,setHtml)
}

def on() {
    sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("swtich turned on"))
    runIn(1,setHtml)
}

def setOscillate(set) {
    sendEvent(name: "oscillate", value: set, descriptionText: getDescriptionText("ocillate set to ${set}"))
    runIn(1,setHtml)
}

def setMode(mode) {
    sendEvent(name: "mode", value: mode, descriptionText: getDescriptionText("mode set to ${mode}"))
    runIn(1,setHtml)
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

def setHtml() {
    String html = "Power: "+device.currentValue("switch")+"<br>Speed: "+device.currentValue("speed")+"<br>Mode: "+device.currentValue("mode")+"<br>Oscillate: "+device.currentValue("oscillate")
    sendEvent(name: "html", value: html, descriptionText: getDescriptionText("html set to ${html}"))
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}