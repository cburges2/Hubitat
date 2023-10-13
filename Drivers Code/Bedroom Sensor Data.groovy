/*

Data Storage Driver - Bedroom Sensor Data

v 1.0 - 12/22 - Set attributes for bedroom motion status
v.2.0 - 2/23 - Added auto timeout for motion status

*/

metadata {
	definition (
			name: "Bedroom Sensor Data",
			namespace: "hubitat",
			author: "Chris B."
	) { 
        
		capability "Actuator"

        //TYPES: ENUM, STRING, DYNAMIC_ENUM, JSON_OBJECT, NUMBER, DATE, VECTOR3
        attribute "bedroomMotionStatus", "ENUM"
        attribute "bedroomScheduledMode", "ENUM"
        attribute "bedroomMode","ENUM"
        attribute "bedroomSensor", "ENUM"
        attribute "bedroomTimeoutMinutes", "NUMBER"
        attribute "doorWatch", "ENUM"
        attribute "napTime", "ENUM"
        attribute "backDoorSensor", "ENUM"
        attribute "bedroomACStatus", "ENUM"
		attribute "tvPower", "ENUM"

		// Commands needed to change internal attributes of virtual device.
        command "setBedroomMotionStatus", [[name:"Bedroom Motion Status",type:"ENUM", description:"Set Bedroom Motion", constraints:["Active","Inactive","Timeout"]]]
		command "setBedroomScheduledMode", [[name:"Bedroom Scheduled Mode",type:"ENUM", description:"Set Bedroom Scheduled Mode", constraints:["Dawn","Morning","Day","Evening","TV Time","Night","Dim","Bright"]]]
        command "setBedroomMode", [[name:"Bedroom Mode",type:"ENUM", description:"Set Bedroom Mode", constraints:["Dawn","Morning","Day","Evening","TV Time","Night","Dim","Bright"]]]
        command "setBedroomSensor", [[name:"Bedroom Sensor On/Off",type:"ENUM", description:"Set Bedroom Sensor", constraints:["On","Off"]]]
        command "setBedroomTimeoutMinutes",[[name:"Bedroom Timout Minutes",type:"NUMBER"]]
        command "setDoorWatch", [[name:"Door Watch",type:"ENUM", description:"Set Door Watch", constraints:["Yes","No"]]]
        command "setNapTime", [[name:"Nap Time Yes/No",type:"ENUM", description:"Set Nap Time", constraints:["Yes","No"]]]
        command "setBackDoorSensor", [[name:"Back Door Sensor On/Off",type:"ENUM", description:"Set Back Door Sensor", constraints:["On","Off"]]]
        command "setBedroomACStatus", [[name:"Bedroom AC Status",type:"ENUM", description:"Set Bedroom AC Status", constraints:["Off","Fan","Cool"]]]
		command "setTvPower", [[name:"TV Power",type:"ENUM", description:"Set TV Power State", constraints:["On","Off"]]]

	}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)      
	}
}


def installed() {
	log.warn "installed..."
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

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

// Commands needed to change internal attributes of virtual device.

def setBedroomMotionStatus(value) {
    logDebug "setBedroomMotionStatus(${value}) was called"	
    if (value == "Inactive" || value == "Active") {
		sendEvent(name: "bedroomMotionStatus", value: value, descriptionText: getDescriptionText("bedroomMotionStatus was set to ${value}"),stateChange: true)
		if (((device.currentValue("bedroomTimeoutMinutes")).toInteger())*60>0){
	        runIn(((device.currentValue("bedroomTimeoutMinutes")).toInteger())*60,setBedroomMotionTimeout)
	    }
	} else if (value == "Timeout") runIn(1, setBedroomMotionTimeout)
}

def setBedroomMotionTimeout() {
	logDebug "setBedroomMotionTimeout() was called"
	sendEvent(name: "bedroomMotionStatus", value: "Timeout", descriptionText: getDescriptionText("bedroomMotionStatus was set to Timeout"),stateChange: true)
}

def setBedroomScheduledMode(value) {
	logDebug "setBedroomScheduledMode(${value}) was called"
	sendEvent(name: "bedroomScheduledMode", value: value, descriptionText: getDescriptionText("bedroomScheduledMode was set to ${value}"),stateChange: true)
}

def setBedroomMode(mode) {
	logDebug "setBedroomMode(${mode}) was called"
	sendEvent(name: "bedroomMode", value: mode, descriptionText: getDescriptionText("bedroomMode was set to ${mode}"),stateChange: true)
}

def setBedroomSensor(value) {
	logDebug "setBedroomSensor(${value}) was called"
	sendEvent(name: "bedroomSensor", value: value, descriptionText: getDescriptionText("bedroomSensor was set to ${value}"),stateChange: true)
}

def setBedroomTimeoutMinutes(value) {
	logDebug "setBedroomTimeoutMinutes(${value}) was called"
	sendEvent(name: "bedroomTimeoutMinutes", value: value, descriptionText: getDescriptionText("setBedroomTimeoutMinutes was set to ${value}"),stateChange: true)
}

def setDoorWatch(status) {
	logDebug "setDoorWatch(${status}) was called"
	sendEvent(name: "doorWatch", value: status, descriptionText: getDescriptionText("doorWatch was set to ${status}"),stateChange: true)
}

def setNapTime(status) {
	logDebug "setNapTime(${status}) was called"
	sendEvent(name: "napTime", value: status, descriptionText: getDescriptionText("napTime was set to ${status}"),stateChange: true)
}

def setBackDoorSensor(status) {
	logDebug "setBackDoorSensor(${status}) was called"
	sendEvent(name: "backDoorSensor", value: status, descriptionText: getDescriptionText("backDoorSensor was set to ${status}"),stateChange: true)
}

def setBedroomACStatus(status) {
	logDebug "setBedroomACStatus(${status}) was called"
	sendEvent(name: "bedroomACStatus", value: status, descriptionText: getDescriptionText("bedroomACStatus was set to ${status}"),stateChange: true)
}

def setTvPower(value) {
	logDebug "setTvPower(${value}) was called"
	sendEvent(name: "tvPower", value: value, descriptionText: getDescriptionText("tvpower was set to ${value}"),stateChange: true)
}

def parse(String description) {
	logDebug "$description"
}


private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}