/*

Data Storage Driver - Front Sensor Data

This driver stores data as attributes to set and get, like a global variable.
Auto sets motion status attributes to Timeout in minutes set in timeoutMinutes attributes.
Also sets timeout minutes for each scene when changed. 

v 1.0 - 12/22 - Set-up all needed attribute names
v 2.0 - 2/23 - Added auto Timeout for Motion Status attributes
v 2.5 - 10/14/23 - Removed Inactive State
v 3.0 - 10/16/23 - Added Stairs sensor and Front door sensor
v 3.5 - 11/3/23 - Added kitcvhemMotion and livingRoomMotion - go active for 3 seconds only if Timeout when Active
v 3.6 - 11/5/23 - Setting frontScheduledMode() will also set frontMode and activate motion if room is Active (for scheduler)
v 3.7 - 11/12/23 - Unshedule old timeout jobs when new timeoutMinutes is set
v 3.8 - 12/11/23 - Set timeoutMinutes when scheduledMode is updated
v 4.0 - 02/04/24 - Added Pantry Light and motion activation

*/
import java.text.SimpleDateFormat 
metadata {
	definition (
			name: "Front Sensor Data",
			namespace: "hubitat",
			author: "Chris B."
	) { 
      
		capability "Actuator"
		capability "PresenceSensor"
        
        // Internal attributes needed to store data in virtual device
        //TYPES: ENUM, STRING, DYNAMIC_ENUM, JSON_OBJECT, NUMBER, DATE, VECTOR3
		attribute "druHome", "ENUM"
		//attribute "dinnerTime", "STRING"
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
		attribute "frontActivityScroll", "STRING"
        attribute "readingLight", "ENUM"
        attribute "mailStatus", "ENUM"
        attribute "onVacation", "ENUM"
        attribute "frontACPause", "ENUM"
        attribute "sceneSwitch", "ENUM"
		attribute "frontDoorMotionStatus", "ENUM"
		attribute "frontDoorTimeoutMinutes", "ENUM"
		attribute "stairsMotionStatus", "ENUM"
		attribute "stairsTimeoutMinutes", "ENUM"	
		attribute "kitchenMotion", "ENUM"
		attribute "livingRoomMotion", "ENUM"
		attribute "stairsMotion", "ENUM"
		attribute "frontDoorMotion", "ENUM"
		attribute "pantryMotionStatus", "ENUM"
		attribute "pantryMotion", "ENUM"
		attribute "pantryTimeoutMinutes", "ENUM"
		attribute "xmasLights", "ENUM"
		attribute "xmasStatus", "ENUM"
		attribute "kitchenActivityMotion", "ENUM"
		attribute "presence", "ENUM"
		attribute "weatherForecast", "STRING"

		// Commands needed to change internal attributes of virtual device.
        command "setDruHome", [[name:"druHome",type:"ENUM", description:"Set Dru Home", constraints:["Yes","No"]]]
        //command "setDinnerTime",["STRING"]  

		// Front Mode and sensor 
		command "setFrontScheduledMode", [[name:"frontScheduledMode",type:"ENUM", description:"Set Front Scheduled Mode", constraints:["Dawn","Morning","Day","Evening","Dinner","Late Evening","TV Time","Night","Dim","Bright"]]]
        command "setFrontMode", [[name:"frontMode",type:"ENUM", description:"Set Front Mode", constraints:["Dawn","Morning","Day","Evening","Dinner","Late Evening","TV Time","Night","Dim","Bright"]]]
        command "setFrontSensor", [[name:"frontSensor",type:"ENUM", description:"Set Front Sensor", constraints:["On","Off"]]]
		// set timeout minutes
		command "setFrontDoorTimeoutMinutes", [[name:"frontDoorTimeoutMinutes",type:"ENUM", description:"Set Front Door Motion Timeout", constraints:["1","5","10","15","20","25","30","35","40","45"]]]
        command "setFrontTimeoutMinutes", [[name:"frontMotionTimeout",type:"ENUM", description:"Set Front Motion Timeout", constraints:["1","5","10","15","20","25","30","35","40","45","60","75","90","120"]]]
        command "setKitchenActivityTimeoutMin", [[name:"kitchenActivityTimeoutMin",type:"ENUM", description:"Set Kitchen Activity Timeout", constraints:["1","5","10","15","20","25","30","35","40","45"]]]
		command "setStairsTimeoutMinutes", [[name:"stairsTimeoutMinutes",type:"ENUM", description:"Set Stairs Motion Timeout", constraints:["1","5","10","15","20","25","30","35","40","45"]]]
		command "setPantryTimeoutMinutes", [[name:"pantryMotionTimeout",type:"ENUM", description:"Set Pantry Motion Timeout", constraints:["1","2","3","4","5","7","10","12","15","20","25","30","45","60"]]]
		// set activity status
		command "setKitchenActivityStatus", [[name:"kitchenActivityStatus",type:"ENUM", description:"Set Kitchen Activity", constraints:["Active","Timeout"]]]   
        command "setFrontActivity", ["STRING"]
		// other status
        command "setReadingLight", [[name:"readingLight",type:"ENUM", description:"Set Reading Light", constraints:["true","false"]]]
        command "setMailStatus", [[name:"mailStatus",type:"ENUM", description:"Set Mail Status", constraints:["true","false"]]]
        command "setOnVacation", [[name:"onVacation",type:"ENUM", description:"Set onVacation", constraints:["Yes","No"]]]
        command "setFrontACPause", [[name:"frontACPause",type:"ENUM", description:"Set frontACPause", constraints:["Yes","No"]]]
        command "setSceneSwitch", [[name:"sceneSwitch",type:"ENUM", description:"Set Scene Switch", constraints:["on","off"]]]
		// set motion status
		command "setFrontMotionStatus", [[name:"frontMotionStatus",type:"ENUM", description:"Set Front Motion", constraints:["Active","Timeout","Inactive"]]]
		command "setFrontDoorMotionStatus", [[name:"frontDoorMotionStatus",type:"ENUM", description:"Set Front Door Motion", constraints:["Active","Timeout"]]]	
		command "setStairsMotionStatus", [[name:"stairsMotionStatus",type:"ENUM", description:"Set Stairs Motion", constraints:["Active","Timeout"]]]
		command "setPantryMotionStatus", [[name:"pantryMotionStatus",type:"ENUM", description:"Set Pantry Motion", constraints:["Active","Timeout"]]]	
		command "setKitchenMotionStatus", [[name:"kitchenMotionStatus",type:"ENUM", description:"Set Kitchen Motion", constraints:["Active","Timeout","Inactive"]]]
		command "setLivingRoomMotionStatus", [[name:"livingRoomMotionStatus",type:"ENUM", description:"Set Living Room Motion", constraints:["Active","Timeout","Inactive"]]]
		// set motion active
		command "setKitchenMotion"
		command "setLivingRoomMotion"
		command "setStairsMotion"
		command "setFrontDoorMotion"
		command "setPantryMotion"
		//command "setKitchenActivityMotion", [[name:"kitchenActivityMotion",type:"ENUM", description:"Set Kithen Activity Motion", constraints:["active","inactive"]]]
		command "setXmasLights", [[name:"xmasLights",type:"ENUM", description:"Set Xmas Lights", constraints:["true","false"]]]
		command "setXmasStatus", [[name:"xmasStatus",type:"ENUM", description:"Set Xmas Status", constraints:["Before","Eve","Day","Past"]]]
		command "clearActivity"
		command "setPresence", [[name:"presence",type:"ENUM", description:"Set Alert Presence", constraints:["not present","yellow","orange","red"]]]
		command "setWeatherForecast", ["STRING"]
		
		
		/*command "setScheduleDawn", ["STRING"]
		command "setScheduleMorning", ["STRING"]
		command "setScheduleDay", ["STRING"]
		command "setScheduleEvening", ["STRING"]
		command "setScheduleDinner", ["STRING"]
		command "setScheduleLateEvening", ["STRING"]
		command "setScheduleTvTime", ["STRING"]
		command "setScheduleDim", ["STRING"]
		command "setScheduleNight", ["STRING"]
		command "setScheduleFireplaceOn", ["STRING"]
		command "setScheduleFireplaceOff", ["STRING"]*/
	}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)     
		input( name: "numActivity", type:"number", title: "Number of Activity Messages in Scrool", defaultValue: 10)    
	}
}

