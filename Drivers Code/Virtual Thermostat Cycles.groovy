/*
	Virtual Thermostat Cycles 
    Version 1.0  2/10/24

    This Driver is for a virtual thermostat with Added Features 
	
	Added features:
    - Contact Sensor capability opens and closes contact with heat thermostatMode (for running alexa connected routines to turn on/off heater fan or Aux Heat)
	- Switch capability to turn on/off the contact sensor (AC fan or aux heat) as an on/off switch tile
    - Motion capability changes motion with cool thermostatMode.  Is active when cooling, inactive when off/fan only (for running alexa connected air conditioner routines ac state)
    - setRunInMin() added to turn on the Switch after a delay in Minutes
	
    Version 2.0 2/13/24
    - refactored to use cycling.  
    - Ramping:  When below setpoint +- hysteresis, will ramp until it gets to that point.  The cycleWait flag is set to not cycle during this time
    - Cycling:  Will cycle when temp is below setpoint +- cyclingHystresis.  Duration for cyclingSeconds is based on cycles per hour chosen in prefrences initially.
                Cycling will terminate if temp goes over cycling point
                The cycleWait flag is set at end of cycle.  It is cleared after setCycleSeconds or if temp goes back above cycling point. 
    - Stopping:  Cycles are timed, and they stop when done or when the temp rises above the stop hysteresis temp.  

	Version 2.1 3/1/24
	- Cycle times are now self adjusting based on last run.  If cycles go over target they are reduced, under target the are increaed. 
	- Tracks rise and fall changes to determine cycle highs and lows for calcs.  
	- Cycles are counted.  If it takes more than two cycles to get to temp, cycle time is increased regardless of end temp.  
	- Center the cycles around the setpoint by adjusting the cycle hysteresis based on averge of cycle high/low. 
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

		attribute "supportedThermostatFanModes", "JSON_OBJECT"
		attribute "supportedThermostatModes", "JSON_OBJECT"
		attribute "hysteresis", "NUMBER"
        attribute "stopHysteresis", "NUMBER"
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
		attribute "outsideTemp", "NUMBER"
		attribute "temperatureState", "ENUM"
		attribute "cycleSeconds", "ENUM"
		attribute "waitSeconds", "ENUM"
		//attribute ""

		// Commands needed to change internal attributes of virtual device.
		command "setTemperature", ["NUMBER"]
		command "setThermostatOperatingState", ["ENUM"]
		command "setThermostatSetpoint", ["NUMBER"]
		command "setSupportedThermostatFanModes", ["JSON_OBJECT"]
		command "setSupportedThermostatModes", ["JSON_OBJECT"]
        command "setThermostatFanMode", [[name:"thermostatFanMode",type:"ENUM", description:"Thermo Fan Mode", constraints:["off","cool","fan"]]]
        command "setHysteresis", ["NUMBER"]
        command "setStopHysteresis", ["NUMBER"]
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
        command "setCycleState",[[name:"cycleState",type:"ENUM", description:"Cycling State", constraints:["Cycling","Ramping","Waiting"," "]]]
		command "setOutsideTemp", ["NUMBER"]
		command "resetCycleWait"
		command "stopCycle"
		command "initCycling"
		command "manageCycle"
		command "initCycling"
		command "setCycleSeconds", ["ENUM"]
		command "setWaitSeconds", ["ENUM"]
		command "setTemperatureState",[[name:"temperatureState",type:"ENUM", description:"Rise/Fall State", constraints:["rising","rising steady","falling","falling steady"]]]
	}

	preferences {
        input( name: "iconPath", type: "string", description: "Address Path to icons", title: "Set Icon Path", defaultValue: "https://cburges2.github.io/ecowitt.github.io/Dashboard%20Icons/")
		input( name: "useACState", type:"bool", title: "Enable using AC State and Icon",defaultValue: false)
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
		input( name: "zoneType",type:"enum",title: "Zone Type", options:["Well Insulated","Normal","Drafty","Basement"], description:"Pick the type of Zone", defaultValue: "Normal")
		input( name: "adjustCycleMinutes",type:"enum",title: "Adjust Cycle Minutes", options:["-10","-9","-8","-7","-6","-5","-4","-3","-2","-1","0","1","2","3","4","5","6","7","8","9","10"], description:"Lower if temp goes too high(heat)/low(cool) when cycling", defaultValue: 0)
		input( name: "adjustIntervalMinutes",type:"enum",title: "Adjust Interval Minutes", options:["-10","-9","-8","-7","-6","-5","-4","-3","-2","-1","0","1","2","3","4","5","6","7","8","9","10"], description:"Lower if temp goes too low(heat)/high(cool) between cycles", defaultValue: 0)
		input( name: "secondsPerTenths",type:"enum",title: "Adjust Cycle and Wait Seconds per tenths over/under target", options:["0","1","2","3","4","5","8","10","12","15","20","25","30","35","40","45"], description:"Change if cycles are over/under correcting", defaultValue: 20)
	}
}

def installed() {
	log.warn "installed..."

	updated()
	initialize()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	log.warn "description logging is: ${txtEnable == true}"
	if (logEnable) runIn(21600,logsOff)   // 6 hours

	//state.numCycles = 1.0

	//runIn(1,initCycling)
}

def initialize() {
	initAttributes()
	initStates()
	runIn(1,initCycling)
}

def initAttributes() {
	sendEvent(name: "temperature", value: convertTemperatureIfNeeded(68.0,"F",1))
	sendEvent(name: "thermostatSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
	sendEvent(name: "heatingSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
	sendEvent(name: "coolingSetpoint", value: convertTemperatureIfNeeded(75.0,"F",1))
	state.lastRunningMode = "heat"
	updateDataValue("lastRunningMode", "heat")
	setThermostatOperatingState("heat")
	setSupportedThermostatFanModes(["auto","circulate","on"])
	setSupportedThermostatModes(["auto", "cool",  "heat", "off"])
	thermoOff()
	fanAuto()
	setCyclingHysteresis(".08")
	setStopHysteresis(".1")
	setCycling("false")
	setCycleState("idle")
	sendEvent(name: "hysteresis", value: (hysteresis ?: 0.5).toBigDecimal())    
}

def initStates() {

	def temperature = device.currentValue("temperature").toBigDecimal()

	// used for cycles
	state.intervalSeconds = 0
	state.cycleSeconds = 0

	// set by prefs
	state.setIntervalSeconds = 0
	state.setCycleSeconds = 0

	// calc loss
	state.calcLoss = false
	state.waitEndTemp = temperature
	state.fallEndTemp = temperature
	state.steadyFallTemp = temperature

	// calc gain
	state.calcGain = false
	state.riseEndTemp = temperature
	state.steadyRiseTemp = temperature
	state.numCycles = 0.0

	state.cycleinterval = 0
	state.cycleMinutes = 0

	// Cycle state flags
	state.ramping = false
	state.cyclingOn = false
	state.cycleWait = false
	state.overWait = false

	// temp status
	state.tempState = "falling"
	state.lastTemp = temperature

	state.calcCycleHysteresis = false
	state.lastRunningMode = "idle"
}

def initCycling() {
	logDebug("Calculating Cycle States")

	setCyclingHysteresis(".08"); setStopHysteresis(".1")

    state.intervalSeconds = state?.cycleInterval * 60
	setIntervalSecondsAttrib(state?.intervalSeconds.toString())

    // set cycle initial mins based on 32 degrees outside "Well Insulated","Normal","Drafty","Basement"
	def cycleMins = 0
	logDebug("zoneType is ${zoneType}")
	if (zoneType == "Drafty") {cycleMins = 11; intervalMins = 9; setCyclingHysteresis(".08"); setStopHysteresis(".15")} 
	else if (zoneType == "Normal") {cycleMins = 5; intervalMins = 8; setCyclingHysteresis(".08"); setStopHysteresis(".23")} 
	else if (zoneType == "Well Insulated") {cycleMins = 3; intervalMins = 5; setCyclingHysteresis(".03"); setStopHysteresis(".18")} 
	else if (zoneType == "Basement") {cycleMins = 2; intervalMins = 7; setCyclingHysteresis(".01"); setStopHysteresis(".16")} 

	def cycleMinutes = (Math.ceil(cycleMins)).toInteger()	
	def cycleSeconds = (Math.ceil(cycleMins) * 60).toInteger()

	logDebug("Cycle Seconds are ${cycleSeconds}")

	def intervalMinutes = (Math.ceil(intervalMins)).toInteger()
	def intervalSeconds = (Math.ceil(intervalMins) * 60).toInteger()
	logDebug("Interval Seconds are ${intervalSeconds}")

	// set states
	state.cycleMinutes = cycleMinutes
	state.setCycleSeconds = cycleSeconds
	state.setIntervalSeconds = intervalSeconds

	// get these base values modified by change settings in prefrences
	updateCycleSeconds()

}

def setCycleSeconds(seconds) {
	state.cycleSeconds = seconds.toInteger()
	setCycleSecondsAttrib(seconds)
}

def setWaitSeconds(seconds) {
	state?.intervalSeconds = seconds.toInteger()
	setWaitSecondsAttrib(seconds)
}

// use settings in prefrences to adjust cycle and interval times from cycle minutes defaults
def updateCycleSeconds() {

	// get current values
	def setCycleSeconds = state?.setCycleSeconds
	logDebug("Set Seconds is ${setCycleSeconds}")
	def setIntervalSeconds = state?.setIntervalSeconds
	logDebug("Set Interval is ${setIntervalSeconds}")

	// get offset values
	def cycleChangeSeconds = settings?.adjustCycleMinutes.toInteger() * 60
	logDebug("Change Cycle Seconds is ${cycleChangeSeconds}")
	def intervalChangeSeconds = settings?.adjustIntervalMinutes.toInteger() * 60
	logDebug("Change Interval Seconds is ${intervalChangeSeconds}")

	// update the seconds
	def newCycleSeconds = setCycleSeconds + cycleChangeSeconds
	logDebug("New Cycle Seconds is ${newCycleSeconds}")
    def newIntervalSeconds = setIntervalSeconds + intervalChangeSeconds
	logDebug("New Interval Seconds is ${newIntervalSeconds}")

	// update states
	state.cycleSeconds = newCycleSeconds
	setCycleSecondsAttrib(newCycleSeconds.toString())
	state.intervalSeconds = newIntervalSeconds
	setWaitSecondsAttrib(newIntervalSeconds.toString())
}

// **************************** Mangage Cycle *********************************
def manageCycle(){

    def cyclingHysteresis = (device.currentValue("cyclingHysteresis").toBigDecimal())   // start cycle if falling temp, stop cycle if still rising
	def coolingSetpoint = device.currentValue("coolingSetpoint").toBigDecimal()
	def heatingSetpoint = device.currentValue("heatingSetpoint").toBigDecimal()
	def temperature = (device.currentValue("temperature") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()

	def hysteresis = (device.currentValue("hysteresis")).toBigDecimal() // start ramp
	def thermostatMode = device.currentValue("thermostatMode") ?: "off"

	def highTarget = 0.0
	def lowTarget = 0.0
	def cycleTarget = 0.0
	def lowLimit = 0.0

	if (thermostatMode == "heat") {
		highTarget = (heatingSetpoint + cyclingHysteresis)
		lowTarget = (heatingSetpoint - hysteresis) + 0.05
		cycleTarget = heatingSetpoint + cyclingHysteresis
		lowLimit = lowTarget + 0.1
	}
	if (thermostatMode == "cool") {
		highTarget= (coolingSetpoint - cyclingHysteresis)
		lowTarget = (coolingSetpoint + hysteresis) - 0.05
		cycleTarget = coolingSetpoint - cyclingHysteresis
		lowLimit = lowTarget - 0.1
	} 

	def rising = state?.tempState == "rising" || state?.tempState == "rising steady"
	def falling = state?.tempState == "falling" || state?.tempState == "falling steady"	

	// CALC LOSS -only once after a wait 
	if (state.calcLoss) {
		logDebug("************* calcLoss Running **************")   
/* 		logDebug("low Target is ${lowTarget}")
		def waitEndTemp = state?.waitEndTemp
		logDebug("Wait End Temp is ${waitEndTemp}")
		def adjustWait = false
		def endDiff = 0.0 */

