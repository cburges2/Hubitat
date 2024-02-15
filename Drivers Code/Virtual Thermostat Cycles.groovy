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
    - Cycling:  Will cycle when temp is below setpoint +- cyclingHystresis.  Duration for cyclingSeconds is based on cycles per hour chosen in prefrences
                Cycling will terminate if temp goes over cycling point
                The cycleWait flag is set at end of cycle.  It is cleared after setCycleSeconds or if temp goes back above cycling point. 
    - Stopping:  Cycles are timed, and they stop when done or when the temp rises above the stop hysteresis temp.  
*/

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
        attribute "motion", "ENUM"      // motion is active with cool operatingState, inactive when idle/fan
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
	    command "setIdleBrightness", [[name:"idleBrightness",type:"ENUM", description:"Set Idle brightness", constraints:["0", "1", "2", "3", "4", "5"]]]
		command "setCycling",[[name:"cycling",type:"ENUM", description:"Cycling Heat", constraints:["true","false"]]]
		command "setCyclingHysteresis", ["NUMBER"]
        command "setCycleState",[[name:"cycleState",type:"ENUM", description:"Cycling State", constraints:["Cycling","Ramping","Waiting"," "]]]
		command "setOutsideTemp", ["NUMBER"]
	}

	preferences {
        input( name: "iconPath", type: "string", description: "Address Path to icons", title: "Set Icon Path", defaultValue: "https://cburges2.github.io/ecowitt.github.io/Dashboard%20Icons/")
		input( name: "useACState", type:"bool", title: "Enable using AC State and Icon",defaultValue: false)
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
		input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
		input( name: "cyclesPerHour",type:"enum",title: "Rough Cycles Per Hour", options:["2","3","4","5"], description:"", defaultValue: 3)
		input( name: "adjustCycleMinutes",type:"enum",title: "Adjust Cycle Minutes", options:["-10","-9","-8","-7","-6","-5","-4","-3","-2","-1","0","1","2","3","4","5","6","7","8","9","10"], description:"Lower if temp goes too high when cycling", defaultValue: 0)
		input( name: "adjustIntervalMinutes",type:"enum",title: "Adjust Interval Minutes", options:["-10","-9","-8","-7","-6","-5","-4","-3","-2","-1","0","1","2","3","4","5","6","7","8","9","10"], description:"Lower if temp goes too low between cycles", defaultValue: 0)
        input( name: "secondsPerDegree",type:"enum",title: "Adjust Cycle Seconds per Outside Temp Degree", options:["0","1","2","3","4","5","8","10","12","15","20","25","30","35","40","45"], description:"Raise if cycling too much in colder temps", defaultValue: 20)
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

    //unschedule()    
    state.cycleWait = false
	//state.ramping = false

	state.lastTemp = device.currentValue("temperature")
    runIn(1,initCycling)

	initialize()
}