def installed() {
	log.warn "installed..."
	state.activity = []
	initialize()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	log.warn "description logging is: ${txtEnable == true}"
	if (logEnable) runIn(1800,logsOff)
	//state.activity = ["","","","",""]
	//state.activity = []
	initialize()
}

def initialize() {
	if (state?.lastRunningMode == null) {

	}
	
}

def clearActivity() {
	state.activity = []
	setFrontActivity("Activities Cleared")
}

// Command methods needed to change internal attributes of virtual device.
def setDruHome(value) {
    logDebug "setDruHome(${value}) was called"
    sendEvent(name: "druHome", value: value, descriptionText: getDescriptionText("druHome was set to ${value}"),stateChange: true)
}

/*
def setDinnerTime(value) {
	logDebug "setDinnerTime(${value}) was called"
	sendEvent(name: "dinnerTime", value: value, descriptionText: getDescriptionText("dinnerTime was set to ${value}"),stateChange: true)
}*/

def setFrontMotionStatus(status) {
	logDebug "setFrontMotionStatus(${status}) was called"
	sendEvent(name: "frontMotionStatus", value: status, descriptionText: getDescriptionText("frontMotionStatus was set to ${status}"),stateChange: true)	
/*     if (status == "Active") {
        def sec = 3600   // one hour
        logDebug "frontMotionyStatus Timeout in (${sec})"
	    runIn(sec, setFrontMotionTimeout)
	}     */   
}

