/*
	Data Store Driver

	Copyright 2025 -> C. Burgess
*/

metadata {
	definition (
			name: "Data Store Driver",
			namespace: "chrisb",
			author: "Chris B"
	) {
		capability "Actuator"


        attribute "string1", "STRING"
        attribute "string2", "STRING"
        attribute "number1", "NUMBER"
        attribute "number1", "NUMBER"
        

		// Commands needed to change internal attributes of virtual device.
        command "setStringAttribute", [[name:"name",type:"ENUM", description:"Set String Attribute Name", constraints:["string1","string2"]],[name:"value",type:"STRING", description:"Set Attribute value"]]
        command "setNumberAttribute", [[name:"name",type:"ENUM", description:"Set Number Attribute Name", constraints:["number1","number2"]],[name:"value",type:"NUMBER", description:"Set Attribute value"]]
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
}

def parse(String description) { noCommands("parse") }

def setNumberAttribute(name, value) {
    setAttribute(name, value)
}

def setStringAttribute(name, value) {
    setAttribute(name, value)
}

def setAttribute(name, value) {
    sendEvent(name: name, value: value)
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}