/* 		// Check if temp still below heating setpoint at end of wait (should be above)  ---?? keep?
		if (waitEndTemp > cycleTarget && rising) { 
			logDebug("Temperature under setpoint at wait end")

			endDiff = heatingSetpoint - waitEndTemp
			logDebug("Wait endDiff is ${endDiff}")

			adjustWait = true
		} */
/*
		// check if temp is below cycle point and falling (too long of a wait, cycle would have started otherwise)
		if ((waitEndTemp < cycleTarget) && falling) { 
			logDebug("Temperature under setpoint at wait end")

			endDiff = cycleTarget - waitEndTemp
			logDebug("Wait endDiff is ${endDiff}")

			adjustWait = true
		}

		// Adjust wait time
		if (adjustWait) {
			def secPerTenth = secondsPerTenths.toBigDecimal()
			logDebug("Seconds per tenth is ${secPerTenth}")

			// find how many tenths we are off by and adjust by seconds per tenths
			def numAdjust = endDiff / 0.1     
			logDebug("numAdjust (diff /.1) is ${numAdjust}")
			
			def diffSec = numAdjust * secPerTenth
			def diffSeconds = Math.round(diffSec)
			def absDiff = Math.abs(diffSeconds)

			if (absDiff <= 300) {
				adjustWaitSeconds(diffSeconds)    // add to wait to get above setpoint
			}
		} */

		state.calcLoss = false			
	}

	// CALC CYCLE HYSTERESIS - only once per cycle
	if (state?.calcCycleHysteresis) {
 		logDebug("************* calcCycleHysteresis Running **************")   

		def endTemp = state?.fallEndTemp
		def highTemp = state?.riseEndTemp
		logDebug("Fall End Temp is ${endTemp}")
		logDebug("Rise End Temp is ${highTemp}")

		def avg = (highTemp + endTemp) / 2.0
		def diff = 0.0
		def update = false

		if (thermostatMode == "heat") {
			diff = heatingSetpoint - avg
			update = true
		}

		if (thermostatMode == "cool") {
			diff = coolingSetpoint + avg
			update = true
		}	

		if (update) {
			logDebug("Adjusting ${diff} degrees from cyclingHysteresis based on fall end temp")
			def setHyst = device.currentValue("cyclingHysteresis").toBigDecimal()
			def newHyst = setHyst + diff
			logDebug("New Cycle Hysteresis is ${newHyst} degrees")

			// Adjust Hysteresis
			if (newHyst < 0.0) {newHyst = 0.0}
			if (newHyst > 0.5) {newHyst = 0.3}
			setCyclingHysteresis(newHyst)
		}

		state.calcCycleHysteresis = false
	}	

	// CALC GAIN - only once after a cycle - do not calc if it took two cycles to get above setpoint (low cycle), or if cyle was stopped above cycleHysteresis  
	if (state?.calcGain) {
		logDebug("************* calcGain Running **************")
		state.calcGain = false
		def count = state?.numCycles
		logDebug("Cycle Count is ${count}")
		state.numCycles = 0.0
		//def shortCycle = state?.cycleStopped && count == 1.0
		//if (!shortCycle) {		  								 //!state?.lowCycle && 				
			logDebug("High Target is ${highTarget}")
			def highTemp = state?.riseEndTemp
			logDebug("Rise End Temp is ${highTemp}")

			def highDiff = highTarget - highTemp
			logDebug("highDiff is ${highDiff}")

			def secPerTenth = secondsPerTenths.toBigDecimal()
			logDebug("Seconds per tenth is ${secPerTenth}")
					
			// find how many tenths over we are and adjust by seconds per tenths
			numAdjust = highDiff / 0.1     
			logDebug("numAdjust (diff /.1) is ${numAdjust}")
			
			def diffSec = numAdjust * secPerTenth
			logDebug("Calculated DiffSec is ${diffSec}")

			def diffSeconds = Math.round(diffSec).toInteger()  // diff as int
			def absDiff = Math.abs(diffSeconds)    // diff as positive value

			// if multiple cycles needed, multiply by cycles it took to get to temp and double the limit
			def adjustLimit = 600

			// treat two cycles as one, but reduce difference a bit
			if (count == 2 && diffSeconds > 60) {
				diffSeconds = diffSeconds - 30
				logDebug("Count was two: diffSeconds now ${diffSeconds}")
			}

			if (count > 2) {
				logDebug("The cycle count is ${count}")
				adjustLimit = 800
				diffSeconds = (60.0 * (count - 1)).toInteger() // create a new difference for the extra cycles
				logDebug("Multi-cycle diffSeconds is now ${diffSeconds}")
			}					

			if (absDiff > adjustLimit) {
				if (diffSeconds > 0) {diffSeconds = adjustLimit.toInteger()}
				else if (diffSeconds < 0) {(difSeconds = (0 - adjustLimit)).toInteger()}
			}

			// adjust
			adjustCycleSeconds(diffSeconds)
			adjustWaitSeconds(-30)   // adjust wait opposite of cycles
			//state?.addToCycleSecs = diffSeconds
			logDebug("Adjusting cycle time and wait time by ${diffSeconds} seconds based on target cycles")
	}

	def stopHysteresis = (device.currentValue("stopHysteresis").toBigDecimal())	// stop cycle
	def thermostatOperatingState = device.currentValue("thermostatOperatingState") ?: "heat"

    def ramping = false
    def cyclingOn = false

    def coolOn = coolingSetpoint + hysteresis
    def heatOn = heatingSetpoint - hysteresis

    def coolingOn = (temperature > coolOn) 
    def heatingOn = (temperature < heatOn)	

    def heatOff = heatingSetpoint + cyclingHysteresis
    def coolOff = coolingSetpoint - hysteresis 
    def cycleOn
	def belowSetpoint

	// ****** Set Cycling and Ramping Points ********
	def wasRamping = state?.ramping	// save ramp state before it changes
    // ** Set Cycling State **
    if (thermostatMode == "cool") {
        // cycle settings       
        cycleOn = coolingSetpoint - cyclingHysteresis  // cycle setpoint
        cyclingOn = (temperature > cycleOn && temperature <= coolOn && rising) || (temperature > coolingSetpoint && temperature < coolOn && rising)	
        ramping = temperature > coolOn  // Check if rampting to temp
		belowSetpoint = temperature > (coolingSetpoint + 0.1)
    }
    if (thermostatMode == "heat") {
        // cycle settings
        cycleOn = heatingSetpoint + cyclingHysteresis  // cycle setpoint
        logDebug("cycleOn is ${heatingSetpoint + cyclingHysteresis}")
        logDebug("temp is ${temperature}")
        cyclingOn = (temperature < cycleOn && temperature >= heatOn && falling) || (temperature < heatingSetpoint && temperature > heatOn && falling)
        ramping = temperature < heatOn  // Check if rampting to temp   
		belowSetpoint = temperature < (heatingSetpoint - 0.1)
    }
	// set States to match
	state.ramping = ramping
	state.cyclingOn = cyclingOn	

	// start ramp wait if just done ramping (two minute wait to check temp)
	if (wasRamping && !ramping) {
		logDebug("Ramping is done, setting wait")
		setRampWait()
	}

	if (ramping) {heatingOn = true}

	logDebug("Ramping is ${ramping}")
	logDebug("Cycling On is ${cyclingOn}")

    // cancel cycleWait when below setpoint and falling
    if (state?.cycleWait && belowSetpoint && falling) {		// *****************?
		logDebug("Stoping Wait, need to cycle at ${temperature} with temps falling")
		state.overWait = true
		// Bump up cycle time
		adjustCycleSeconds(30)
		resetCycleWait()   // allow another cycle to run	
	}  																	
    
    // Start cycle if below cycle temp and not waiting or ramping
    if (cyclingOn && !ramping && state?.cycleWait == false) {
    	logDebug("OK to Cycle")	
		if (device.currentValue("cycling") == "false") {
			logDebug("Statring cycling with temp change")
			startCycle()
		}
    }

    def cycling = device.currentValue("cycling") == "true"

 	// stop if cycling and at stop hysteresis with temp rising (heatOff is cycle point)
	logDebug("Temp State is ${state?.tempState}")
	def changeOK = falling
	if (thermostatMode == "cooling") changeOK = rising
	if (cycling && temperature >= heatOff) {  
		logDebug("Stopping cycle at stop hysteresis while temp rising")
		stopCycle()
		cyclingOn = false                 		
		cycling = false
		// bump down cycle time
		adjustCycleSeconds(-30)
	} else if ((temperature >= heatOff) && thermostatOperatingState == "heating") {  // if heat is on even if not cycling?
		heatingOn = false
		stopCycle()
		cyclingOn = false
	}

    logDebug("cycling is ${cycling}")   
	def waitingState = device.currentValue("cycleState") == "Waiting"
	logDebug("waiting is ${waitingState}")
    
	// if rise beyond cyclingOn point and cycling, stop.
    if (!cyclingOn && cycling) {
        logDebug("Stopping cycle at cycling setpoint")
		stopCycle()
    }

	// if waiting, and temps less than or equal to heating setpoint, and falling, start a cycle
	if (state?.cycleWait && falling && temperature <= heatingSetpoint) {
		stopCycleWait()
	}

    // set cycleState attribute for dashboard
	def cycleStateValue = device.currentValue("cycleState")
	def newState = " "
    if (ramping) {newState = "Ramping"}
    else if (cycling) {newState = "Cycling"}
    else if (state?.cycleWait == true) {newState = "Waiting"}
	else newState = "Idle"
	
	if (!cycleStateValue.equals(newState)) {
		logDebug("updateing cycleState to ${newState}")
		setCycleState(newState)
	}
    

    // Check cycle/ramping
    if (cycling && !ramping) { 
        if (thermostatMode == "heat") {
            if (cyclingOn) {heatingOn = true}
        } else if (thermostatMode == "cool") {
            if (cyclingOn) {coolingOn = true}
        }
    } else if (heatingOn) {
        if (thermostatMode == "heat" && ramping) heatingOn = (temperature <= heatingSetpoint - hysteresis)
		if (thermostatMode == "heat" && temperature >= heatOff) heatingOn = false
    } else if (coolingOn) {
        if (thermostatMode == "cool" && ramping) coolingOn = (temperature >= coolingSetpoint + hysteresis)
		if (thermostatMode == "cool" && temperature <= coolOff) coolingOn = false
    }

	if (temperature > (heatingSetpoint + stopHysteresis) && thermostatOperatingState == "heating")   // fail safe off
	if (ramping && !heatingOn) {heatingOn = true}

    // ***** Set Operating State ******
    // Cool
	if (thermostatMode == "cool") {
        if (coolingOn && thermostatOperatingState != "cooling") {
            setThermostatOperatingState("cooling")
            sendEvent(name: "motion", value: "active", isStateChange: forceUpdate) 
			on()    		
        }    
        else if (!coolingOn && thermostatOperatingState != "idle") {
            setThermostatOperatingState("idle")
            sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
			off()
        }
    // Heat    
	} else if (thermostatMode == "heat") {
		if (heatingOn && thermostatOperatingState != "heating") {
			setThermostatOperatingState("heating")
            sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate) 
		}
		else if (!heatingOn && thermostatOperatingState != "idle") {
			setThermostatOperatingState("idle")
            sendEvent(name: "contact", value: "open", isStateChange: forceUpdate) 
		}
    // Auto not implemented with cycling
	} 
}
// ****************************************** End Mangage Cycle ***************************************************