def setFrontMotionTimeout() {
    logDebug "setFrontMotionTimeout() was called"
	sendEvent(name: "frontMotionStatus", value: "Timeout", descriptionText: getDescriptionText("frontMotionStatus was set to Timeout"),stateChange: true)
}

def setFrontMotionInactive() {	
    logDebug "setFrontMotionInactive() was called"
	sendEvent(name: "frontMotionStatus", value: "Inactive", descriptionText: getDescriptionText("frontMotionStatus was set to Inactive"),stateChange: true)
}

def setFrontMotionActive() {
    logDebug "setFrontMotionActive() was called"
	sendEvent(name: "frontMotionStatus", value: "Active", descriptionText: getDescriptionText("frontMotionStatus was set to Active"),stateChange: true)
}

def setFrontScheduledMode(value) {
	logDebug "setFrontScheduledMode(${value}) was called"	
	sendEvent(name: "frontScheduledMode", value: value, descriptionText: getDescriptionText("frontScheduledMode was set to ${value}"),stateChange: true)
	setFrontMode(value)
	if (device.currentValue("kitchenMotionStatus") == "Active") {
		setKitchenMotion()
	}
	if (device.currentValue("livingRoomMotionStatus") == "Active") {
		setLivingRoomMotion()
	}	
}

def setFrontMode(mode) {
	logDebug "setFrontMode(${mode}) was called"
	sendEvent(name: "frontMode", value: mode, descriptionText: getDescriptionText("frontMode was set to ${mode}"),stateChange: true)
	if (mode != "All Off" && mode != "Lamps Off") setFrontTimeoutMinutes(getTimeoutMinutes(value))
}

