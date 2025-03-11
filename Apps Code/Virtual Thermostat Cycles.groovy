/*
	Virtual Thermostat Cycles
    Version 1.0  2/10/24

    This Driver is for a virtual thermostat with Added Features 
	
	Added features:
    - Contact Sensor capability opens and closes contact with heat thermostatMode (for running alexa connected routines to turn on/off heater fan or Aux Heat)
	- Switch capability to turn on/off the contact sensor (AC fan or aux heat) as an on/off switch tile
    - Motion capability changes motion with cool thermostatMode.  Is active when cooling, inactive when off/fan only (for running alexa connected air conditioner routines ac state)
	
I use three hysteresis settings (maybe better to call them offsets).  Reverse for cooling. 

hysteresis - This determines when to Ramp. I use 0.2 degrees generally. So at 0.2 degrees below setpoint, the furnace will ramp continuously up to that temperature.
 Once at temp, cycles kick in to maintain temperature 
 
cyclingHysteresis - determines when the cycling starts when above setpoint and temps are falling. Depends on the zone, but generally around 0.2 to start cycling when temp is 0.2 above setpoint.
 If a cycle is running and temp goes above this point, cycling will stop and there will be a wait set.

targetHysteresis - This is the stop point for a cycle, and it is a bit less than the cycleHystereis. This will stop a cycle if temperature is rising past this setting, and the thermostat
 is still cycling. If a cycle actually gets up to the cyclingHysteresis point before turning off, it tends to overheat, so the target hysteresis stops the cycle a bit before that.
 
    
	Version 2.0 2/13/24
    - refactored to use cycling.  
    - Ramping:  When below setpoint +- hysteresis, will ramp until it gets to that point.  The cycleWait flag is set to not cycle during this time
    - Cycling:  Will cycle when temp is below setpoint +- cyclingHystresis.  Duration for cyclingSeconds is based on cycles per hour chosen in prefrences initially.
                Cycling will terminate if temp goes over cycling point
                The cycleWait flag is set at end of cycle.  It is cleared after setCycleSeconds or if temp goes back above cycling point. 
    - Stopping:  Cycles are timed, and they stop when done or when the temp rises above the target hysteresis temp.  

	Version 2.1 3/1/24
	- Cycle times are now self adjusting based on last run.  If cycles go over target they are reduced, under target the are increaed. 
	- Tracks rise and fall changes to determine cycle highs and lows for calcs.  
	- Cycles are counted.  If it takes more than two cycles to get to temp, cycle time is increased regardless of end temp.  
	- Center the cycles around the setpoint by adjusting the cycle hysteresis based on averge of cycle high/low. 

	Version 3.0: 4/1/24
	- Eliminated self adjusting cycles.
	- calc cycle times from the slop of the temp rise and fall.  Working to some extent, but with way too many constant calcuations. 
	- Abandoned, not really possible due to temp sensor reporting times vs changes, causing inconsistant slopes, even when using a running average of slope values.  

	Version 4.0 11/14/24
	- Eliminated sloap calculations for cycle times
	- Default cycle time and default wait time is now set in preferences, along with a coefficient for changes. 
	- All adjustments are now made from outside temp diferences from below heating temps (50 F), to above ac temps (70 F). 
	- The coefficeint represnets how much of a change will be made to cycle time and wait time based on how cold or hot it is outside, and setpoint. 

	Version 4.1 12/15/24
	- Added display attributes for a cycle times display and a hysteresis settings display.  Also, a setpoint display attribute with degrees.   
	- Made the calcuation for cycle times non-linear, so more change when colder/hotter. 

*/

