/*
	USB Signal Light Child Device

	Copyright 2026 -> C. Burgess

	Control a USB three port switch with LED indicators plugged into the outputs

*/

metadata {
    definition (
            name: "USB Signal Light Child Device",
            namespace: "Hubitat",
            author: "Chris B"
    ) {

		capability "Actuator"
		capability "Switch"

		attribute "blue", "ENUM"
		attribute "green", "ENUM"
		attribute "red", "ENUM"
		attribute "status", "ENUM"
		attribute "switch", "ENUM"

		// Commands 
		command "setBlue", [[name:"setBlue",type:"ENUM", description:"Set Blue Light", constraints:["on","off"]]]
		command "setGreen", [[name:"setGreen",type:"ENUM", description:"Set Green Light", constraints:["on","off"]]]
		command "setRed", [[name:"setRed",type:"ENUM", description:"Set Red Light", constraints:["on","off"]]]
		command "setAColor", [[name:"color",type:"ENUM", description:"Color to Turn On or Off", constraints:["red","green","blue"]],[name:"status",type:"ENUM", description:"Switch State", constraints:["on","off"]]]
		command "toggleBlue"
		command "toggleGreen"
		command "toggleRed"
		command "on"
		command "off"
	} 
        
    preferences {
        input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
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

def setBlue(status) {setAColor("blue", status)}
def setGreen(status) {setAColor("green", status)}
def setRed(status) {setAColor("red", status)}

// call to parent
def setAColor(color, status) {
	parent?.setColor(color,status)
	sendEvent(name: color, value: status)
	runIn(1,setStatus)
	                                
}

def toggleBlue() {
	toggle("blue")
}

def toggleRed() {
	toggle("red")
}

def toggleGreen() {
	toggle("green")
}

def toggle(color) {	
	if (device.currentValue(color) == "on") {
		sendEvent(name: color, value: "off")
		parent?.setColor(color, "off")
	}
	if (device.currentValue(color) == "off") {
		sendEvent(name: color, value: "on")
		parent?.setColor(color, "on")
	}
	runIn(1,setStatus)
}

def on() {
	parent?.allOn()
	setAllColors("on")
	sendEvent(name: "switch", value: "on")
}

def off() {
	parent?.allOff()
	setAllColors("off")
	sendEvent(name: "switch", value: "off")
}

def setAllColors(status) {
	sendEvent(name: "green", value: status)
	sendEvent(name: "red", value: status)
	sendEvent(name: "blue", value: status)
	runIn(1, setStatus)
}

def setStatus() {
	def status = ""
	if (device.currentValue("red") == "on") {status = status + "Red  "}
	if (device.currentValue("green") == "on") {status = status + "Green  "}
	if (device.currentValue("blue") == "on") {status = status + "Blue"}
	if (status == "") {status = "off"}
	sendEvent(name: "status", value: status)
	setSwitch()
}

def setSwitch() {
	if (device.currentValue("blue") == "on" || device.currentValue("green") == "on" || device.currentValue("red") == "on") {sendEvent(name: "switch", value: "on")}
	if (device.currentValue("blue") == "off" && device.currentValue("green") == "off" && device.currentValue("red") == "off") {sendEvent(name: "switch", value: "off")}
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