def startCycle() {
	logDebug("Starting Cycle")
	setCycling("true")
	runIn(state?.cycleSeconds, stopCycle)
}

def stopCycle() {
    logDebug("Ending Cycle")		
    if (device.currentValue("cycling") == "true") {
		if (state.numCycles < 5) {state.numCycles = state?.numCycles + 1.0}		
		setCycling("false")
		unschedule("stopCycle")
		setCycleWait()
		logDebug("Number of cycles at Stop Cycle is ${state?.numCycles}")
	}

    runIn(1,manageCycle)
}

// Start Another Cycle - check when when wait ?
def restartCycle() {
	def waiting = device.currentValue("cycleState") == "Waiting"
	def state = device.currentValue("temperatureState")
	def falling = (state == "falling") || (state == "falling steady")	
	if (state?.cyclingOn && falling) {   // if cyclingOn and not waiting and not rising, start another cycle. 
		startCycle()	
		logDebug("Starting another Cycle...")		
	}
}

def setCycleWait() {
	logDebug("setting Cycle Wait")
	state.cycleWait = true
	runIn(1,manageCycle)    	
	runIn(state?.intervalSeconds, stopCycleWait)  	
}

def setRampWait() {
	logDebug("setting Ramp Wait")
	state.cycleWait = true
	runIn(1,manageCycle)    	
	runIn(120, stopCycleWait)  	// may need another cycle
}