def initialize() {
	if (state?.lastRunningMode == null) {
		sendEvent(name: "temperature", value: convertTemperatureIfNeeded(68.0,"F",1))
		sendEvent(name: "thermostatSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
		sendEvent(name: "heatingSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
		sendEvent(name: "coolingSetpoint", value: convertTemperatureIfNeeded(75.0,"F",1))
		state.lastRunningMode = "heat"
		updateDataValue("lastRunningMode", "idle")
		setThermostatOperatingState("idle")
		setSupportedThermostatFanModes(["auto","circulate","on"])
		setSupportedThermostatModes(["auto", "cool",  "heat", "off"])
		thermoOff()
		fanAuto()
        state.ramping = "idle"
		state.cycleWait = false
		setCyclingHysteresis(".08")
		setStopHysteresis(".1")
		setCycling("false")
		setCycleState(" ")
        sendEvent(name: "hysteresis", value: (hysteresis ?: 0.5).toBigDecimal())       
	}
}

def initCycling() {
	logDebug("Calculating Cycle States")

    // calc cycle interval minutes
	Double cycles = Double.valueOf(cyclesPerHour)       
	def cycleIntervalMins = Math.round(15.0 / cycles)   
    def adjustIntervalMins = Double.valueOf(adjustIntervalMinutes)
    
    state.cycleInterval = (cycleIntervalMins + Math.ceil(adjustIntervalMins)).toInteger()
    state.cycleIntervalSeconds = state?.cycleInterval * 60
    
    state.intervalSeconds = state?.cycleInterval * 60

	def adjustCycleMins = Double.valueOf(adjustCycleMinutes) 
    // set cycle mins (based on 15 / cycles) at 32 degrees outside
	def cycleMins = 0
	if (cycles == 2.0) {cycleMins = 8}
	else if (cycles == 3.0) {cycleMins = 5}
	else if (cycles == 4.0) {cycleMins = 4}
	else if (cycles == 5.0) {cycleMins = 3}

	state.cycleMinutes = (cycleMins + Math.ceil(adjustCycleMins)).toInteger()
	state.cycleSeconds = (cycleMins * 60) + (Math.ceil(adjustCycleMins) * 60).toInteger()

	state.setSeconds = state?.cycleSeconds
    
	adjustCycleSeconds()    // adjust cycle and interval by outside temp
}

def manageCycle(){
	def ambientTempChangePerCycle = 0.25
	def hysteresis = (device.currentValue("hysteresis")).toBigDecimal()

	def coolingSetpoint = (device.currentValue("coolingSetpoint") ?: convertTemperatureIfNeeded(75.0,"F",1)).toBigDecimal()
	def heatingSetpoint = (device.currentValue("heatingSetpoint") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()
	def temperature = (device.currentValue("temperature") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()

	def thermostatMode = device.currentValue("thermostatMode") ?: "off"
	def thermostatOperatingState = device.currentValue("thermostatOperatingState") ?: "idle"

    def stopHysteresis = (device.currentValue("stopHysteresis").toBigDecimal())	
    def cyclingHysteresis = (device.currentValue("cyclingHysteresis").toBigDecimal())

    def ramping = false
    def cyclingOn = false

    def coolOn = coolingSetpoint + hysteresis
    def heatOn = heatingSetpoint - hysteresis

    def coolingOn = (temperature > coolOn) 
    def heatingOn = (temperature < heatOn)	

    def heatOff = heatingSetpoint + stopHysteresis
    def coolOff = coolingSetpoint - hysteresis 
    def cycleOn


    // ** Set Cycling State **
    if (thermostatMode == "cool") {
        // cycle settings       
        cycleOn = coolingSetpoint - cyclingHysteresis  // cycle setpoint
        cyclingOn = temperature >= cycleOn
        ramping = temperature > coolOn  // Check if rampting to temp
		state.ramping = ramping
    }
    if (thermostatMode == "heat") {
        // cycle settings
        cycleOn = heatingSetpoint + cyclingHysteresis  // cycle setpoint
        logDebug("cycleOn is ${heatingSetpoint + cyclingHysteresis}")
        logDebug("temp is ${temperature}")
        cyclingOn = temperature <= cycleOn     
        ramping = temperature < heatOn  // Check if rampting to temp   
		state.ramping = ramping    
    }

    if (ramping) {
        setCycleWait()
    }

    // cancel cycleWait when above setpoint
    if (state?.cycleWait == true && temperature >= cycleOn) {resetCycleWait()}
    
    // cycle if at cycle temp and at target
    if (cyclingOn && !ramping && state.cycleWait == false) {
    	logDebug("OK to Cycle")	
		if (device.currentValue("cycling") == "false") {
			logDebug("Statring cycling with temp change")
			setCycling("true")
			runIn(state?.cycleSeconds, stopCycle)
		}
    }

    def cycling = device.currentValue("cycling") == "true"

 	// stop if cycling and at stop hysteresis with temp rising
	logDebug("Temp State is ${state?.tempState}")
	if (cycling && temperature >= heatOff && state?.tempState == "rising") {
		logDebug("Stopping cycle at stop hysteresis while temp rising")
		setCycling("false")
		cyclingOn = false    
        unschedule("stopCycle")
        setCycleWait()   		
		cycling = false
	}

    logDebug("cycling is ${cycling}")   
    // if cycling turns off while cycling, stop the cycle (reaches setpoint +- cycleHysteresis)
    
    if (!cyclingOn && cycling) {
        logDebug("Stopping cycle at setpoint")
        unschedule("stopCycle")
        setCycling("false")
        setCycleWait()    
        if (temperature >= (heatingSetpoint - hysteresis)) setThermostatOperatingState("idle")   // idle if not at ramp point
    }

    // set cycleState attribute for dashboard
    if (ramping) {setCycleState("Ramping")}
    else if (cycling) {setCycleState("Cycling")}
    else if (state?.cycleWait == true) {setCycleState("Waiting")}
    else setCycleState(" ")
    
    // Check cycle/ramping
    if (cycling && !ramping) { 
        if (thermostatMode == "heat") {
            if (cyclingOn) {heatingOn = true}
        } else if (thermostatMode == "cool") {
            if (cyclingOn) {coolingOn = true}
        }
    } else if (heatingOn) {
        if (thermostatOperatingState == "heating" && ramping) heatingOn = (temperature <= heatingSetpoint - hysteresis)
		if (thermostatOperatingState == "heating" && temperature >= heatOff) heatingOn = false
    } else if (coolingOn) {
        if (thermostatOperatingState == "cooling" && ramping) coolingOn = (temperature >= coolingSetpoint + hysteresis)
		if (thermostatOperatingState == "cooling" && temperature <= coolOff) coolingOn = false
    }
	
    // ** Set Operating State **
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

def stopCycle() {
    logDebug("Ending Cycle")
    if (device.currentValue("cycling") == "true") {
        setCycling("false")
		def temperature = (device.currentValue("temperature") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()
		def hysteresis = (device.currentValue("hysteresis")).toBigDecimal()
		def heatingSetpoint = (device.currentValue("heatingSetpoint") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()		
        setCycleWait()
    }	
    runIn(3,manageCycle)
}

def setCycleWait() {
	logDebug("setting Cycle Wait")
	state.cycleWait = true
	if (state?.ramping == false) {		
		unschedule("recheckCycleWait")
		runIn(state?.intervalSeconds, resetCycleWait)  	// intervalSeconds is out-temp adjusted cycleIntervalSeconds
	} else runIn(60,recheckCycleWait) 					// check if ramping false again in 1 min to ensure we resetCycleWait
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

// use outside temp to adjust cycle seconds by secsPerDegree set in prefrences
def adjustCycleSeconds() {

	def outTemp = device.currentValue("outsideTemp").toBigDecimal()
	def degreesBelowFreezing = 32.0 - outTemp
	logDebug("Degrees below freezing is ${degreesBelowFreezing}")

	def setSeconds = state?.setSeconds
	def secsPerDegree = secondsPerDegree.toInteger()
	logDebug("Secs per degree is ${secsPerDegree}")

	logDebug("Set Seconds is ${setSeconds}")

	def changeSeconds = Math.round(degreesBelowFreezing * secsPerDegree)
	logDebug("Change Seconds is ${changeSeconds}")

	def newSeconds = setSeconds + changeSeconds // longer cycles when colder
	logDebug("New Seconds is ${newSeconds}")

	state.cycleSeconds = newSeconds

    // set new interval seconds too for cycleWait
	logDebug("set Interval is ${state?.cycleIntervalSeconds}")

    def newInterval = state?.cycleIntervalSeconds - changeSeconds  // shorter intervals when colder
	logDebug("New Interval is ${newInterval}")
	if (newInterval < 60) newInterval = 60
    state.intervalSeconds = newInterval
	logDebug("new Interval is ${state.intervalSeconds}")
}

// not implemented - check if AC is in state it should be
def checkError() {
    def checkErrors = device.currentValue("errorCheck") 
    def status = device.currrentValue("acStatus")
    def thermostatOperatingState = device.currentValue("thermostatOperatingState") ?: "idle"
}

// Commands needed to change internal attributes of virtual device.
def setTemperature(temperature) {
	logDebug "setTemperature(${temperature}) was called"
	sendTemperatureEvent("temperature", temperature)
	runIn(1, manageCycle)

	def temp = temperature.toBigDecimal()
	if (state?.lastTemp > temp) {
		state.tempState = "falling"
	} else state.tempState = "rising"
	state?.lastTemp = temperature.toBigDecimal()
	
}

def setOutsideTemp(value) {
	logDebug "setOutsideTemp(${value}) was called"
	sendEvent(name: "outsideTemp", value: value, descriptionText: getDescriptionText("outsideTemp set to ${value}"))
	adjustCycleSeconds()
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
	setThermostatOperatingState ("idle")
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
	runIn(1, manageCycle)
}

def setHeatingSetpoint(setpoint) {
	logDebug "setHeatingSetpoint(${setpoint}) was called"
	updateSetpoints(null, setpoint, null, null)
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