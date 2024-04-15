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

	Version 3.0 3/12/24
	- Enough with the adjusting, slope is now calculated during the rise/fall period, and a five-point running averge of slope values is used to calculate a 
	  cycle seconds directly using a room coefficient.  For Heat, Cycle when temp falling below cycle point, wait for a period after the cycle, stop when heating when temperature is rising. 
	  Reverse is true with cool.  Below/above ramp point will constantly ramp to temperature. 
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
		attribute "outsideTemp", "NUMBER"
		attribute "temperatureState", "ENUM"
		attribute "cycleSeconds", "ENUM"
		attribute "waitSeconds", "ENUM"
		attribute "slope", "NUMBER"
		attribute "autoMode", "ENUM"
		attribute "fanSpeed", "ENUM"
		//attribute ""

		// Commands needed to change internal attributes of virtual device.
		command "setTemperature", ["NUMBER"]
		command "setThermostatOperatingState", ["ENUM"]
		command "setThermostatSetpoint", ["NUMBER"]
		command "setAutoSetpoint", ["NUMBER"]
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
        command "setCycleState",[[name:"cycleState",type:"ENUM", description:"Cycling State", constraints:["Cycling","Ramping","Waiting"," "]]]
		command "setOutsideTemp", ["NUMBER"]
		command "setSlope", ["NUMBER"]
		command "stopCycleWait"
		command "stopCycle"
		command "initCycling"
		command "manageCycle"
		command "initCycling"
		command "setCycleSeconds", ["ENUM"]
		command "setWaitSeconds", ["ENUM"]
		command "setCycleWait"
		command "updateCycleSeconds"
        command "setAutoMode",[[name:"autoMode",type:"ENUM", description:"Auto Switchover Mode", constraints:["true","false"]]]
		command "setFanSpeed",[[name:"fanSpeed",type:"ENUM", description:"AC Fan Speed", constraints:["low","medium","high"]]]
		command "setTemperatureState",[[name:"temperatureState",type:"ENUM", description:"Rise/Fall State", constraints:["rising","rising steady","falling","falling steady"]]]
	}

	preferences {
        input( name: "iconPath", type: "string", description: "Address Path to icons", title: "Set Icon Path", defaultValue: "https://cburges2.github.io/ecowitt.github.io/Dashboard%20Icons/")
		input( name: "useACState", type:"bool", title: "Enable using AC State and Icon",defaultValue: false)
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
		input( name: "autoCoefficient", type:"bool", title: "Enable Auto Adjust of Cycle Coefficient", defaultValue: true)
		//input( name: "adjustCycleMinutes",type:"enum",title: "Adjust Cycle Minutes", options:["-10","-9","-8","-7","-6","-5","-4","-3","-2","-1","0","1","2","3","4","5","6","7","8","9","10"], description:"Lower if temp goes too high(heat)/low(cool) when cycling", defaultValue: 0)
		input( name: "adjustIntervalMinutes",type:"enum",title: "Adjust Interval Minutes", options:["-10","-9","-8","-7","-6","-5","-4","-3","-2","-1","0","1","2","3","4","5","6","7","8","9","10"], description:"Lower if temp goes too low(heat)/high(cool) between cycles", defaultValue: 0)
		input( name: "cycleCoefficient",type:"enum",title: "Coefficient For Calculating Cycle Seconds from Slope", options:["10","15","20","25","30","35","40","45","50","55","60","65","70","75","80","85","90","95","100","110","115","120","125","130","135","140","145","150","155","160","165","170","175","180","185","190","195","200"], description:"Decrease if temps going too far beyond target, Increase if short cycling", defaultValue: 50)
	}
}

def installed() {
	log.warn "installed..."

	updated()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	log.warn "description logging is: ${txtEnable == true}"
	if (logEnable) runIn(21600,logsOff)   // 6 hours

	runIn(1, updateCycleSeconds)  // if coefficient was changed, updates to new seconds

	setSupportedThermostatModes([cool, heat, off])
	setSupportedThermostatFanModes([circulate, fan, on])
	//state.numSlope = 0
	//state.firstEnded = true
	//state.firstSlope = false
	//state.slopeArray =  [0.722,0.717,0.322,0.605,0.422] 
	//state.firstEnded = false
	//state.secondCycle = false
	//state.numCycles = 0.0
	//state.runCycles = 0
	//initialize()
	//initCycling()
	//state.calcLoss = false
}

def initialize() {
	//initAttributes()
	initCycling()
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
	setCyclingHysteresis("0.08")
	setTargetHysteresis("0.1")
	setHysteresis("0.2")
	setCycling("false")
	setCycleState("idle")
}