def stopCycleWait() {
	logDebug("Resetting Cycle Wait")
	unschedule("resetCycleWait")
    state.cycleWait = false
	setCycleState(" ")
	state.calcLoss = true
	runIn(1,manageCycle)
}

// Recheck to see if ramping is done if ramping
def recheckCycleWait() {
	runIn(1,setCycleWait)
}

// reset cycle wait if we do not get to cycle temp from ramp
def resetCycleWait() {
	logDebug("Resetting Cycle Wait")
	unschedule("resetCycleWait")
    state.cycleWait = false
	setCycleState(" ")

    runIn(1,manageCycle)
}

def setCycleState (value) {
	logDebug "setCycleState(${value}) was called"
	sendEvent(name: "cycleState", value: value, descriptionText: getDescriptionText("cycleState set to ${value}"))    
}


// cycleSeconds is used, setSeconds is set in init
// intervalSeconds are used, cycleIntervalSeconds is set in init  
// adjust cycle seconds based on auto adjust diff
def adjustCycleSeconds(diff) {

	def cycleSeconds = state?.cycleSeconds
	logDebug("Cycle Seconds is ${cycleSeconds}")

	def setpointChangeSecs = diff.toInteger()

	def newSeconds = cycleSeconds + diff 
	logDebug("New Seconds is ${newSeconds}")

	def change = newSeconds - cycleSeconds

	// limit a change to 200 seconds
	if (change > 200.0) {
		newSeconds = cycleSeconds + 200.0
	} else if (change < -200 ) {
		newSeconds = cycleSencods - 200.0
	}

	// limit the range that can be updated to
	if (newSeconds < 30)  {newSeconds = 30}
	if (newSeconds > 1200)  {newSeconds = 1200}

	state.cycleSeconds = newSeconds
	setCycleSecondsAttrib(newSeconds.toString())
	logDebug("Changed Cycle is ${newSeconds}")
}