int getTimeoutMinutes(scene) {
	int mins = 15
    if (scene == "Dawn") {mins = 10}
	else if (scene == "TV Time" || scene == "Dinner") {mins = 30}
    else if (scene == "Morning" || scene == "Bright") {mins = 10}
    else if (scene == "Night") {mins = 5}
    else if (scene == "Day" || scene == "Evening" || scene == "Dim" || scene == "Late Evening") {mins = 10}
	else {mins = 10}

	return mins
}

def setFrontSensor(value) {
	logDebug "setFrontSensor(${value}) was called"
	sendEvent(name: "frontSensor", value: value, descriptionText: getDescriptionText("frontSensor was set to ${value}"),stateChange: true)
}

def setFrontTimeoutMinutes(min) {
	logDebug "setFrontTimeoutMinutes(${min}) was called"
	sendEvent(name: "frontTimeoutMinutes", value: min, descriptionText: getDescriptionText("frontTimeoutMinutes was set to ${min}"),stateChange: true)
	//unschedule(setFrontMotionTimeout)
	//unschedule(setKitchenMotionTimeout)
	//unschedule(setLivingRoomMotionTimeout)
}

def setFrontDoorTimeoutMinutes(min) {
	logDebug "setFrontDoorMinutes(${min}) was called"
	sendEvent(name: "frontDoorTimeoutMinutes", value: min, descriptionText: getDescriptionText("frontDoorTimeoutMinutes was set to ${min}"),stateChange: true)
	unschedule(setFrontDoorMotionTimeout)
}

def setStairsTimeoutMinutes(min) {
	logDebug "setStairsMinutes(${min}) was called"
	sendEvent(name: "stairsTimeoutMinutes", value: min, descriptionText: getDescriptionText("stairsTimeoutMinutes was set to ${min}"),stateChange: true)
	unschedule(setStairsMotionTimeout)
}

def setKitchenActivityStatus(status) {
	logDebug "setKitchenActivityStatus(${status}) was called"	
	if (status == "Active") {
		if (device.currentValue("kitchenActivityStatus") == "Timeout") {setKitchenActivityMotion()}
		sendEvent(name: "kitchenActivityStatus", value: "Active", descriptionText: getDescriptionText("kitchenActivityStatus was set to Active"),stateChange: true)
		unschedule("setKitchenActivityTimeout")
	}
	else if (status == "Timeout") {
		runIn(1,setKitchenActivityTimeout)
	} else if (status == "Inactive") {
		setKitchenActivityInactive()
	}
}