def initStates() {

	def temperature = device.currentValue("temperature").toBigDecimal()

	// used for cycles
	state.waitSeconds = 0
	state.cycleSeconds = 0

	// set by prefs
	//state.setIntervalSeconds = 0
	//state.setCycleSeconds = 0

	// calc loss
	state.calcLoss = false
	state.waitEndTemp = temperature
	state.fallEndTemp = temperature
	state.steadyFallTemp = temperature

	// calc gain
	state.calcGain = false
	state.riseEndTemp = temperature
	state.steadyRiseTemp = temperature
	//state.numCycles = 0.0
	//state.runCycles = 0

	// Cycle state flags
	state.ramping = false
	state.cycling = false
	state.waiting = false
	state.numCycles = 0.0

	// temp status
	state.tempState = "falling steady"
	state.lastTemp = temperature
	state.highStamp = temperature
	state.lowStamp = temperature

	//state.calcCycleHysteresis = false
	state.lastRunningMode = "idle"

	state.slope = 1.0
	state.lastSlope = 1.0

	state.lastFallStamp = 0
	staet.lastFallTemp = temperature
	state.slopeArray = [0.0]
	state.firstSlope = false
	state.numSlope = 0
}

def initCycling() {
	logDebug("Calculating Cycle States")

	setCycleSeconds(60)
	setWaitSeconds(120)
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

	// *** CALC GAIN *****  flag a new cycle and calc hysteresis to center the cycle (to do)
	if (state?.calcGain) {
		logDebug("********calcGain Running *************")		
		//state.firstEnded = false
		state.calcGain = false
		state.firstSlope = true
		state?.waiting = false

		// check if temp got above target temp (heatSlope or coolSlope)
		if (state?.numSlope >= 5) {
			state.numSlope = 0
			def coefficient = settings?.cycleCoefficient.toBigDecimal()
/* 			if (state?.highTemp > heatOff) {
				try {
					def newCoefficient = (coefficient - 5.0).toInteger()
					def strCoefficient = newCoefficient.toString()
					device.updateSetting("cycleCoefficient",[value:strCoefficient,type:"enum"])
				} catch (Exception e) {
					logDebug("Bad new coefficient calc...${e}")
				}
			} 
			if (state?.highTemp < heatOff) {
				try {
					def newCoefficient = (coefficient + 5.0).toInteger()
					def strCoefficient = newCoefficient.toString()
					device.updateSetting("cycleCoefficient",[value:strCoefficient,type:"enum"])
				} catch (Exception e) {
					logDebug("Bad new coefficient calc...${e}")
				}
			} 	 */		
		}		
	}

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
	
	// CALC LOSS - only once per cycle at bottom of cycle - center hysteresis
	if (state?.calcLoss) {
 		logDebug("************* calcLoss Running **************")   

		def endTemp = state?.fallEndTemp
		def highTemp = state?.riseEndTemp
		logDebug("Fall End Temp is ${endTemp}")
		logDebug("Rise End Temp is ${highTemp}")

		def avg = (highTemp + endTemp) / 2.0
		logDebug("Averge Temp is ${avg}")
		def diff = 0.0

		if (thermostatMode == "heat") {diff = heatingSetpoint - avg}  // positive if low
		if (thermostatMode == "cool") {diff = coolingSetpoint - avg}
		logDebug("diff is ${diff}")

		logDebug("cyclingHysteresis is ${cyclingHysteresis}")
		logDebug("Hyst Diff is ${diff}")

		// TO DO - CALC HYST IN GAIN not LOSS
		def update = false
		if (update) {
			logDebug("Adjusting ${diff} degrees from cyclingHysteresis based on low end temp")
			def setHyst = device.currentValue("cyclingHysteresis").toBigDecimal()
			def newHyst = setHyst + diff
			logDebug("New Cycle Hysteresis is ${newHyst} degrees")

			// Adjust Hysteresis
			if (newHyst < 0.0) {newHyst = 0.0}
			if (newHyst > 0.5) {newHyst = 0.3}
			setCyclingHysteresis(newHyst)
		}

		state.calcLoss = false
		// case 1..5: //inclusive range
		// case 5..<9: //exclusive range, 9 is exluded
	}	

	// **** Calculate Slope when trend is falling or rising ****
	if (wrongTrend && state?.waiting == false && state?.cycling == false) {		
		if (calculateSlope) {
			logDebug("Calculating Slope")   // temperature, overSetpoint needed 
			calcSlope(temperature)			
		}
	} else {logDebug("Didn't meet requirements to calc Slope")}

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

// Update hysteresis settings based on target calcs
/* def updateHysteresis(ramp, wait, cycle) {

	setHysteresis(ramp)
	setTargetHysteresis(wait)
	setCyclingHysteresis(cycle)
} */

// set cycleState attribute for dashboard
def updateCycleState(ramping, cycling, waiting) {
	logDebug("updateCycleState called with Ramping: ${ramping}, Cycing: ${cycling}")
	logDebug("state.waiting is ${state?.waiting}")

	if (cycling && waiting) {cycling = false}
	if (ramping && waiting) {waiting = false}

	def cycleStateValue = device.currentValue("cycleState")
	def newState = " "
    if (ramping) {newState = "Ramping"}
    else if (cycling) {newState = "Cycling"}
    else if (waiting) {newState = "Waiting"}
	else newState = "Idle"	
	if (!cycleStateValue.equals(newState)) {
		logDebug("updateing cycleState to ${newState}")
		setCycleState(newState)
	}
}

// take current and previous and calc slope 
def calcSlope(temperature) {

	if (state?.firstSlope == false) {
		Date now = new Date()
		def x1 = state?.lastFallStamp
		def x2 = now.getTime()
		def y1 = state?.lastFallTemp
		def y2 = temperature
		logDebug("x1 is ${x1}")
		logDebug("y1 is ${y1}")
		logDebug("x2 is ${x2}")
		logDebug("y2 is ${y2}")

		def change = y2 - y1
		logDebug("change is ${change}")
		def absChange = Math.abs(change)  // since can be fall or rise based on heat/cool
		logDebug("Absolute Change is ${absChange}")

		// Calc if temperature changed by more than .04. 
		if (absChange >= 0.02) {

			logDebug("x1 is ${x1} and x2 is ${x2}")
			logDebug("y1 is ${y1} and y2 is ${y2}")

			// get slope within a reasonable range to work with
			def slope = ((y2 - y1) / (x2 - x1)) * 10000000  
			logDebug("Slope is ${slope}")
			//def lastSlope = state?.lastSlope

			state.lastFallStamp = x2
			state.lastFallTemp = y2

			// add to slope list
			def absNew = Math.abs(slope) 
			logDebug("Slope ABS is ${absNew}")
			
			// find mean before adding new slope
			def size = state?.slopeArray.size();
			double sum = 0.0;
			for(double a : state?.slopeArray)
				sum += a;
			def oldMean = sum/size;
			logDebug("Old mean is ${oldMean}")	

			def newDiff = oldMean - absNew    // check if new slope is within current average 	
			def absDiff = Math.abs(newDiff)
			logDebug("absolute diff is ${absDiff}")
		
			// update Slope only if new slope is within 2 of the absolute old avg. 
			if (absDiff <= 10.0) {
				// update array and keep at 5
				if (absNew < 0) {absNew = Math.abs(slope) }
				logDebug("Adding Slope ${absNew}")
				if (absNew > 0) {			
					state?.slopeArray.push(absNew)
					if (state?.slopeArray.size() > 5)
					state.slopeArray.removeAt(0)

					// find mean of new array
					size = state?.slopeArray.size();
					double sum2 = 0.0;
					for(double a2 : state?.slopeArray)
						sum2 += a2;
					def mean = sum2/size;
					logDebug("Slope mean is ${mean}")	

					def absSlope = Math.abs(mean)    // slope as positive value for cycle times

					// set slope values
					state.slope = absSlope
					setSlope(absSlope)
					state.lastSlope = absSlope

					// calculate new cycle seconds using pref coefficient
					def coefficient = 0.0				
					coefficient = settings?.cycleCoefficient.toBigDecimal()
					logDebug("cycle coefficient is ${coefficient}")					
					def secs = (absSlope * coefficient)+(absSlope)

					def cycle = Math.round(secs).toInteger()
					logDebug("New Cycle seconds is ${cycle}")

					if (cycle < 90) {cycle = 90}

					// set cycle seconds values
					state.newCycleSeconds = cycle
					setCycleSeconds(cycle)
					state.numSlope = state?.numSlope + 1  // count to 5 slopes before adjusting coefficient

				} else {logDebug("absolute slope is negative!!!")}
				logDebug("Slope array is ${state?.slopeArray}")
			} else {
				// update array with last slope value to push toward changing slope (if not zero)
				if (absDiff > 0) {
					logDebug("New slope not in range to calc slope - adding last slope to array")
					def lastSlope = state?.lastSlope
					state?.slopeArray.push(slope)
					if (state?.slopeArray.size() > 5)
					state.slopeArray.removeAt(0)	
				}			
			}

		} else {
			logDebug("change not in range to calculate slope")
			// if falling steady discard last data: for temp sensors that only fall once in a cycle
			if (absChange > 0 && state?.tempState == "falling steady") {			
				logDebug("Change while falling steady - setting new last fall temp")  
				state.lastFallTemp = temperature
				state.lastFallStamp = now.getTime()
			}
		}
	} 
	if (state?.firstSlope == true && overSetpoint) {
		logDebug("Skipping first slope calc after top of cycle and saving current loss change data")
		state.lastFallTemp = temperature
		state.lastFallStamp = now.getTime()		
		state.firstSlope = false				
	} else if (state?.firstSlope == true) {
		logDebug("Skipping first slope Calc")
		state.firstSlope = false
	}
}

def updateCycleSeconds() {
	def absSlope = state?.lastSlope

	// calculate new cycle seconds using pref coefficient
	def coefficient = 0.0				
	coefficient = settings?.cycleCoefficient.toBigDecimal()
	logDebug("cycle coefficient is ${coefficient}")					
	def secs = (absSlope * coefficient)+(absSlope)

	def cycle = Math.round(secs).toInteger()
	logDebug("New Cycle seconds is ${cycle}")

	if (device.currentValue("thermostatMode") == "heat") {
		if (cycle < 120) {cycle = 120}  // don't let it got below 2 min cycles when heating
	} else {if (cycle < 240) {
		cycle = 240} 					// don't let it got below 4 min cycles when cooling
	} 
	// set cycle seconds values
	state.newCycleSeconds = cycle
	setCycleSeconds(cycle)
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
	setCycleState(" ")
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

// adjust cycle seconds based on auto adjust diff
def adjustCycleSeconds(diff) {

	def cycleSeconds = state?.cycleSeconds
	logDebug("Cycle Seconds is ${cycleSeconds}")

	def setpointChangeSecs = diff.toInteger()

	def newSeconds = cycleSeconds + setpointChangeSecs
	logDebug("New Seconds is ${newSeconds}")

	def change = newSeconds - cycleSeconds

	// limit a change to 200 seconds
	if (change > 200) {
		newSeconds = cycleSeconds + 200
	} else if (change < -200 ) {
		newSeconds = cycleSeconds - 200
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
	def interval = state?.waitSeconds
	logDebug("set Interval is ${interval}")

    def newInterval = interval + diff.toInteger()

	logDebug("New Interval is ${newInterval}")

	if (newInterval < 300) {newInterval = 300}  // keep wait at at least 300 sec
	if (newInterval > 750) {newInterval = 750}  // keep below 750
  
	state.waitSeconds = newInterval
	setWaitSecondsAttrib(newInterval.toString())
	logDebug("Changed Interval is ${newInterval}")
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
			state.calcGain = true	
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
			state.calcLoss = true
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
	if (value > 80.0) {setFanSpeed("medium")}
	else if (value > 90.0) {setFanSpeed("high")}
	else {setFanSpeed("low")}
	runIn(1,manageCycle)
}

def setSlope(value) {
	logDebug "setSlope(${value}) was called"
	sendEvent(name: "slope", value: value, descriptionText: getDescriptionText("slope was set to ${value}"))
}

def setCyclingHysteresis(value) {
	logDebug "setCyclingHysteresis(${value}) was called"
	sendEvent(name: "cyclingHysteresis", value: value, descriptionText: getDescriptionText("cyclingHysteresis set to ${value}"))
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
}

def setTargetHysteresis(value) {
	logDebug "setTargetHysteresis(${value}) was called"
	sendEvent(name: "targetHysteresis", value: value, descriptionText: getDescriptionText("targetHysteresis set to ${value}"))
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
	updateSetpoints(setpoint, null, null, null)
}

def setAutoSetpoint(setpoint) {

	def coolOffset = setpoint.toInteger() + 1
	def heatOffset = setpoint.toINteger() - 1

	def mode = device.currentValue("thermostatMode")
	if (mode == "cool") {
		setCoolingSetpoint(setpoint)
		setHeatingSetpoint(heatOffset)
	}
	if (mode == "heat") {
		setHeatingSetpoint(setpoint)
		setCoolingSetpoint(coolOffset)
	}
}

def setCoolingSetpoint(setpoint) {
	logDebug "setCoolingSetpoint(${setpoint}) was called"
	updateSetpoints(null, null, setpoint)
	runIn(3, manageCycle)
}

def setHeatingSetpoint(setpoint) {
	logDebug "setHeatingSetpoint(${setpoint}) was called"	
	updateSetpoints(null, setpoint, null,)
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