import groovy.time.*
metadata {
	definition (
			name: "Virtual Thermostat Cycles",
			namespace: "hubitat",
			author: "Kevin L., Mike M., Bruce R., Chris B."
	) {
		capability "Actuator"
		capability "Sensor"
		capability "Temperature Measurement"
		capability "Thermostat"
        capability "Contact Sensor"
        capability "Switch"
        capability "Motion Sensor"
        capability "Presence Sensor"

/* 		attribute "supportedThermostatFanModes", "JSON_OBJECT"
		attribute "supportedThermostatModes", "JSON_OBJECT" */
		attribute "hysteresis", "NUMBER"
        attribute "targetHysteresis", "NUMBER"
        attribute "errorCheck", "ENUM"  // to do
        attribute "motion", "ENUM"      // motion is active with cool operatingState, inactive when heat/fan
        attribute "presence", "ENUM"    // presence is AC Status set from Webcore based on AC contact sensor status
		attribute "acStatusIcon", "STRING"
		attribute "iconFile", "STRING"
		attribute "operatingBrightness", "ENUM"
		attribute "idleBrightness", "ENUM"
		attribute "acStatus", "STRING"
		attribute "cycling", "ENUM"
		attribute "cyclingHysteresis", "NUMBER"
        attribute "cycleState", "ENUM"
		attribute "outsideTemp", "STRING"
		attribute "temperatureState", "ENUM"
		attribute "cycleSeconds", "ENUM"
		attribute "waitSeconds", "ENUM"
		attribute "slope", "NUMBER"
		attribute "autoMode", "ENUM"
		attribute "fanSpeed", "ENUM"
		attribute "displayHysteresis", "STIRNG"
		attribute "displaySetpoint", "STIRNG"
		attribute "displayCycles", "STIRNG"
		//attribute ""

		// Commands needed to change internal attributes of virtual device.
		command "setTemperature", ["NUMBER"]
		command "setThermostatOperatingState", ["ENUM"]
		command "setThermostatSetpoint", ["NUMBER"]
 		command "setSupportedThermostatFanModes", ["JSON_OBJECT"]
		command "setSupportedThermostatModes", ["JSON_OBJECT"] 
        command "setThermostatFanMode", [[name:"thermostatFanMode",type:"ENUM", description:"Thermo Fan Mode", constraints:["on","auto","circulate"]]]
        command "setHysteresis", ["NUMBER"]
        command "setTargetHysteresis", ["NUMBER"]
        command "setErrorCheck",[[name:"errorCheck",type:"ENUM", description:"AC Error Check", constraints:["true","false"]]]
        command "setContact",[[name:"contact",type:"ENUM", description:"Set AC on/off", constraints:["open","closed"]]]
        command "setMotion",[[name:"motion",type:"ENUM", description:"Set AC cooling on/off", constraints:["active","inactive"]]]
        command "setPresence",[[name:"presence",type:"ENUM", description:"Set AC State", constraints:["off","fan","cool"]]]
		command "setAcStatusIcon", ["STRING"]
        command "setOnInMin", ["NUMBER"]
		command "setOperatingBrightness", [[name:"operatingBrightness",type:"ENUM", description:"Set operating brightness", constraints:["0", "1", "2", "3", "4", "5"]]]
	    command "setIdleBrightness", [[name:"heatBrightness",type:"ENUM", description:"Set Idle brightness", constraints:["0", "1", "2", "3", "4", "5"]]]
		command "setCycling",[[name:"cycling",type:"ENUM", description:"Cycling Heat", constraints:["true","false"]]]
		command "setCyclingHysteresis", ["NUMBER"]
        command "setCycleState",[[name:"cycleState",type:"ENUM", description:"Cycling State", constraints:["Cycling","Ramping","Waiting","Idle"]]]
		command "setOutsideTemp", ["STRING"]
		command "stopCycleWait"
		command "stopCycle"
		command "initCycling"
		command "manageCycle"
		command "initCycling"
		command "setCycleSeconds", ["ENUM"]
		command "setWaitSeconds", ["ENUM"]
		command "setCycleWait"
		command "updateCycleSeconds"
		command "setDisplaySetpoint"
		command "setDisplayHysteresis"
		command "setDisplayCycles"
        command "setAutoMode",[[name:"autoMode",type:"ENUM", description:"Auto Switchover Mode", constraints:["true","false"]]]
		command "setFanSpeed",[[name:"fanSpeed",type:"ENUM", description:"AC Fan Speed", constraints:["low","medium","high"]]]
		command "setTemperatureState",[[name:"temperatureState",type:"ENUM", description:"Rise/Fall State", constraints:["rising","rising steady","falling","falling steady"]]]
	}

	preferences {
        input( name: "iconPath", type: "string", description: "Address Path to icons", title: "Set Icon Path", defaultValue: "https://cburges2.github.io/ecowitt.github.io/Dashboard%20Icons/")
		input( name: "useACState", type:"bool", title: "Enable using AC State and Icon",defaultValue: false)
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
		input( name: "cycleCoefficient",type:"enum",title: "Adjust Cycle Coefficient", options:["1","2","3","4","5","6","8","10","12","15","20","25","30","35","40","45","50"], description:"Lower if temp goes too high(heat)/low(cool) when cycling in colder or hotter weather", defaultValue: 0)
		input( name: "defaultSeconds",type:"enum",title: "Default Cycle Seconds for Zone", options:["10","15","20","25","30","35","40","45","50","55","60","65","70","75","80","85","90","95","100","115","120","125","130","140","145","155","160","165","170","175","180","185","190","195","200","150","200","250","300","350","400","450","500","550","600","650","700","750","800","850","900","950","1000","1100","1150","1200","1250","1300","1350","1400","1450","1500"], description:"Decrease if temps going too far beyond target, Increase if short cycling", defaultValue: 200)
		input(name: "defaultWait",type:"enum",title: "Default Wait Cycle Seconds for Zone", options:["100","105","110","115","120","125","130","140","145","155","160","165","170","175","180","185","190","195","200","150","200","250","300","350","400","450","500","550","600","650","700","750","800","850","900","950","1000","1100","1150","1200","1250","1300","1350","1400","1450","1500","1550","1650"], description:"Increase if heat cycling before sensing temp increase from a cycle", defaultValue: 120)
	}
}

