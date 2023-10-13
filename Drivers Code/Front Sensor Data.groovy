/*

Data Storage Driver - Front Sensor Data

This driver stores data as attributes to set and get like a global variable.
Auto sets motion status attributes to Timeout in minutes set in timeoutMinutes attributes.

v 1.0 - 12/22 - Set-up all needed attribute names
v 2.0 - 2/23 - Added auto Timeout for Motion Status attributes

*/

metadata {
	definition (
			name: "Front Sensor Data",
			namespace: "hubitat",
			author: "Chris B."
	) { 
      
		capability "Actuator"
        
        // Internal attributes needed to store data in virtual device
        //TYPES: ENUM, STRING, DYNAMIC_ENUM, JSON_OBJECT, NUMBER, DATE, VECTOR3
		attribute "druHome", "ENUM"
		attribute "dinnerTime", "STRING"
        attribute "frontMotionStatus", "ENUM"
        attribute "frontScheduledMode", "ENUM"
        attribute "frontMode","ENUM"
        attribute "frontSensor", "ENUM"
        attribute "frontTimeoutMinutes", "ENUM"
        attribute "kitchenActivityStatus", "ENUM"
        attribute "kitchenActivityTimeoutMin", "ENUM"
        attribute "kitchenMotionStatus", "ENUM"
        attribute "livingRoomMotionStatus", "ENUM"
        attribute "frontActivity", "STRING"
        attribute "readingLight", "ENUM"
        //attribute "xmasLights", "ENUM"
        //attribute "treeColor", "ENUM"
        //attribute "candlesOnly", "ENUM"
        attribute "mailStatus", "ENUM"
        attribute "onVacation", "ENUM"
        attribute "frontACPause", "ENUM"
        attribute "sceneSwitch", "ENUM"

		// Commands needed to change internal attributes of virtual device.
        command "setDruHome", [[name:"druHome",type:"ENUM", description:"Set Dru Home", constraints:["Yes","No"]]]
        command "setDinnerTime",["STRING"]
        command "setFrontMotionStatus", [[name:"frontMotionStatus",type:"ENUM", description:"Set Front Motion", constraints:["Active","Inactive","Timeout"]]]
		command "setFrontScheduledMode", [[name:"frontScheduledMode",type:"ENUM", description:"Set Front Scheduled Mode", constraints:["Dawn","Morning","Day","Evening","Dinner","Late Evening","TV Time","Night","Dim","Bright"]]]
        command "setFrontMode", [[name:"frontMode",type:"ENUM", description:"Set Front Mode", constraints:["Dawn","Morning","Day","Evening","Dinner","Late Evening","TV Time","Night","Dim","Bright"]]]
        command "setFrontSensor", [[name:"frontSensor",type:"ENUM", description:"Set Front Sensor", constraints:["On","Off"]]]
        command "setFrontTimeoutMinutes", [[name:"frontMotionTimeout",type:"ENUM", description:"Set Front Motion Timeout", constraints:["5","10","15","20","25","30","35","40","45"]]]
        command "setKitchenActivityStatus", [[name:"kitchenActivityStatus",type:"ENUM", description:"Set Kitchen Activity", constraints:["Active","Inactive","Timeout"]]]
        command "setKitchenActivityTimeoutMin", [[name:"kitchenActivityTimeoutMin",type:"ENUM", description:"Set Kitchen Activity Timeout", constraints:["5","10","15","20","25","30","35","40","45"]]]
        command "setKitchenMotionStatus", [[name:"kitchenMotionStatus",type:"ENUM", description:"Set Kitchen Motion", constraints:["Active","Inactive","Timeout"]]]
        command "setLivingRoomMotionStatus", [[name:"livingRoomMotionStatus",type:"ENUM", description:"Set Living Room Motion", constraints:["Active","Inactive","Timeout"]]]
        command "setFrontActivity", ["STRING"]
        command "setReadingLight", [[name:"readingLight",type:"ENUM", description:"Set Reading Light", constraints:["true","false"]]]
        //command "setXmasLights", [[name:"xmasLights",type:"ENUM", description:"Set xmasLights", constraints:["Yes","No"]]]
        //command "setTreeColor", [[name:"treeColor",type:"ENUM", description:"Set treeColor", constraints:["Colored","White"]]]
        //command "setCandlesOnly", [[name:"candlesOnly",type:"ENUM", description:"Set Candles Only", constraints:["true","false"]]]
        command "setMailStatus", [[name:"mailStatus",type:"ENUM", description:"Set Mail Status", constraints:["true","false"]]]
        command "setOnVacation", [[name:"onVacation",type:"ENUM", description:"Set onVacation", constraints:["Yes","No"]]]
        command "setFrontACPause", [[name:"frontACPause",type:"ENUM", description:"Set frontACPause", constraints:["Yes","No"]]]
        command "setSceneSwitch", [[name:"sceneSwitch",type:"ENUM", description:"Set Scene Switch", constraints:["on","off"]]]
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

// Command methods needed to change internal attributes of virtual device.
def setDruHome(value) {
    logDebug "setDruHome(${value}) was called"
    sendEvent(name: "druHome", value: value, descriptionText: getDescriptionText("druHome was set to ${value}"),stateChange: true)
}

def setDinnerTime(value) {
	logDebug "setDinnerTime(${value}) was called"
	sendEvent(name: "dinnerTime", value: value, descriptionText: getDescriptionText("dinnerTime was set to ${value}"),stateChange: true)
}

def setFrontMotionStatus(status) {
	logDebug "setFrontMotionStatus(${status}) was called"
	sendEvent(name: "frontMotionStatus", value: status, descriptionText: getDescriptionText("frontMotionStatus was set to ${status}"),stateChange: true)	
    if (status == "Active") {
        def sec = 3600   // one hour
        logDebug "frontMotionyStatus Timeout in (${sec})"
	    runIn(sec, setFrontMotionTimeout)
	}       
}

def setFrontMotionTimeout() {
    logDebug "setFrontMotionTimeout() was called"
    if (device.currentValue("frontMotionStatus") == "Inactive") { 
	    sendEvent(name: "frontMotionStatus", value: "Timeout", descriptionText: getDescriptionText("frontMotionStatus was set to Timeout"),stateChange: true)
    }
}

def setFrontScheduledMode(value) {
	logDebug "setFrontScheduledMode(${value}) was called"
	sendEvent(name: "frontScheduledMode", value: value, descriptionText: getDescriptionText("frontScheduledMode was set to ${value}"),stateChange: true)
}

def setFrontMode(mode) {
	logDebug "setFrontMode(${mode}) was called"
	sendEvent(name: "frontMode", value: mode, descriptionText: getDescriptionText("frontMode was set to ${mode}"),stateChange: true)
}

def setFrontSensor(value) {
	logDebug "setFrontSensor(${value}) was called"
	sendEvent(name: "frontSensor", value: value, descriptionText: getDescriptionText("frontSensor was set to ${value}"),stateChange: true)
}

def setFrontTimeoutMinutes(min) {
	logDebug "setFrontTimeoutMinutes(${min}) was called"
	sendEvent(name: "frontTimeoutMinutes", value: min, descriptionText: getDescriptionText("frontTimeoutMinutes was set to ${min}"),stateChange: true)
}

def setKitchenActivityStatus(status) {
	logDebug "setKitchenActivityStatus(${status}) was called"	
	if (status == "Active") setKitchenActivityActive()
	else if (status == "Inactive") sendEvent(name: "kitchenActivityStatus", value: "Inactive", descriptionText: getDescriptionText("kitchenActivityStatus was set to Inactive"),stateChange: true)
	else if (status == "Timeout") runIn(1,setKitchenActivityTimeout)
}

def setKitchenActivityActive() {
	logDebug "setKitchenActivityActive() was called"
	sendEvent(name: "kitchenActivityStatus", value: "Active", descriptionText: getDescriptionText("kitchenActivityStatus was set to Timeout"),stateChange: true)
	def timeoutMinutes = device.currentValue("kitchenActivityTimeoutMin").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)
	logDebug "KitchenActivityStatus Timeout in (${sec})"
	runIn(sec, setKitchenActivityTimeout)
}

def setKitchenActivityTimeout() {
	logDebug "setKitchenActivityTimeout() was called"
	sendEvent(name: "kitchenActivityStatus", value: "Timeout", descriptionText: getDescriptionText("kitchenActivityStatus was set to Timeout"),stateChange: true)
}

def setKitchenActivityTimeoutMin(min) {
	logDebug "setKitchenActivityTimeoutMin(${min}) was called"
	sendEvent(name: "kitchenActivityTimeoutMin", value: min, descriptionText: getDescriptionText("kitchenActivityTimeoutMin was set to ${min}"),stateChange: true)
}

def setKitchenMotionStatus(status) {
	logDebug "setKitchenMotionStatus(${status}) was called"	
	if (status == "Active") setKitchenMotionActive()
	else if (status == "Inactive") sendEvent(name: "kitchenMotionStatus", value: "Inactive", descriptionText: getDescriptionText("kitchenMotionStatus was set to Inactive"),stateChange: true)
	else if (status == "Timeout") runIn(1, setKitchenMotionTimeout)
}

def setKitchenMotionActive() {
	logDebug "setKitchenMotionActive() was called"
    sendEvent(name: "kitchenMotionStatus", value: "Active", descriptionText: getDescriptionText("kitchenMotionStatus was set to Active"),stateChange: true)
	def timeoutMinutes = device.currentValue("frontTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)			
	setFrontMotionStatus("Active")
	runIn(sec, setKitchenMotionTimeout)
	logDebug "KitchenMotionStatus Timeout in (${sec})"	
}

def setKitchenMotionTimeout() {
	logDebug "setKitchenMotionTimeout() was called"
    sendEvent(name: "kitchenMotionStatus", value: "Timeout", descriptionText: getDescriptionText("kitchenMotionStatus was set to Timeout"),stateChange: true)
}
                                 
def setLivingRoomMotionStatus(status) {
	logDebug "setLivingRoomMotionStatus(${status}) was called"
    if (status == "Active") setLivingRoomMotionActive()
	else if (status == "Inactive") sendEvent(name: "livingRoomMotionStatus", value: "Inactive", descriptionText: getDescriptionText("livingRoomMotionStatus was set to Inactive"),stateChange: true)	
	else if (status == "Timeout") runIn(1,setLivingRoomMotionTimeout)
}

def setLivingRoomMotionActive() {
	logDebug "setLivingRoomMotionActive() was called"
    sendEvent(name: "livingRoomMotionStatus", value: "Active", descriptionText: getDescriptionText("LivingRoomMotionStatus was set to Active"),stateChange: true)
	def timeoutMinutes = device.currentValue("frontTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)	
	setFrontMotionStatus("Active")
	logDebug "LivingRoomMotionStatus Timeout in (${sec})"
	runIn(sec, setLivingRoomMotionTimeout)
}

def setLivingRoomMotionTimeout() {
	logDebug "setLivingRoomMotionTimeout() was called"
	sendEvent(name: "livingRoomMotionStatus", value: "Timeout", descriptionText: getDescriptionText("LivingRoomMotionStatus was set to Timeout"),stateChange: true)
}

def setFrontActivity(status) {
	logDebug "setFrontActivity(${status}) was called"
	sendEvent(name: "frontActivity", value: status, descriptionText: getDescriptionText("frontActivity was set to ${status}"),stateChange: true)
}

def setReadingLight(status) {
	logDebug "setReadingLight(${status}) was called"
	sendEvent(name: "readingLight", value: status, descriptionText: getDescriptionText("readingLight was set to ${status}"),stateChange: true)
}
/*
def setXmasLights(status) {
	logDebug "setXmasLights(${status}) was called"
	sendEvent(name: "xmasLights", value: status, descriptionText: getDescriptionText("xmasLights was set to ${status}"),stateChange: true)
}

def setTreeColor(status) {
	logDebug "setTreeColor(${status}) was called"
	sendEvent(name: "treeColor", value: status, descriptionText: getDescriptionText("treeColor was set to ${status}"),stateChange: true)
}

def setCandlesOnly(status) {
	logDebug "setCandlesOnly(${status}) was called"
	sendEvent(name: "candlesOnly", value: status, descriptionText: getDescriptionText("candlesOnly was set to ${status}"),stateChange: true)
}
*/ 

def setMailStatus(status) {
	logDebug "setMailStatus(${status}) was called"
	sendEvent(name: "mailStatus", value: status, descriptionText: getDescriptionText("mailStatus was set to ${status}"),stateChange: true)
}

def setOnVacation(status) {
	logDebug "setOnVacation(${status}) was called"
	sendEvent(name: "onVacation", value: status, descriptionText: getDescriptionText("onVacation was set to ${status}"),stateChange: true)
}

def setFrontACPause(status) {
	logDebug "setFrontACPause(${status}) was called"
	sendEvent(name: "frontACPause", value: status, descriptionText: getDescriptionText("frontACPause was set to ${status}"),stateChange: true)
}

def setSceneSwitch(status) {
	logDebug "setSceneSwitch(${status}) was called"
	sendEvent(name: "sceneSwitch", value: status, descriptionText: getDescriptionText("sceneSwitch was set to ${status}"),stateChange: true)
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