def setKitchenActivityInactive() {
	logDebug "setKitchenActivityActive() was called"
	sendEvent(name: "kitchenActivityStatus", value: "Inactive", descriptionText: getDescriptionText("kitchenActivityStatus was set to Inactive"),stateChange: true)
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

def setKitchenActivityMotion() {
	logDebug "setKitchenActivityMotion() was called"
    sendEvent(name: "kitchenActivityMotion", value: "active", descriptionText: getDescriptionText("kitchenActivityMotion was set to active"),stateChange: true)
	runIn(3,resetKitchenActivityMotion)
}

def resetKitchenActivityMotion() {
	sendEvent(name: "kitchenActivityMotion", value: "inactive", descriptionText: getDescriptionText("kitchenActivityMotion was set to inactive"),stateChange: true)
}

def setKitchenActivityTimeoutMin(min) {
	logDebug "setKitchenActivityTimeoutMin(${min}) was called"
	sendEvent(name: "kitchenActivityTimeoutMin", value: min, descriptionText: getDescriptionText("kitchenActivityTimeoutMin was set to ${min}"),stateChange: true)
	unschedule(setKitchenActivityTimeout)
}

def setKitchenMotionStatus(status) {
	logDebug "setKitchenMotionStatus(${status}) was called"	
	if (status == "Active") {
		if (device.currentValue("kitchenMotionStatus") == "Timeout") {							
			setKitchenMotion()  // trigger back for the scene to run
		}
		setFrontMotionActive()	
		sendEvent(name: "kitchenMotionStatus", value: "Active", descriptionText: getDescriptionText("kitchenMotionStatus was set to Active"),stateChange: true)	
		unschedule("setKitchenMotionTimeout")	
	}
	if (status == "Timeout") {
		runIn(1, setKitchenMotionTimeout)
		runIn(1, setKitchenActivityTimeout)
	} 
	if (status == "Inactive") {	
		setKitchenMotionInactive()
	}
}

def setKitchenMotionInactive() {
	logDebug "setKitchenMotionInactive() was called"
	sendEvent(name: "kitchenMotionStatus", value: "Inactive", descriptionText: getDescriptionText("kitchenMotionStatus was set to Inactive"),stateChange: true)	
	def timeoutMinutes = device.currentValue("frontTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)			
	runIn(sec, setKitchenMotionTimeout)
	logDebug "KitchenMotionStatus Timeout in (${sec})"	
	def livingRoomMotion = device.currentValue("livingRoomMotionStatus")
	if (livingRoomMotion == "Inactive" || livingRoomMotion == "Timeout") {setFrontMotionInactive()}
}

def setKitchenMotionTimeout() {
	logDebug "setKitchenMotionTimeout() was called"
    sendEvent(name: "kitchenMotionStatus", value: "Timeout", descriptionText: getDescriptionText("kitchenMotionStatus was set to Timeout"),stateChange: true)

	if (device.currentValue("livingRoomMotionStatus") == "Timeout") {setFrontMotionTimeout()}
}

def setKitchenMotion() {
	logDebug "setKitchenMotion() was called"
    sendEvent(name: "kitchenMotion", value: "active", descriptionText: getDescriptionText("kitchenMotion was set to active"),stateChange: true)

	runIn(3,resetKitchenMotion)
}

def resetKitchenMotion() {
	sendEvent(name: "kitchenMotion", value: "inactive", descriptionText: getDescriptionText("kitchenMotion was set to inactive"),stateChange: true)
}
                                 
def setLivingRoomMotionStatus(status) {
	logDebug "setLivingRoomMotionStatus(${status}) was called"
    if (status == "Active") {
		if (device.currentValue("livingRoomMotionStatus") == "Timeout"){
			setLivingRoomMotion() 		
		} 
		sendEvent(name: "livingRoomMotionStatus", value: "Active", descriptionText: getDescriptionText("livingRoomMotionStatus was set to Active"),stateChange: true)	
		setFrontMotionActive()
		unschedule("setLivingRoomMotionTimeout")
	}
	if (status == "Timeout") {
		runIn(1,setLivingRoomMotionTimeout)
	} 
	if (status == "Inactive") {
		setLivingRoomMotionInactive()
	}
}

def setLivingRoomMotionInactive() {
	logDebug "setLivingRoomMotionInactive() was called"
    sendEvent(name: "livingRoomMotionStatus", value: "Inactive", descriptionText: getDescriptionText("livingRoomMotionStatus was set to Inactive"),stateChange: true)
	def timeoutMinutes = device.currentValue("frontTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)	
	logDebug "LivingRoomMotionStatus Timeout in (${sec})"
	runIn(sec, setLivingRoomMotionTimeout)
	def kitchenMotion = device.currentValue("kitchenMotionStatus")
	if (kitchenMotion == "Inactive" || kitchenMotion == "Timeout") {setFrontMotionInactive()}	
}

def setLivingRoomMotionTimeout() {
	logDebug "setLivingRoomMotionTimeout() was called"
	sendEvent(name: "livingRoomMotionStatus", value: "Timeout", descriptionText: getDescriptionText("livingRoomMotionStatus was set to Timeout"),stateChange: true)

	if (device.currentValue("kitchenMotionStatus") == "Timeout") {setFrontMotionTimeout()}
}

def setLivingRoomMotion() {
	logDebug "setLvingRoomMotion was called"
    sendEvent(name: "livingRoomMotion", value: "active", descriptionText: getDescriptionText("livingRoomMotion was set to active"),stateChange: true)
	runIn(3,resetLivingRoomMotion)
}

def resetLivingRoomMotion() {
	sendEvent(name: "livingRoomMotion", value: "inactive", descriptionText: getDescriptionText("livingRoomMotion was set to inactive"),stateChange: true)
}

// Pantry Motion 
def setPantryTimeoutMinutes(min) {
	logDebug "setPantryTimeoutMinutes(${min}) was called"
	sendEvent(name: "pantryTimeoutMinutes", value: min, descriptionText: getDescriptionText("pantryTimeoutMinutes was set to ${min}"),stateChange: true)
	unschedule(setPantryMotionTimeout)
}

def setPantryMotionStatus(status) {
	logDebug "setPantryMotionStatus(${status}) was called"
    if (status == "Active") {
		if (device.currentValue("pantryMotionStatus") == "Timeout") setPantryMotion() 
		setPantryMotionActive()
	}
	else if (status == "Timeout") runIn(1,setPantryMotionTimeout)
}

def setPantryMotionActive() {
	logDebug "setPantryMotionActive() was called"
    sendEvent(name: "pantryMotionStatus", value: "Active", descriptionText: getDescriptionText("pantryMotionStatus was set to Active"),stateChange: true)
	def timeoutMinutes = device.currentValue("pantryTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)	
	logDebug "pantryMotionStatus Timeout in (${sec})"
	runIn(sec, setPantryMotionTimeout)
}

def setPantryMotionTimeout() {
	logDebug "setPantryMotionTimeout() was called"
	sendEvent(name: "pantryMotionStatus", value: "Timeout", descriptionText: getDescriptionText("pantryMotionStatus was set to Timeout"),stateChange: true)
}

def setPantryMotion() {
	logDebug "setLvingRoomMotion was called"
    sendEvent(name: "pantryMotion", value: "active", descriptionText: getDescriptionText("pantryMotion was set to active"),stateChange: true)
	runIn(3,resetPantryMotion)
}

def resetPantryMotion() {
	sendEvent(name: "pantryMotion", value: "inactive", descriptionText: getDescriptionText("pantryMotion was set to inactive"),stateChange: true)
}

// Front Door Motion
def setFrontDoorMotionStatus(status) {
	logDebug "setFrontDoorMotionStatus(${status}) was called"
    if (status == "Active") {
		if (device.currentValue("frontDoorMotionStatus") == "Timeout") setFrontDoorMotion("active")
		setFrontDoorMotionActive()
	}
	else if (status == "Timeout") runIn(1,setFrontDoorMotionTimeout)
}

def setFrontDoorMotionActive() {
	logDebug "setFrontDoorMotionActive() was called"
    sendEvent(name: "frontDoorMotionStatus", value: "Active", descriptionText: getDescriptionText("frontDoorMotionStatus was set to Active"),stateChange: true)
	def timeoutMinutes = device.currentValue("frontDoorTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)	
	logDebug "frontDoorMotionStatus Timeout in (${sec})"
	runIn(sec, setFrontDoorMotionTimeout)
}

def setFrontDoorMotionTimeout() {
	logDebug "setFrontDoorMotionTimeout() was called"
	sendEvent(name: "frontDoorMotionStatus", value: "Timeout", descriptionText: getDescriptionText("frontDoorMotionStatus was set to Timeout"),stateChange: true)
	unschedule(setFrontDoorMotionTimeout)
}

def setFrontDoorMotion(status) {
	logDebug "setFrontDoorMotion(${status}) was called"
    sendEvent(name: "frontDoorMotion", value: "active", descriptionText: getDescriptionText("frontDoorMotion was set to active"),stateChange: true)
	runIn(3,resetFrontDoorMotion)
}

def resetFrontDoorMotion() {
	sendEvent(name: "frontDoorMotion", value: "inactive", descriptionText: getDescriptionText("frontDoorMotion was set to inactive"),stateChange: true)
}

def setStairsMotionStatus(status) {
	logDebug "setStairsMotionStatus(${status}) was called"
    if (status == "Active") {
		if (device.currentValue("stairsMotionStatus") == "Timeout") {setStairsMotionActive()}
	}
	if (status == "Timeout") runIn(1,setStairsMotionTimeout)
}

def setStairsMotionActive() {
	logDebug "setStairsMotionActive() was called"
    sendEvent(name: "stairsMotionStatus", value: "Active", descriptionText: getDescriptionText("stairsMotionStatus was set to Active"),stateChange: true)
    if (device.currentValue("stairsMotionStatus") == "Timeout") {setStairsMotion()}
	def timeoutMinutes = device.currentValue("stairsTimeoutMinutes").toInteger()
	logDebug "timeoutMinutes is (${timeoutMinutes})"
	def sec = (timeoutMinutes * 60)	
	logDebug "stairsMotionStatus Timeout in (${sec})"
	runIn(sec, setStairsMotionTimeout)
}

def setStairsMotionTimeout() {
	logDebug "setStairsMotionTimeout() was called"
	sendEvent(name: "stairsMotionStatus", value: "Timeout", descriptionText: getDescriptionText("stairsMotionStatus was set to Timeout"),stateChange: true)
}

def setStairsMotion(status) {
	logDebug "setStairsMotion(${status}) was called"
    sendEvent(name: "stairsMotion", value: "active", descriptionText: getDescriptionText("stairsnMotion was set to active"),stateChange: true)
	runIn(3,resetStairsMotion)
}

def resetStairsMotion() {
	sendEvent(name: "stairsMotion", value: "inactive", descriptionText: getDescriptionText("stairsMotion was set to inactive"),stateChange: true)
}

def setFrontActivityOld(status) {
	logDebug "setFrontActivity(${status}) was called"
	sendEvent(name: "frontActivity", value: status, descriptionText: getDescriptionText("frontActivity was set to ${status}"),stateChange: true)
}

def setFrontActivity(status) {
	logDebug "setFrontActivity(${status}) was called"
	def now = new SimpleDateFormat("hh:mm a").format(new Date())
	def activity = now + ": " + status
	sendEvent(name: "frontActivity", value: activity, descriptionText: getDescriptionText("frontActivity was set to ${status}"),stateChange: true)
	setFrontActivityScroll(activity)
}

def setFrontActivityScroll(activity) { 
    // add to activity list
	def size = settings?.numActivity.toInteger()
    state?.activity.push(activity)
    if (state?.activity.size() > size)
        state.activity.removeAt(0)
    logDebug(state?.luxArray)
	String allActivity = ""
	def arraySize = state?.activity.size()
	for (int x=arraySize-1; x>=0; x--) {
		allActivity = allActivity + state?.activity[x]
		if (x != 0) {allActivity = allActivity+"<br>"}
	}
	//String allActivity = state?.activity[4]+"<br>"+state?.activity[3]+"<br>"+state?.activity[2]+"<br>"+state?.activity[1]+"<br>"+state?.activity[0]
	String html = """<marquee scrolldelay="500" direction="up" style="font-size:11pt; text-align:center; height: 38px;">${allActivity}</marquee>"""
	sendEvent(name: "frontActivityScroll", value: html, descriptionText: getDescriptionText("frontActivityScroll was set to ${html}}"),stateChange: true)
}

def setReadingLight(status) {
	logDebug "setReadingLight(${status}) was called"
	sendEvent(name: "readingLight", value: status, descriptionText: getDescriptionText("readingLight was set to ${status}"),stateChange: true)
}

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

def setXmasLights(status) {
	logDebug "setXmasLights(${status}) was called"
	sendEvent(name: "xmasLights", value: status, descriptionText: getDescriptionText("xmasLights was set to ${status}"),stateChange: true)
}

def setXmasStatus(status) {
	logDebug "setXmasStatus(${status}) was called"
	sendEvent(name: "xmasStatus", value: status, descriptionText: getDescriptionText("xmasStatus was set to ${status}"),stateChange: true)
}

def setPresence(set) {
	logDebug "setPresence(${set}) was called"
	sendEvent(name: "presence", value: set, descriptionText: getDescriptionText("presence was set to ${set}"),stateChange: true) 	
}

def setWeatherForecast(set) {
	logDebug "setWeatherForecast(${set}) was called"
	sendEvent(name: "weatherForecast", value: set, descriptionText: getDescriptionText("weatherForecast was set to ${set}"),stateChange: true) 	
}

/* // Set Schedules
def setScheduleDawn(cron) {
	logDebug "setDawnSchedule(${cron}) was called"
	sendEvent(name: "scheduleDawn", value: cron, descriptionText: getDescriptionText("scheduleDawn was set to ${cron}"),stateChange: true)
}
def setScheduleMorning(cron) {
	logDebug "setMorningSchedule(${cron}) was called"
	sendEvent(name: "scheduleMorning", value: cron, descriptionText: getDescriptionText("scheduleMorning was set to ${cron}"),stateChange: true)
}
def setScheduleDay(cron) {
	logDebug "setDaySchedule(${cron}) was called"
	sendEvent(name: "scheduleDay", value: cron, descriptionText: getDescriptionText("scheduleDay was set to ${cron}"),stateChange: true)
}
def setScheduleEvening(cron) {
	logDebug "setEveningSchedule(${cron}) was called"
	sendEvent(name: "scheduleEvening", value: cron, descriptionText: getDescriptionText("scheduleEvening was set to ${cron}"),stateChange: true)
}
def setScheduleDinner(cron) {
	logDebug "setDinnerSchedule(${cron}) was called"
	sendEvent(name: "scheduleDinner", value: cron, descriptionText: getDescriptionText("scheduleDinner was set to ${cron}"),stateChange: true)
}
def setScheduleLateEvening(cron) {
	logDebug "setLateEveningSchedule(${cron}) was called"
	sendEvent(name: "scheduleLateEvening", value: cron, descriptionText: getDescriptionText("scheduleLateEvening was set to ${cron}"),stateChange: true)
}
def setScheduleTvTime(cron) {
	logDebug "setTvTimeSchedule(${cron}) was called"
	sendEvent(name: "scheduleTvTime", value: cron, descriptionText: getDescriptionText("scheduleTvTime was set to ${cron}"),stateChange: true)
}
def setScheduleDim(cron) {
	logDebug "setDimSchedule(${cron}) was called"
	sendEvent(name: "scheduleDim", value: cron, descriptionText: getDescriptionText("scheduleDim was set to ${cron}"),stateChange: true)
}
def setScheduleNight(cron) {
	logDebug "setNightSchedule(${cron}) was called"
	sendEvent(name: "scheduleNight", value: cron, descriptionText: getDescriptionText("scheduleNight was set to ${cron}"),stateChange: true)
}
def setScheduleFireplaceOn(cron) {
	logDebug "setFireplaceOnSchedule(${cron}) was called"
	sendEvent(name: "scheduleFireplaceOn", value: cron, descriptionText: getDescriptionText("scheduleFireplaceOn was set to ${cron}"),stateChange: true)
}
def setScheduleFireplaceOff(cron) {
	logDebug "setFireplaceOffSchedule(${cron}) was called"
	sendEvent(name: "scheduleFireplaceOff", value: cron, descriptionText: getDescriptionText("scheduleFireplaceOff was set to ${cron}"),stateChange: true)
}*/

def parse(String description) {
	logDebug "$description"
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