def installed() {
	log.warn "installed..."
	runIn(1,initCycling)
	initAttributes()
	initStates()
	updated()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	log.warn "description logging is: ${txtEnable == true}"
	if (logEnable) runIn(21600,logsOff)   // 6 hours

	//initCycling()
	//initAttributes()
	//initStates()
	
	initialize()

}

def initialize() {

	initCycling()
	//state.remove("waiting")
	//state.remove("cycling")	
	//state.waiting = false
	//state.cycling = false
}

def initAttributes() {
	sendEvent(name: "temperature", value: convertTemperatureIfNeeded(68.0,"F",1))
	sendEvent(name: "thermostatSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
	sendEvent(name: "heatingSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
	sendEvent(name: "coolingSetpoint", value: convertTemperatureIfNeeded(75.0,"F",1))
	sendEvent(name: "outsideTemp", value: convertTemperatureIfNeeded(65.0,"F",1))
	setThermostatOperatingState("heat")
	setSupportedThermostatFanModes([auto, circulate, on])
	setSupportedThermostatModes([cool, heat, off])
	thermoOff()
	fanAuto()
	setCyclingHysteresis("0.08")
	setTargetHysteresis("0.1")
	setHysteresis("0.2")
	setCycling("false")
	setCycleState("idle")	
	setPresence("off")
	if (settings?.useAcState) {
		setIconFile("ac-off")
		setAcStatusIcon("off")
	}
}

def initStates() {

	def temperature = device.currentValue("temperature").toBigDecimal()

	// used for cycles
	state.waitSeconds = 0
	state.cycleSeconds = 0

	state.fallEndTemp = temperature
	state.steadyFallTemp = temperature

	// calc gain
	state.riseEndTemp = temperature
	state.steadyRiseTemp = temperature

	// Cycle state flags
	state.ramping = false
	state.cycling = false
	state.waiting = false
	state.numCycles = 0.0

	// temp status
	state.tempState = "falling steady"
	state.lastTemp = temperature
	state.highStamp = 0
	state.lowStamp = 0

	state.lastRunningMode = "idle"
	state.heatFactor = 0
	state.coolFactor = 0
}

def initCycling() {
	logDebug("Calculating Cycle States")

	def outTemp = Float.valueOf(device.currentValue("outsideTemp"))

	adjustFactors(outTemp)

	//setCycleSeconds((settings?.defaultSeconds).toInteger())
	//setWaitSeconds((settings?.defaultWait).toInteger())

}

def setCycleSeconds(seconds) {
	state.cycleSeconds = seconds.toInteger()
	setCycleSecondsAttrib(seconds)
}

def setWaitSeconds(seconds) {
	state.waitSeconds = seconds.toInteger()
	setWaitSecondsAttrib(seconds)
}

def setCycleSecondsAttrib(value) {
	sendEvent(name: "cycleSeconds", value: value, descriptionText: getDescriptionText("cycleSeconds Attribute set to ${value}"))	
}

def setWaitSecondsAttrib(value) {
	sendEvent(name: "waitSeconds", value: value, descriptionText: getDescriptionText("waitSeconds Atrribute set to ${value}"))	
}

String getAutoMode(coolPoint, heatPoint, temp, mode, hyst) {

	def coolDemand = temp > coolPoint + hyst
	def heatDemand = temp < heatPoint - hyst
	def newMode = mode
	logDebug("Heat Demand is ${heatDemand}")
	logDebug("Cool Demand is ${coolDemand}")
	logDebug("mode is ${mode}")


	if (!(coolDemand && heatDemand)) {

		if (coolDemand && mode == "heat") {
			logDebug("Thermostat Mode changed to cool")
			setThermostatMode("cool")
			newMode = "cool"
		}
		if (heatDemand && mode == "cool") {
			logDebug("Thermostat Mode changed to heat")
			setThermostatMode("heat")
			newMode = "heat"
		}
	} else {logDebug("Difference not in range for auto mode change")}
	
	return newMode
}

def setDisplays() {
	setDisplaySetpoint()
	setDisplayHysteresis()
	setDisplayCycles()
}

def setDisplaySetpoint() {
	String display = " "+device.currentValue("thermostatSetpoint")+"°"
	sendEvent(name: "displaySetpoint", value: display, descriptionText: getDescriptionText("displaySetpoint set to ${display}"))

}

def setDisplayHysteresis() {
	String display = "Hysteresis: "+device.currentValue("hysteresis")+"<br>Cycling Hyst: "+device.currentValue("cyclingHysteresis")+"<br>Target Hyst: "+device.currentValue("targetHysteresis")
	sendEvent(name: "displayHysteresis", value: display, descriptionText: getDescriptionText("displayHysteresis set to ${display}"))
}

def setDisplayCycles() {
	String display = "Cycle Secs: "+device.currentValue("cycleSeconds")+"<br>Wait Secs:: "+device.currentValue("waitSeconds")
	sendEvent(name: "displayCycles", value: display, descriptionText: getDescriptionText("displayCycles set to ${display}"))
}

// **************************** Mangage Cycle *********************************
def manageCycle(){
	logDebug("Manage Cycle Running...")
	def thermostatOperatingState = device.currentValue("thermostatOperatingState") ?: "heat"
	def coolingSetpoint = device.currentValue("coolingSetpoint").toBigDecimal()
	def heatingSetpoint = device.currentValue("heatingSetpoint").toBigDecimal()
	def temperature = (device.currentValue("temperature") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()
	def hysteresis = (device.currentValue("hysteresis")).toBigDecimal() // start ramp
	def thermostatMode = device.currentValue("thermostatMode") ?: "off"

	// check auto changeover if autoMode true
	if (device.currentValue("autoMode") == "true") {
		logDebug("Auto Mode is true")
		thermostatMode = getAutoMode(coolingSetpoint, heatingSetpoint, temperature, thermostatMode, hysteresis)
	}

    def cyclingHysteresis = (device.currentValue("cyclingHysteresis").toBigDecimal())   // start cycle if falling temp, stop cycle if still rising
	def targetHysteresis = (device.currentValue("targetHysteresis").toBigDecimal())	// stop cycle

	// cycle on points
    def coolOn = coolingSetpoint - cyclingHysteresis
    def heatOn = heatingSetpoint + cyclingHysteresis

	// slope calc
	def coolSlope = coolingSetpoint + targetHysteresis
	def heatSlope = heatingSetpoint - targetHysteresis

	// target cycle off points
    def coolOff = coolingSetpoint - targetHysteresis
    def heatOff = heatingSetpoint + targetHysteresis
	
	// ****** Set Ramping ********
	def coolRamp = coolingSetpoint + hysteresis
	def heatRamp = heatingSetpoint - hysteresis

    // ** check ramping and cycling **
	def wasRamping = state?.ramping	// save ramp state before it changes
	def wasCycling = state?.cycling // save cycle state before it changes

	def ramping = false  
    def cycling = false
	def demand = false
	def wrongTrend = false
	def madeTarget = false
	//def wait = false
	def overSetpoint = false

	def rising = state?.tempState == "rising" || state?.tempState == "rising steady"
	def falling = state?.tempState == "falling" || state?.tempState == "falling steady"		
	logDebug("rising is ${rising}")
	logDebug("falling is ${falling}")

	// set flags for heat and cool settings (on = cyclingHyst, off = waitHyst, Ramp = hysteresis)
    if (thermostatMode == "cool") {
        ramping = temperature > coolRamp
		cycling = (temperature > coolOn) && (temperature <= coolRamp) && rising
		demand = temperature > coolingSetpoint
		wrongTrend = rising	
		madeTarget = temperature <= coolOn
		calculateSlope = temperature <= coolSlope
		overSetpoint = temperature <= coolOff
    }
    if (thermostatMode == "heat") {
 		ramping = temperature < heatRamp 
		cycling = (temperature < heatOn) && (temperature >= heatRamp) && falling
		demand = temperature < heatingSetpoint
		wrongTrend = falling
		madeTarget = temeprature >= heatOn
		calculateSlope = temperature >= heatSlope
		overSetpoint = temperature >= heatOff
	}
	
 	// set States to match
	def waiting = state?.waiting
	logDebug("Ramping Initial is ${ramping}")
	logDebug("Cycling Initial is ${cycling}")	
	state.cyclingOn = cycling	
	state.ramping = ramping

	logDebug("cycling is ${cycling}")   
	logDebug("ramping is ${ramping}") 
	logDebug("waiting is ${waiting}")

	// set cycleState attribute
	if (waiting && ramping) {
		stopCycleWait()
		waiting = false
	}
	updateCycleState(ramping, cycling, waiting)	

	// Set demand need for state and cycle updates
	def needCycle = (cycling || ramping) && state?.waiting == false
	
	logDebug("needCycle is ${needCycle}")   

    // ***** Set Thermostat Operating State ******
    // Cool
	if (thermostatMode == "cool") {
        if (needCycle && thermostatOperatingState != "cooling") {
			startCoolCycle()
        }    
        else if (!needCycle && thermostatOperatingState != "idle") {
			stopCoolCycle()
        }
    // Heat    
	} else if (thermostatMode == "heat") {
		if (needCycle && thermostatOperatingState != "heating") {
			startHeatCycle()
		}
		else if (!needCycle && thermostatOperatingState != "idle") {			
			stopHeatCycle()
		}
    // Auto not implemented with cycling
	} 
}
// ****************************************** End Mangage Cycle ***************************************************
// ******************************************                   ***************************************************


// set cycleState attribute for dashboard
def updateCycleState(ramping, cycling, waiting) {
	logDebug("updateCycleState called with Ramping: ${ramping}, Cycing: ${cycling}")
	logDebug("state.waiting is ${state?.waiting}")

	if (cycling && waiting) {cycling = false}
	if (ramping && waiting) {waiting = false}

	def cycleStateValue = device.currentValue("cycleState")
	def newState = "Idle"
    if (ramping) {newState = "Ramping"}
    else if (cycling) {newState = "Cycling"}
    else if (waiting) {newState = "Waiting"}
	else newState = "Idle"	
	if (!cycleStateValue.equals(newState)) {
		logDebug("updateing cycleState to ${newState}")
		setCycleState(newState)
	}

	runIn(1,setDisplays)
}

// Set Temperature Rise Fall
def setTempState(temp) {

	def lastTempState = state?.tempState
	def tempState = "steady"
	def lastTemp = state?.lastTemp
	logDebug("Last tempState was ${lastTempState}")
	logDebug("Temperature is ${temp}")
	logDebug("lastTemp is ${lastTemp}")

	if (temp < lastTemp) {						// temps falling	
		logDebug("Temp Decreased")	
		tempState = "falling"
		if (lastTempState == "falling steady") {
			tempState = "falling"
			state.riseEndTemp = state?.steadyFallTemp  // temp to use for 
			Date now = new Date()
			state.highStamp = now.getTime()
			logDebug("Flag set to Calc Gain")
		}
		if (lastTempState == "rising") {
			tempState = "falling steady"	
			state.steadyFallTemp = state?.lastTemp		
		}
		if (lastTempState == "rising steady") {			// bouncing
			tempState = "falling steady"		
		}		
	}
	if (temp > lastTemp) {						// temps rising
		logDebug("Temp Increased")	
		tempState = "rising"
		if (lastTempState == "rising steady") {
			tempState = "rising"	
			state.fallEndTemp = state?.steadyRiseTemp	
			Date now = new Date()
			state.lowStamp = now.getTime()
			logDebug("Set Flag to Calc Loss")
		}
		if (lastTempState == "falling" ) {
			tempState = "rising steady"
			state.steadyRiseTemp = state?.lastTemp
		}
		if (lastTempState == "falling steady" ) {
			tempState = "rising steady"					// bouncing
		}		
	}	

	state?.lastTemp = temp
	if (tempState != "steady") {
		state?.tempState = tempState   // if steady, it didn't update correctly
		setTemperatureState(tempState)
		logDebug("temperatureState is ${tempState}")	
	}
	runIn(1,setDisplays)
}

def setOutsideTemp(value) {
	logDebug "setOutsideTemp(${value}) was called"
	sendEvent(name: "outsideTemp", value: value, descriptionText: getDescriptionText("outsideTemp set to ${value}"))


    //def mode = device.currentValue("thermostatMode")
	//logDebug("Mode is ${mode}")
	def outTemp = Float.valueOf(value)
	state.outTemp = outTemp

	adjustFactors(outTemp)
	runIn(1,setDisplays)

}

// Adjust Cycle Seconds by factor based on temp diff and coefficient
def adjustFactors(outTemp) {
	def mode = device.currentValue("thermostatMode")

	if (mode == "cool") {		
		def over70 = outTemp - 70.0   // degrees over 70
		def coolPoint = Float.valueOf(device.currentValue("coolingSetpoint"))
		def under74 = 74.0 - coolPoint
		logDebug("over70 is ${over70}")
		
		if (over70 > 0.0) {
			def factor = Math.round(over70 * Float.valueOf(settings?.cycleCoefficient))
			logDebug("factor is ${factor}")

			state.coolFactor = factor
			logDebug("cool factor is ${state?.coolFactor}")

			updateCycleSeconds(factor)
			updateWaitSeconds(factor)
			runIn(1,manageCycle)
		} else {state.coolFactor = 0}
	}
	if (mode == "heat") {		
		def outDiff = 50.0 - outTemp  // degrees under 50
		logDebug("outDiff is ${outDiff}")

		def heatPoint = Float.valueOf(device.currentValue("heatingSetpoint"))
		def over69 = heatPoint - 69.0  // setpoint degrees over 69
		
		logDebug("over69 = ${over69}")
		
		def cycle = Float.valueOf(settings?.cycleCoefficient.toInteger())
		//over69 = over69 * cycle

		// increse diff with cold
		def coldFactor = (outDiff / 25.0) * outDiff

		outDiff = outDiff + over69 + coldFactor  // account for thermo setpoint and cold factor
		
		if (outDiff > 0.0) { 
			def factor = Math.round(outDiff * cycle)
	
			logDebug("factor is ${factor}")

			state.heatFactor = factor
			logDebug("heat factor is ${state?.heatFactor}")

			updateCycleSeconds(factor)
			updateWaitSeconds(factor)
			runIn(1,manageCycle)
		} else {state.heatFactor = 0}		
	}

}

def updateCycleSeconds(factor) {
	logDebug("updateCycleSeconds called with factor ${factor}")

	// calculate new cycle seconds using pref coefficient
	def cycle = settings?.cycleCoefficient.toInteger()		
	logDebug("cycle coefficient is ${cycle}")

	def seconds = settings?.defaultSeconds.toInteger()
	logDebug("default seconds is ${seconds}")
	cycle = seconds + factor	
	logDebug("cycle seconds changed to ${cycle}")

	if (cycle < seconds) {cycle = seconds}
	if (cycle > 1200) {cycle = 1200}  	

	// set cycle seconds values
	state.newCycleSeconds = cycle
	setCycleSeconds(cycle)
	runIn(1,setDisplays)
}

// update cycle seconds based on outside temp factor
def updateWaitSeconds(factor) {

    // set new interval seconds too for cycleWait
	def interval = settings?.defaultWait.toInteger()
	//def interval = state?.waitSeconds
	logDebug("set Interval is ${interval}")

    def newInterval = interval - factor

	logDebug("New Interval is ${newInterval}")

	if (newInterval < interval) {newInterval = 120}  // keep wait at room setting
	if (newInterval > 950) {newInterval = 950}  // keep below 950
  
	state.waitSeconds = newInterval
	setWaitSeconds(newInterval)
	logDebug("Changed Interval is ${newInterval}")

	runIn(1,setDisplays)
}

def startHeatCycle() {
	def waiting = state?.waiting
	if (!waiting) {
		logDebug("Heating Cycle Started")
		setThermostatOperatingState("heating")
		sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate) 		
		state.cycling = true
		def cycleOff = state?.cycleSeconds
		if (state?.ramping == false) {runIn(cycleOff,setCycleWait)}
	}
}

def startCoolCycle() {
	logDebug("Cooling Cycle Started")
	setThermostatOperatingState("cooling")
	sendEvent(name: "motion", value: "active", isStateChange: forceUpdate) 
	on()   
	def cycleOff = state?.cycleSeconds
	state.cycling = true
	if (state?.ramping == false) {runIn(cycleOff,setCycleWait)}
}

def stopHeatCycle() {
	logDebug("Heating Cycle Ended")
	setThermostatOperatingState("idle")
	state.cycling = false
	sendEvent(name: "contact", value: "open", isStateChange: forceUpdate) 
}

def stopCoolCycle() {
	logDebug("Cooling Cycle Ended")
	setThermostatOperatingState("idle")
	state.cycling = false
	sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
	off()
}

def setCycleWait() {
	logDebug("setting Cycle Wait")
	state.waiting = true
	state.cycling = false		
	def waitSecs = (state?.waitSeconds)
	runIn(waitSecs, stopCycleWait)  	// may need another cycle
	runIn(1,manageCycle)  
}

def stopCycleWait() {
	logDebug("Resetting Cycle Wait")
	unschedule("resetCycleWait")
    state.waiting = false
	setCycleState("Idle")
	unschedule(stopCycleWait)
	setCycleState("Idle")
	runIn(1,manageCycle)
}

def stopCycle() {
    logDebug("Ending Cycle")		
    if (device.currentValue("cycling") == "true") {
		setCycling("false")
		setCycleWait()
	}
    runIn(1,manageCycle)
}

def setCycleState(value) {
	logDebug "setCycleState(${value}) was called"
	sendEvent(name: "cycleState", value: value, descriptionText: getDescriptionText("cycleState set to ${value}"))    
}

// not implemented - check if AC is in state it should be
def checkError() {
    def checkErrors = device.currentValue("errorCheck") 
    def status = device.currrentValue("acStatus")
    def thermostatOperatingState = device.currentValue("thermostatOperatingState") ?: "heat"
}

// Commands needed to change internal attributes of virtual device.
def setTemperature(temperature) {
	logDebug "setTemperature(${temperature}) was called"
	
	sendTemperatureEvent("temperature", temperature)
	def temp = temperature.toBigDecimal()
	setTempState(temp)

	runIn(1, manageCycle)
	runIn(2, setDisplays)
}

def setTemperatureState(value) {
	sendEvent(name: "temperatureState", value: value, descriptionText: getDescriptionText("temperatureState set to ${value}"))	
}

def setCyclingHysteresis(value) {
	logDebug "setCyclingHysteresis(${value}) was called"
	sendEvent(name: "cyclingHysteresis", value: value, descriptionText: getDescriptionText("cyclingHysteresis set to ${value}"))
	setDisplayHysteresis()
}

// Commands needed to change internal attributes of virtual device.
def setCycling(value) {
	logDebug "setCycling(${value}) was called"
	sendEvent(name: "cycling", value: value, descriptionText: getDescriptionText("cycling set to ${value}"))
	runIn(3, manageCycle)
}

def setAutoMode(value) {
	logDebug "setAutoMode(${value}) was called"
	sendEvent(name: "autoMode", value: value, descriptionText: getDescriptionText("autoMode was set to ${value}"))
}

def setFanSpeed(value) {
	logDebug "setFanSpeed(${value}) was called"
	sendEvent(name: "fanSpeed", value: value, descriptionText: getDescriptionText("fanSpeed was set to ${value}"))
}

def setOperatingBrightness(value) {
	logDebug "setOperatingBrightness(${value}) was called"
	sendEvent(name: "operatingBrightness", value: value, descriptionText: getDescriptionText("operatingBrightness set to ${value}"))
}

def setIdleBrightness(value) {
	logDebug "setIdleBrightness(${value}) was called"
	sendEvent(name: "idleBrightness", value: value, descriptionText: getDescriptionText("idleBrightness set to ${value}"))
}

def setHumidity(humidity) {
	logDebug "setHumidity(${humidity}) was called"
	sendEvent(name: "humidity", value: humidity, unit: "%", descriptionText: getDescriptionText("humidity set to ${humidity}%"))
}

def setThermostatOperatingState (operatingState) {
	logDebug "setThermostatOperatingState (${operatingState}) was called"
	sendEvent(name: "thermostatOperatingState", value: operatingState, descriptionText: getDescriptionText("thermostatOperatingState set to ${operatingState}"))
}

def setSupportedThermostatFanModes(fanModes) {
	logDebug "setSupportedThermostatFanModes(${fanModes}) was called"
	sendEvent(name: "supportedThermostatFanModes", value: fanModes, descriptionText: getDescriptionText("supportedThermostatFanModes set to ${fanModes}"))
}

// (auto, cool, emergency heat, heat, off)
def setSupportedThermostatModes(modes) {
	logDebug "setSupportedThermostatModes(${modes}) was called"	
	sendEvent(name: "supportedThermostatModes", value: modes, descriptionText: getDescriptionText("supportedThermostatModes set to ${modes}"))
}

def setHysteresis(value) {
	logDebug "setHysteresis(${value}) was called"
	sendEvent(name: "hysteresis", value: value, descriptionText: getDescriptionText("hysteresis set to ${value}"))
	setDisplayHysteresis()
}

def setTargetHysteresis(value) {
	logDebug "setTargetHysteresis(${value}) was called"
	sendEvent(name: "targetHysteresis", value: value, descriptionText: getDescriptionText("targetHysteresis set to ${value}"))
	setDisplayHysteresis()
}

def setErrorCheck(value) {
	logDebug "setErrorCheck(${value}) was called"
	sendEvent(name: "errorCheck", value: value, descriptionText: getDescriptionText("errorCheck set to ${value}"))
}

private eventSend(name,verb,value,unit = ""){
    String descriptionText = "${device.displayName} ${name} ${verb} ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    if (unit != "") sendEvent(name: name, value: value ,descriptionText: descriptionText, unit:unit)
    else  sendEvent(name: name, value: value ,descriptionText: descriptionText)
}

def on() {
    String verb = (device.currentValue("switch") == "on") ? "is" : "was turned"
    eventSend("switch",verb,"on")
    sendEvent(name: "contact", value: "open", isStateChange: forceUpdate)
}

def off() {
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    eventSend("switch",verb,"off")
    sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
}

def active() {
   sendEvent(name: "motion", value: "active", isStateChange: forceUpdate) 
}

def inactive() {
    sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
}

def setContact(value) {
	logDebug "setContact(${value}) was called"
	sendEvent(name: "contact", value: value, descriptionText: getDescriptionText("contact set to ${value}"))
}

def setMotion(value) {
	logDebug "setMotion(${value}) was called"
	sendEvent(name: "motion", value: value, descriptionText: getDescriptionText("motion set to ${value}"))
}

def setOnInMin(min) {
    logDebug "setOnInMin(${min}) was called"
    def sec = (min * 60).toInteger()
    runIn(sec, turnOn)
}

def turnOn() {
  on()
}

//def auto() { setThermostatMode("auto") }

def cool() { setThermostatMode("cool") }

def emergencyHeat() { setThermostatMode("heat") }

def heat() { setThermostatMode("heat") }
def thermoOff() { setThermostatMode("off") }

def setThermostatMode(mode) {
	sendEvent(name: "thermostatMode", value: "${mode}", descriptionText: getDescriptionText("thermostatMode is ${mode}"))
	updateThermostatSetpoint(mode)
	runIn(1, manageCycle)
}

def fanAuto() {
    setThermostatFanMode("auto") 
}
def fanCirculate() {
    setThermostatFanMode("circulate") 
}
def fanOn() { 
    setThermostatFanMode("on") 
}

def setThermostatFanMode(fanMode) {
	sendEvent(name: "thermostatFanMode", value: "${fanMode}", descriptionText: getDescriptionText("thermostatFanMode is ${fanMode}"))
}

def updateThermostatSetpoint(mode) {
	if (mode == "cool") {setThermostatSetpoint(device.currentValue("coolingSetpoint"))}
	if (mode == "heat") {setThermostatSetpoint(device.currentValue("heatingSetpoint"))}
}

def setThermostatSetpoint(setpoint) {   
	logDebug "setThermostatSetpoint(${setpoint}) was called"
	updateSetpoints(setpoint, null, null)
}

def setCoolingSetpoint(setpoint) {
	logDebug "setCoolingSetpoint(${setpoint}) was called"
	updateSetpoints(null, null, setpoint)
	runIn(3, manageCycle)
}

def setHeatingSetpoint(setpoint) {
	logDebug "setHeatingSetpoint(${setpoint}) was called"	
	updateSetpoints(null, setpoint, null)
	runIn(1, manageCycle)
}


def setPresence(value) {
	logDebug "setPresence(${value}) was called"
	sendEvent(name: "presence", value: value, descriptionText: getDescriptionText("presence set to ${value}"))
	sendEvent(name: "acStatus", value: value, descriptionText: getDescriptionText("acStatus set to ${value}"))
    if (value == "fan") setAcStatusIcon("ac-fan.svg")
    if (value == "cool") setAcStatusIcon("ac-cool.svg")
    if (value == "off")  setAcStatusIcon("ac-off.svg")
}

def setAcStatusIcon(img) {
    logDebug "setAcStatusIcon(${img}) was called"
    def current = device.currentValue("iconFile")
    logDebug "Image Match is ${current == img}"  
    if (current != img) {
        sendEvent(name: "acStatusIcon", value: "<img class='icon' src='${settings?.iconPath}${img}' />")
        sendEvent(name: "iconFile", value: img) 
    }
}

private updateSetpoints(sp = null, hsp = null, csp = null) {

	if (hsp == null) hsp = device.currentValue("heatingSetpoint",true)
	if (csp == null) csp = device.currentValue("coolingSetpoint",true)
	def thermostatMode = device.currentValue("thermostatMode",true)

	def hspChange = isStateChange(device,"heatingSetpoint",hsp.toString())
	def cspChange = isStateChange(device,"coolingSetpoint",csp.toString())
	def spChange
	if (sp == null) {spChange = false}
	else {spChange == true}
	logDebug("cspChange is ${cspChange}")

	def descriptionText
	def name
	def value
	def unit = "°${location.temperatureScale}"

	if (hspChange) {
		if (thermostatMode == "heat") {
			sp = hsp
			spChange = true
		}
	}

	if (cspChange) {
		if (thermostatMode == "cool") {
			sp = csp
			spChange = true
		}	
	}		

	if (hspChange) {
		value = hsp
		name = "heatingSetpoint"
		descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
		if (txtEnable) log.info descriptionText
		sendEvent(name: name, value: value, descriptionText: descriptionText, unit: unit, stateChange: true)
	}
	if (cspChange) {
		value = csp
		name = "coolingSetpoint"
		logDebug("coolingSetpoint is ${value}")
		descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
		if (txtEnable) log.info descriptionText
		sendEvent(name: name, value: value, descriptionText: descriptionText, unit: unit, stateChange: true)
	}
	if (spChange) {
		value = sp
		name = "thermostatSetpoint"
		descriptionText = "${device.displayName} ${name} was set to ${value}${unit}"
		if (txtEnable) log.info descriptionText
		sendEvent(name: name, value: value, descriptionText: descriptionText, unit: unit, stateChange: true)
        runIn(1, manageCycle)
	}
	runIn(1, setDisplays)
}

def setSchedule(schedule) {
	sendEvent(name: "schedule", value: "${schedule}", descriptionText: getDescriptionText("schedule is ${schedule}"))
}

private sendTemperatureEvent(name, val) {
	sendEvent(name: "${name}", value: val, unit: "°${getTemperatureScale()}", descriptionText: getDescriptionText("${name} is ${val} °${getTemperatureScale()}"), isStateChange: true)
}

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