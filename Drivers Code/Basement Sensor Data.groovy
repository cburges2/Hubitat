/*

Data Storage Driver - Basement Sensor Data

v 1.0 - 3/20/23 - Set attributes for basement

*/
import java.text.SimpleDateFormat 
metadata {
	definition (
			name: "Basement Sensor Data",
			namespace: "hubitat",
			author: "Chris B."
	) { 
        
		capability "Actuator"

        //TYPES: ENUM, STRING, DYNAMIC_ENUM, JSON_OBJECT, NUMBER, DATE, VECTOR3
        attribute "theaterMotionStatus", "ENUM"
		attribute "officeMotionStatus", "ENUM"
		attribute "basementMotionStatus", "ENUM"
		attribute "basementHallMotionStatus", "ENUM"
        attribute "theaterMode","ENUM"  // theater, room
		attribute "officeMode", "ENUM"
        attribute "theaterSensor", "ENUM"
		attribute "officeSensor", "ENUM"
        attribute "theaterTimeoutMinutes", "ENUM"
        attribute "officeTimeoutMinutes", "ENUM"
        attribute "basementHallTimeoutMinutes", "ENUM"
        attribute "basementTimeoutMinutes", "ENUM"
		attribute "basementHallSensor", "ENUM"
		attribute "basementSensor", "ENUM"
		attribute "theaterTimeout", "ENUM"

		// Commands needed to change internal attributes of virtual device.
        command "setTheaterMotionStatus", [[name:"Theater Motion Status",type:"ENUM", description:"Set Theater Motion", constraints:["Active","Inactive","Timeout"]]]
		//command "setTheaterScheduledMode", [[name:"Theater Scheduled Mode",type:"ENUM", description:"Set Theater Scheduled Mode", constraints:["Dawn","Morning","Day","Evening","TV Time","Night","Dim","Bright"]]]
        command "setTheaterMode", [[name:"Theater Mode",type:"ENUM", description:"Set Theater Mode", constraints:["Screen", "Room", "Work"]]]
        command "setTheaterSensor", [[name:"Theater Sensor On/Off",type:"ENUM", description:"Set Theater Sensor", constraints:["On","Off"]]]
		command "setBasementHallSensor", [[name:"Basement Hall Sensor On/Off",type:"ENUM", description:"Set Baement Hall Sensor", constraints:["On","Off"]]]
		command "setBasementSensor", [[name:"Basement Sensor On/Off",type:"ENUM", description:"Set Baement Sensor", constraints:["On","Off"]]]
        command "setTheaterTimeoutMinutes",[[name:"theaterTimeoutMinutes",type:"ENUM", description:"Set Theater Timeout", constraints:["5","10","15","20","25","30","35","40","45","60","90","120"]]]
        command "setOfficeMotionStatus", [[name:"Office Motion Status",type:"ENUM", description:"Set Office Motion", constraints:["Active","Inactive","Timeout"]]]
		command "setBasementMotionStatus", [[name:"Basement Motion Status",type:"ENUM", description:"Set Basement Motion", constraints:["Active","Inactive","Timeout"]]]
		command "setBasementHallMotionStatus", [[name:"Basement Hall Motion Status",type:"ENUM", description:"Set Basement Hall Motion", constraints:["Active","Inactive","Timeout"]]]
		//command "setOfficeScheduledMode", [[name:"Office Scheduled Mode",type:"ENUM", description:"Set Office Scheduled Mode", constraints:["Dawn","Morning","Day","Evening","TV Time","Night","Dim","Bright"]]]
        command "setOfficeMode", [[name:"Office Mode",type:"ENUM", description:"Set Office Mode", constraints:["Dim","Work"]]]
        command "setOfficeSensor", [[name:"Office Sensor On/Off",type:"ENUM", description:"Set Office Sensor", constraints:["On","Off"]]]
        command "setOfficeTimeoutMinutes",[[name:"officeTimeoutMinutes",type:"ENUM", description:"Set Office Timeout", constraints:["5","10","15","20","25","30","35","40","45"]]]	
		command "setBasementTimeoutMinutes",[[name:"basementTimeoutMinutes",type:"ENUM", description:"Set basement Timeout", constraints:["5","10","15","20","25","30","35","40","45"]]]
		command "setBasementHallTimeoutMinutes",[[name:"basementHallTimeoutMinutes",type:"ENUM", description:"Set Basement Hall Timeout", constraints:["5","10","15","20","25","30","35","40","45"]]]
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

def setTheaterTimeout() {
    logDebug "setTheaterTimout() was called"
    Date date = new Date();   // current date
    Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
    calendar.setTime(date);   // assigns calendar to current date/time 
    def interval = device.currentValue("theaterTimeoutMinutes").toInteger()
    calendar.add(Calendar.MINUTE, interval)
	Date newDate = calendar.getTime()
	newTime = new SimpleDateFormat("hh:mm a").format(newDate)
	logDebug "New Time is ${newTime}"
	if (device.currentValue("theaterSensor") == "On") sendEvent(name: "theaterTimeout", value: newTime, descriptionText: getDescriptionText("theatertimeout set to ${newTime}")) 
      else sendEvent(name: "theaterTimeout", value: "--:-- --", descriptionText: getDescriptionText("theatertimeout set to --:-- --"))
}

def setTheaterMotionStatus(value) {
    logDebug "setTheaterMotionStatus(${value}) was called"	
	if (value == "Active") setTheaterMotionActive()		
	else if (value == "Inactive") sendEvent(name: "theaterMotionStatus", value: "Inactive", descriptionText: getDescriptionText("theaterMotionStatus was set to Inactive"),stateChange: true)
	else if (value == "Timeout") runIn(1,setTheaterMotionTimeout)   
}

def setTheaterMotionActive() {
	logDebug "setTheaterMotionActive() was called"
	sendEvent(name: "theaterMotionStatus", value: "Active", descriptionText: getDescriptionText("theaterMotionStatus was set to Active"),stateChange: true)
	def timeoutMinutes = device.currentValue("theaterTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)
	logDebug "theaterMotionStatus Timeout in (${sec})"
	setTheaterTimeout()
	runIn(sec, setTheaterMotionTimeout)
}

def setTheaterMotionTimeout() {
	logDebug "setTheaterMotionTimeout() was called"
	sendEvent(name: "theaterMotionStatus", value: "Timeout", descriptionText: getDescriptionText("theaterMotionStatus was set to Timeout"),stateChange: true)
}

def setOfficeMotionStatus(value) {
    logDebug "setOfficeMotionStatus(${value}) was called"	
	if (value == "Active") setOfficeMotionActive()
	else if (value == "Inactive") sendEvent(name: "officeMotionStatus", value: "Inactive", descriptionText: getDescriptionText("officeMotionStatus was set to Inactive"),stateChange: true)
	else if (value == "Timeout") runIn(1,setOfficeMotionTimeout)
}

def setOfficeMotionActive() {
	logDebug "setOfficeMotionActive() was called"
	sendEvent(name: "officeMotionStatus", value: "Active", descriptionText: getDescriptionText("officeMotionStatus was set to Active"),stateChange: true)			
	def timeoutMinutes = device.currentValue("officeTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)
	logDebug "officeMotionStatus Timeout in (${sec})"
	runIn(sec, setOfficeMotionTimeout)
}

def setOfficeMotionTimeout() {
	logDebug "setOfficeMotionTimeout() was called"
	sendEvent(name: "officeMotionStatus", value: "Timeout", descriptionText: getDescriptionText("officeMotionStatus was set to Timeout"),stateChange: true)
}

def setBasementMotionStatus(value) {
    logDebug "setBasementMotionStatus(${value}) was called"	
	if (value == "Active") setBasementMotionActive()
	else if (value == "Inactive") sendEvent(name: "basementMotionStatus", value: "Inactive", descriptionText: getDescriptionText("basementMotionStatus was set to Inactive}"),stateChange: true)
	else if (value == "Timeout") runIn(1, setBasementMotionTimeout)
}

def setBasementMotionActive() {
	logDebug "setBasementMotionActive() was called"
	sendEvent(name: "basementMotionStatus", value: "Active", descriptionText: getDescriptionText("basementMotionStatus was set to Active"),stateChange: true)
	def timeoutMinutes = device.currentValue("basementTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)
	logDebug "basementMotionStatus Timeout in (${sec})"
	runIn(sec, setBasementMotionTimeout)
}

def setBasementMotionTimeout() {
	logDebug "setBasementMotionTimeout() was called"
	sendEvent(name: "basementMotionStatus", value: "Timeout", descriptionText: getDescriptionText("basementMotionStatus was set to Timeout"),stateChange: true)
}

def setBasementHallMotionStatus(value) {
    logDebug "setBasementHallMotionStatus(${value}) was called"
	if (value == "Active") setBasementHallMotionActive()
	else if (value == "Inactive") sendEvent(name: "basementHallMotionStatus", value: "Inactive", descriptionText: getDescriptionText("basementHallMotionStatus was set to Inactive"),stateChange: true)
	else if (value == "Timeout") runIn(1,setBasementeHallMotionTimeout)
}

def setBasementHallMotionActive() {
	logDebug "setBasementHallMotionActive() was called"
	sendEvent(name: "basementHallMotionStatus", value: "Active", descriptionText: getDescriptionText("basementHallMotionStatus was set to Active"),stateChange: true)
	def timeoutMinutes = device.currentValue("basementHallTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)
	logDebug "basementHallMotionStatus Timeout in (${sec})"
	runIn(sec, setBasementHallMotionTimeout)
}

def setBasementHallMotionTimeout() {
	logDebug "setBasementHallMotionTimeout() was called"
	sendEvent(name: "basementHallMotionStatus", value: "Timeout", descriptionText: getDescriptionText("basementHallMotionStatus was set to Timeout"),stateChange: true)
}

def setTheaterMode(mode) {
	logDebug "setTheaterMode(${mode}) was called"
	sendEvent(name: "theaterMode", value: mode, descriptionText: getDescriptionText("theaterMode was set to ${mode}"),stateChange: true)
}

def setOfficeMode(mode) {
	logDebug "setOfficeMode(${mode}) was called"
	sendEvent(name: "officeMode", value: mode, descriptionText: getDescriptionText("officeMode was set to ${mode}"),stateChange: true)
}

def setTheaterSensor(value) {
	logDebug "setTheaterSensor(${value}) was called"
	sendEvent(name: "theaterSensor", value: value, descriptionText: getDescriptionText("theaterSensor was set to ${value}"),stateChange: true)
	if (value == "Off") sendEvent(name: "theaterTimeout", value: "--:-- --", descriptionText: getDescriptionText("theaterTimeout was set to --:-- --"),stateChange: true)
	else runIn(1, setTheaterTimeout)
}

def setOfficeSensor(value) {
	logDebug "setOfficeSensor(${value}) was called"
	sendEvent(name: "officeSensor", value: value, descriptionText: getDescriptionText("officeSensor was set to ${value}"),stateChange: true)
}

def setBasementHallSensor(value) {
	logDebug "setBasementHallSensor(${value}) was called"
	sendEvent(name: "basementHallSensor", value: value, descriptionText: getDescriptionText("basementHallSensor was set to ${value}"),stateChange: true)
}

def setBasementSensor(value) {
	logDebug "setBasementSensor(${value}) was called"
	sendEvent(name: "basementSensor", value: value, descriptionText: getDescriptionText("basementSensor was set to ${value}"),stateChange: true)
}

def setTheaterTimeoutMinutes(value) {
	logDebug "setTheaterTimeoutMinutes(${value}) was called"
	sendEvent(name: "theaterTimeoutMinutes", value: value, descriptionText: getDescriptionText("theaterTimeoutMinutes was set to ${value}"),stateChange: true)
}

def setOfficeTimeoutMinutes(value) {
	logDebug "setOfficeTimeoutMinutes(${value}) was called"
	sendEvent(name: "officeTimeoutMinutes", value: value, descriptionText: getDescriptionText("officeTimeoutMinutes was set to ${value}"),stateChange: true)
}

def setBasementTimeoutMinutes(value) {
	logDebug "setBasementTimeoutMinutes(${value}) was called"
	sendEvent(name: "basementTimeoutMinutes", value: value, descriptionText: getDescriptionText("basementTimeoutMinutes was set to ${value}"),stateChange: true)
}

def setBasementHallTimeoutMinutes(value) {
	logDebug "setBasementHallTimeoutMinutes(${value}) was called"
	sendEvent(name: "basementHallTimeoutMinutes", value: value, descriptionText: getDescriptionText("basementHallTimeoutMinutes was set to ${value}"),stateChange: true)
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