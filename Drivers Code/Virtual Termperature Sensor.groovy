/*
	Virtual Temperature Sensor

	Copyright 2024 -> C. Burgess

*/

metadata {
	definition (
			name: "Virtual Temperature Sensor",
			namespace: "chrisb",
			author: "Chris B"
	) {

		capability "TemperatureMeasurement"

        attribute "temperature", "ENUM"

		// Commands needed to change internal attributes of virtual device.
        command "setTemperature", ["ENUM"]
}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
        input( name: "tempAdjust", type: "ENUM", title: "Temp Adjust Amount", defaultValue: 0)
        //input( name: "adjustTemp", type: "ENUM", title: "Calc Temp Adjust from this Temperature", defaultValue: null)
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

    def sensorTemp = state?.sensorTemp.toBigDecimal()
    setTemperature(sensorTemp)
    
	initialize()
}

def initialize() {
	if (state?.lastRunningMode == null) {    

    }	
}

def parse(String description) { noCommands("parse") }

def setTemperature(temp) {
	logDebug "setTemperature was called"
    def inTemp = temp.toBigDecimal()
    def tempAdjust = settings?.tempAdjust.toBigDecimal()
    state.sensorTemp = temp
    def newTemp = inTemp + tempAdjust
    sendEvent(name: "temperature", value: newTemp, descriptionText: getDescriptionText("temperature set to ${newTemp} from ${temp}"))
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}