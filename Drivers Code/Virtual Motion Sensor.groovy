/*
	Virtual Motion Sensor

	Copyright 2023 -> C. Burgess

*/

metadata {
	definition (
			name: "Virtual Motion Sensor",
			namespace: "chrisb",
			author: "Chris B"
	) {

        capability "Motion Sensor"	//"active", "inactive"

		// Commands needed to change internal attributes of virtual device.
        command "setActive"
        command "setInactive"
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

    }	
}

def parse(String description) { noCommands("parse") }

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

// Commands needed to change internal attributes of virtual device.

def setActive() {
	logDebug "setActive was called"
    sendEvent(name: "motion", value: "active", descriptionText: getDescriptionText("motion set to active"))
    if (autoOff.toInteger()>0){
        runIn(autoOff.toInteger(),setInactive)
    }
}

def setInactive() {
	logDebug "setInactive was called"
    sendEvent(name: "motion", value: "inactive", descriptionText: getDescriptionText("motion set to inactive"))
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