// adjust cycle seconds based on auto adjust diff
def adjustWaitSeconds(diff) {

    // set new interval seconds too for cycleWait
	def interval = state?.intervalSeconds
	logDebug("set Interval is ${interval}")

    def newInterval = interval + diff.toInteger()

	logDebug("New Interval is ${newInterval}")

	if (newInterval < 300) {newInterval = 300}  // keep wait at at least 300 sec
	if (newInterval > 750) {newInterval = 750}  // keep below 750
  
	state.intervalSeconds = newInterval
	setWaitSecondsAttrib(newInterval.toString())
	logDebug("Changed Interval is ${newInterval}")
}

def setCycleSecondsAttrib(value) {
	sendEvent(name: "cycleSeconds", value: value, descriptionText: getDescriptionText("cycleSeconds Attribute set to ${value}"))	
}

def setWaitSecondsAttrib(value) {
	sendEvent(name: "waitSeconds", value: value, descriptionText: getDescriptionText("waitSeconds Atrribute set to ${value}"))	
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
		//tempState = "falling"
		if (lastTempState == "falling steady") {
			tempState = "falling"
			state.riseEndTemp = state?.steadyFallTemp  // temp to use for 
			state.calcGain = true	
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
		//tempState = "rising"
		if (lastTempState == "rising steady") {
			tempState = "rising"	
			state.fallEndTemp = state?.steadyRiseTemp	
			state.calcCycleHysteresis = true
			logDebug("Set Flag to Calc Hysteresis")
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
	}
	logDebug("temperatureState is ${tempState}")	
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
}

def setTemperatureState(value) {
	sendEvent(name: "temperatureState", value: value, descriptionText: getDescriptionText("temperatureState set to ${value}"))	
}

def setOutsideTemp(value) {
	logDebug "setOutsideTemp(${value}) was called"
	sendEvent(name: "outsideTemp", value: value, descriptionText: getDescriptionText("outsideTemp set to ${value}"))
	runIn(1,manageCycle)
}

def setCyclingHysteresis(value) {
	logDebug "setCyclingHysteresis(${value}) was called"
	sendEvent(name: "cyclingHysteresis", value: value, descriptionText: getDescriptionText("cyclingHysteresis set to ${value}"))
	//def stop = (value.toBigDecimal() + 0.15).toString()
	//setStopHysteresis(stop)
}

// Commands needed to change internal attributes of virtual device.
def setCycling(value) {
	logDebug "setCycling(${value}) was called"
	sendEvent(name: "cycling", value: value, descriptionText: getDescriptionText("cycling set to ${value}"))
	runIn(3, manageCycle)
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
	updateSetpoints(null,null,null,operatingState)
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
}

def setStopHysteresis(value) {
	logDebug "setStopHysteresis(${value}) was called"
	sendEvent(name: "stopHysteresis", value: value, descriptionText: getDescriptionText("stopHysteresis set to ${value}"))
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

def auto() { setThermostatMode("auto") }

def cool() { setThermostatMode("cool") }

def emergencyHeat() { setThermostatMode("heat") }

def heat() { setThermostatMode("heat") }
def thermoOff() { setThermostatMode("off") }

def setThermostatMode(mode) {
	sendEvent(name: "thermostatMode", value: "${mode}", descriptionText: getDescriptionText("thermostatMode is ${mode}"))
	setThermostatOperatingState ("heat")
	updateSetpoints(null, null, null, mode)
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

def setThermostatSetpoint(setpoint) {   
	logDebug "setThermostatSetpoint(${setpoint}) was called"
	updateSetpoints(setpoint, null, null, null)
}

def setCoolingSetpoint(setpoint) {
	logDebug "setCoolingSetpoint(${setpoint}) was called"
	updateSetpoints(null, null, setpoint, null)
	runIn(1,initCycling)
	runIn(3, manageCycle)
}

def setHeatingSetpoint(setpoint) {
	logDebug "setHeatingSetpoint(${setpoint}) was called"	
	updateSetpoints(null, setpoint, null, null)
	//runIn(1,initCycling)
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

private updateSetpoints(sp = null, hsp = null, csp = null, operatingState = null){
	if (operatingState in ["off"]) return
	if (hsp == null) hsp = device.currentValue("heatingSetpoint",true)
	if (csp == null) csp = device.currentValue("coolingSetpoint",true)
	if (sp == null) sp = device.currentValue("thermostatSetpoint",true)

	if (operatingState == null) operatingState = state.lastRunningMode

	def hspChange = isStateChange(device,"heatingSetpoint",hsp.toString())
	def cspChange = isStateChange(device,"coolingSetpoint",csp.toString())
	def spChange = isStateChange(device,"thermostatSetpoint",sp.toString())
	def osChange = operatingState != state.lastRunningMode

	def newOS
	def descriptionText
	def name
	def value
	def unit = "°${location.temperatureScale}"
	switch (operatingState) {
		case ["pending heat","heating","heat"]:
			newOS = "heat"
			if (spChange) {
				hspChange = true
				hsp = sp
			} else if (hspChange || osChange) {
				spChange = true
				sp = hsp
			}
			if (csp - 2 < hsp) {
				csp = hsp + 2
				cspChange = true
			}
			break
		case ["pending cool","cooling","cool"]:
			newOS = "cool"
			if (spChange) {
				cspChange = true
				csp = sp
			} else if (cspChange || osChange) {
				spChange = true
				sp = csp
			}
			if (hsp + 2 > csp) {
				hsp = csp - 2
				hspChange = true
			}
			break
		default :
			return
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

	state.lastRunningMode = newOS
	updateDataValue("lastRunningMode", newOS)
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