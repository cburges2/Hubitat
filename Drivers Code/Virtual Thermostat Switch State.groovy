/*
	Virtual Thermostat Switch State 
    Version 1.0  2/10/23

    This Driver is for a virtual thermostat with added features:
    - Stop Hysteresis added to heat cycle, to set a smaller hysteresis for heat off (for hot water heating systems - does not apply to cool cycles)
    - Contact Sensor capability opens and closes contact along with the switch cablibility (for running alexa connected routines to turn on/off AC fan or Aux Heat)
	- Switch capability to turn on/off the contact sensor (AC fan or aux heat) as an on/off switch tile
    - Motion capability changes motion with cool thermostatOperatingState.  Is active when cooling, inactive when off/fan only (for running alexa connected air conditioner routines ac state)
    - Status Attributes can be synced to check for trigger errors.  
    - setRunInMin() added to turn on the Switch after a delay in Minutes

*/

metadata {
	definition (
			name: "Virtual Thermostat Switch State",
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
        command "setOnInMin", ["NUMBER"]
	}

	preferences {
		input( name: "hysteresis",type:"enum",title: "Thermostat hysteresis degrees", options:["0.1","0.25","0.5","1","2"], description:"", defaultValue: 0.5)
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
		sendEvent(name: "temperature", value: convertTemperatureIfNeeded(68.0,"F",1))
		sendEvent(name: "thermostatSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
		sendEvent(name: "heatingSetpoint", value: convertTemperatureIfNeeded(68.0,"F",1))
		sendEvent(name: "coolingSetpoint", value: convertTemperatureIfNeeded(75.0,"F",1))
		state.lastRunningMode = "heat"
		updateDataValue("lastRunningMode", "heat")
		setThermostatOperatingState("idle")
		setSupportedThermostatFanModes(["auto","circulate","on"])
		setSupportedThermostatModes(["auto", "cool", "emergency heat", "heat", "off"])
		thermoOff()
		fanAuto()
	}
	sendEvent(name: "hysteresis", value: (hysteresis ?: 0.5).toBigDecimal())
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
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

def manageCycle(){
	def ambientTempChangePerCycle = 0.25
	def hvacTempChangePerCycle = 0.75

	def hysteresis = (device.currentValue("hysteresis")).toBigDecimal()

	def coolingSetpoint = (device.currentValue("coolingSetpoint") ?: convertTemperatureIfNeeded(75.0,"F",1)).toBigDecimal()
	def heatingSetpoint = (device.currentValue("heatingSetpoint") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()
	def temperature = (device.currentValue("temperature") ?: convertTemperatureIfNeeded(68.0,"F",1)).toBigDecimal()

	def thermostatMode = device.currentValue("thermostatMode") ?: "off"
	def thermostatOperatingState = device.currentValue("thermostatOperatingState") ?: "idle"

	def ambientGain = (temperature + ambientTempChangePerCycle).setScale(2)
	def ambientLoss = (temperature - ambientTempChangePerCycle).setScale(2)
	def coolLoss = (temperature - hvacTempChangePerCycle).setScale(2)
	def heatGain = (temperature + hvacTempChangePerCycle).setScale(2)

    logDebug "hysteresis is ${hysteresis}"
	def coolOn = coolingSetpoint + hysteresis
	def coolOff = coolingSetpoint - hysteresis
	logDebug "coolOn is ${coolOn}"
	logDebug "coolOff is ${coolOff}"

	def coolingOn = (temperature >= coolOn)
	if (thermostatOperatingState == "cooling") coolingOn = temperature >= coolOff

    def stopHysteresis = (device.currentValue("stopHysteresis").toBigDecimal())
	def heatOn = heatingSetpoint - hysteresis
	def heatOff = heatingSetpoint + stopHysteresis
	
	def heatingOn = (temperature <= heatOn)
	if (thermostatOperatingState == "heating") heatingOn = (temperature <= heatOff)
	
	if (thermostatMode == "cool") {
        if (coolingOn && thermostatOperatingState != "cooling") {
            setThermostatOperatingState("cooling")
            sendEvent(name: "motion", value: "active", isStateChange: forceUpdate) 
        }    
        else if (!coolingOn && thermostatOperatingState != "idle") {
            setThermostatOperatingState("idle")
            sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
        }
	} else if (thermostatMode == "heat") {
		if (heatingOn && thermostatOperatingState != "heating") setThermostatOperatingState("heating")
		else if (!heatingOn && thermostatOperatingState != "idle") setThermostatOperatingState("idle")
	} else if (thermostatMode == "auto") {
		if (heatingOn && coolingOn) log.error "cooling and heating are on- temp:${temperature}"
		else if (coolingOn && thermostatOperatingState != "cooling") setThermostatOperatingState("cooling")
		else if (heatingOn && thermostatOperatingState != "heating") setThermostatOperatingState("heating")
		else if ((!coolingOn || !heatingOn) && thermostatOperatingState != "idle") setThermostatOperatingState("idle")
	}
}

def checkError() {
    def checkErrors = device.currentValue("errorCheck") 
    def status = device.currrentValue("acStatus")
    def thermostatOperatingState = device.currentValue("thermostatOperatingState") ?: "idle"
    //if (checkErrors == "true") {
    //    if (status == "off") { 
    //        // add code
    //        logDebug "checkError() was called";
    //    {
    //}
}

// Commands needed to change internal attributes of virtual device.
def setTemperature(temperature) {
	logDebug "setTemperature(${temperature}) was called"
	sendTemperatureEvent("temperature", temperature)
	runIn(1, manageCycle)
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

def setSupportedThermostatModes(modes) {
	logDebug "setSupportedThermostatModes(${modes}) was called"
	// (auto, cool, emergency heat, heat, off)
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

def setContact(value) {
	logDebug "setContact(${value}) was called"
	sendEvent(name: "contact", value: value, descriptionText: getDescriptionText("contact set to ${value}"))
}

def setMotion(value) {
	logDebug "setMotion(${value}) was called"
	sendEvent(name: "motion", value: value, descriptionText: getDescriptionText("motion set to ${value}"))
}

def setPresence(value) {
	logDebug "setPresence(${value}) was called"
	sendEvent(name: "presence", value: value, descriptionText: getDescriptionText("presence set to ${value}"))
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
}

def setHeatingSetpoint(setpoint) {
	logDebug "setHeatingSetpoint(${setpoint}) was called"
	updateSetpoints(null, setpoint, null, null)
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


private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}