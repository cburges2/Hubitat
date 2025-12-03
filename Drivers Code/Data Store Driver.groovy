/*
	Data Store Driver

	Copyright 2025 -> C. Burgess   https://raw.githubusercontent.com/cburges2/Hubitat/refs/heads/main/Drivers%20Code/Data%20Store%20Driver.groovy
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
	}
}


def installed() {
	log.warn "installed..." 
	initialize()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
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
