/*
    Virtual Humidistat

    Copyright 2023 -> C. Burgess
    10/3/23 - New Version - volume is setpoint and level is water level
    10/6/23 - Water level presence description is set when setting level for water level
    10/8/23 - Water Level is calculated in the driver based on tank size and consumption
*/

metadata {
    definition (
            name: "Virtual Humidistat",
            namespace: "hubitat",
            author: "Chris B"
    ) {
        capability "Motion Sensor"    //"active", "inactive"
        capability "Light"
        capability "Switch"
        capability "Switch Level"     // water level
        capability "ChangeLevel"      
        capability "Actuator"
        capability "Contact Sensor"  //"open", "closed"
        capability "Presence Sensor"
        capability "AudioVolume"     // humidifiying setpoint
        capability "Lock"            //"locked", "unlocked"
        
        attribute "operatingState", "ENUM" // ["humidifying", "idle", "off"]
        attribute "hysteresis", "NUMBER"  // hysteresis
        attribute "humidity", "NUMBER"    // Room Humidity
        attribute "display", "STRING"     // Dashboard icon display status
        attribute "power", "ENUM"         // power metering outlet (set in sync app)
        attribute "presence", "ENUM"      // Water level description
        attribute "volume", "NUMBER"      // humidifiying setpoint
        attribute "mute", "ENUM"          // unused (comes with volume)
        attribute "motion", "ENUM"        // Sanitization alexa switch - active activates sanitization
        attribute "lock", "ENUM"          // switch for sanitize on/off when humidifier on 
        attribute "checkError", "ENUM"    // check power to see if humidifier is on/off when should/should not be
        attribute "humidifyingSetpoint", "ENUM"
        attribute "energyDuration", "ENUM"

        // Commands needed to change internal attributes of virtual device.
        command "setHumidifyingSetpoint", ["NUMBER"]
        command "setOperatingState", [[name:"operatingState",type:"ENUM", description:"Set Operating State", constraints:["humidifying","idle"]]]
        command "setHumidity", ["NUMBER"]
        command "setHysteresis", ["NUMBER"]
        command "manageCycle"
        command "setDisplay", ["STRING"]
        command "setPower", ["ENUM"]
        command "setPresence", [[name:"waterLevel",type:"ENUM", description:"Set Water Level", constraints:["full","half","empty","fill"]]]
        command "setVolume", ["NUMBER"]
        command "setMotion", [[name:"motion",type:"ENUM", description:"Set Sanitization", constraints:["active","inactive"]]]
        command "setCheckError", [[name:"motion",type:"ENUM", description:"Set Sanitization", constraints:["on","off"]]]
        command "setOuncesPerInch"
        command "setWaterFull"
        command "setLevelTimer"
        command "setEnergyDuration", ["ENUM"]
}

    preferences {
        input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
        input( name: "inchesPerHour", type:"DOUBLE", title: "Inches of water used per Hour", defaultValue: 0.73)
        input( name: "fullInches", type:"DOUBLE", title: "Tank depth in Inches", defaultValue: 16.25)
        input( name: "fullOunces", type:"DOUBLE", title: "Tank volume in Ounces", defaultValue: 235.0)
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
    if (state?.lastRunningMode == null) {    
        sendEvent(name: "Operating State", value: (operatingState ?: "idle"))
        sendEvent(name: "Humidity", value: (humidity ?: 0).toBigDecimal())
        sendEvent(name: "Humidifying Setpoint", value: (humidifyingSetpoint ?: 45).toBigDecimal())   
               
        state.lastRunningMode = "humidifying"
        updateDataValue("lastRunningMode", "humidifying")
        setOperatingState("idle")
        setHumidifyingSetpoint(45)
        setHumidity(50)
        state.runMinutes = 0.0
        state.percentFull = 0
        state.runMinutes = 0.0
        state.inchesUsed = 0.0
        state.ouncesRemaining = 0.0
    }   
}

def parse(String description) { noCommands("parse") }

private eventSend(name,verb,value,unit = ""){
    String descriptionText = "${device.displayName} ${name} ${verb} ${value}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    if (unit != "") sendEvent(name: name, value: value ,descriptionText: descriptionText, unit:unit)
    else  sendEvent(name: name, value: value ,descriptionText: descriptionText)   
}

def on() {
    String verb = (device.currentValue("switch") == "on") ? "is" : "was turned"
    eventSend("switch",verb,"on")
    sendEvent(name: "operatingState", value: "humidifying", descriptionText: getDescriptionText("operatingState set to humidifying")) 
    sendEvent(name: "contact", value: "open", isStateChange: forceUpdate)
    if (device.currentValue("lock") == "locked") {
        sendEvent(name: "motion", value: "active", isStateChange: forceUpdate)
    }    
}

def off() {
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    eventSend("switch",verb,"off")
    sendEvent(name: "operatingState", value: "idle", descriptionText: getDescriptionText("operatingState set to idle")) 
    sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
    if (device.currentValue("lock") == "locked") {
        sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
    }  
}

// set new level afer calcs
def setCalcLevel(value, rate = null) {
    if (value == null) return
    Integer level = limitIntegerRange(value,0,100)
    if (level == 0) {
        off()
        return
    }    
    String verb = (device.currentValue("level") == level) ? "is" : "was set to"
    eventSend("level",verb,level,"%")   
    runIn(1,setDisplay)
    runIn(1,setLevelPresence)    
}

// Set new level from slider (update state variables)
def setLevel(value, rate = null) {
    if (value == null) return
    Integer level = limitIntegerRange(value,0,100)
    if (level == 0) {
        off()
        return
    }    
    String verb = (device.currentValue("level") == level) ? "is" : "was set to"
    eventSend("level",verb,level,"%")       
    // reverse calcs from percent full to set other states
    state.percentFull = value
    double fullInches = settings?.fullInches
    state.inchesRemaining = (fullInches * state?.percentFull)/100
    logDebug("New Inches Remaining is ${state?.inchesRemaining}")
    state.inchesUsed = fullInches - state?.inchesRemaining
    logDebug("Inches Used is ${state?.inchesUsed}")
    //BigDecimal ouncesPerInch = new BigDecimal(settings?.ouncesPerInch)
    state.ouncesRemaining = state?.inchesRemaining * state?.ouncesPerInch 
    double inchesPerHour = settings?.inchesPerHour
    double runMinutes = state?.inchesUsed * (inchesPerHour * 60)
    logDebug("New runMinutes double is ${runMinutes}")
    state.runMinutes = Integer.valueOf(runMinutes.intValue())
    runIn(1,setDisplay)
    runIn(1,setLevelPresence)
}

/* set water level presence description */
def setLevelPresence() {
    logDebug "setLevelPresence() was called"
    def level = device.currentValue("level")
    if (level <= 1) setPresence("empty")
    if (level > 1 && level < 10) setPresence("fill")
    if (level >= 10 && level < 38) setPresence("one-quarter")
    if (level >= 38 && level < 68) setPresence("half")
    if (level >= 68 && level < 88) setPresence("three-quarter")
    if (level >= 88 && level <= 100) setPresence("full")
}

def setPresence(setpoint) {
    logDebug "sePresence(${setpoint}) was called"
    sendEvent(name: "presence", value: setpoint, descriptionText: getDescriptionText("presence set to ${setpoint}"))
    runIn(1,setDisplay)
}

// volume is humidifiying setpoint
def setVolume(setpoint) {
    logDebug "setVolume(${setpoint}) was called"
    sendEvent(name: "volume", value: setpoint, unit: "%", descriptionText: getDescriptionText("volume set to ${setpoint}"))
    sendEvent(name: "humidifyingSetpoint", value: setpoint, descriptionText: getDescriptionText("humidifyingSetpoint set to ${setpoint}"))
    //if (device.currentValue("switch") != "on") on()
    runIn(1, manageCycle)
    runIn(1, setDisplay)
    
}

// setpoint matches volume
def setHumidifyingSetpoint(setpoint) {
    logDebug "setHumidifyingSetpoint(${setpoint}) was called"
    sendEvent(name: "humidifyingSetpoint", value: setpoint, descriptionText: getDescriptionText("humidifyingSetpoint set to ${setpoint}"))
    sendEvent(name: "volume", value: setpoint, unit: "%", descriptionText: getDescriptionText("volume set to ${setpoint}"))
}

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}

def setDisplay() {
    logDebug "setDisplay() was called"
    String display = "Humidity: "+ device.currentValue("humidity")+"%<br> Setpoint: "+ device.currentValue("humidifyingSetpoint")+"%<br> Water: "+device.currentValue("level")+"%<br> "+device.currentValue("operatingState")
    sendEvent(name: "display", value: display, descriptionText: getDescriptionText("display set to ${display}"))
}

def setOperatingState(state) {
    logDebug "setOperatingState(${state}) was called"
    sendEvent(name: "operatingState", value: state, descriptionText: getDescriptionText("operatingState set to ${state}"))   
    runIn(1,setDisplay)
}

def setPower(power) {
    logDebug "setPower(${power}) was called"
    sendEvent(name: "power", value: power, descriptionText: getDescriptionText("power set to ${power}"))   
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def manageCycle(){

    def hysteresis = (device.currentValue("hysteresis"))

    def humidifyingSetpoint = (device.currentValue("humidifyingSetpoint")).toBigDecimal()
    def humidity = (device.currentValue("humidity")).toBigDecimal()
    def operatingState = (device.currentValue("operatingState"))
    
    def humidifyingOn = (humidity <= (humidifyingSetpoint - hysteresis))
    
    if ((humidifyingOn && operatingState != "humidifying")) {  
        setOperatingState("humidifying")
        on()
        if (device.currentValue("lock") == "locked") {
            sendEvent(name: "motion", value: "active", isStateChange: forceUpdate)
        }
        runIn(1,setLevelTimer)
    }
    else if ((!humidifyingOn && operatingState != "idle")){  
        setOperatingState("idle")
        off()
        if (device.currentValue("lock") == "locked") {
            sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
        }    
    }
    runIn(1,setDisplay)   
}

def setLevelTimer() {
    if (device.currentValue("operatingState") == "humidifying") {
        runIn(60,countMinutes)
         logDebug "Level timer started"
    }
}

def countMinutes() {
    setOuncesPerInch()
    if (device.currentValue("operatingState") == "humidifying") {
        state.runMinutes = state?.runMinutes + 1
        BigDecimal inchesPerHour = new BigDecimal(settings?.inchesPerHour)
        state.inchesUsed = (inchesPerHour * (state?.runMinutes/60))
        BigDecimal fullInches = new BigDecimal(settings?.fullInches)
        state.inchesRemaining = fullInches - state?.inchesUsed
        logDebug "inchesRemaining is ${state?.inchesRemaining}"
        logDebug "ouncesPerInch is ${state?.ouncesPerInch}"

        def ouncesRemaining = state?.ouncesPerInch * state?.inchesRemaining

        logDebug "ouncesRemaingin def is ${ouncesRemaining}"
        state?.ouncesRemaining = ouncesRemaining
        logDebug "inchesRemaining is ${state?.inchesRemaining}"
        logDebug "fullInches is ${fullInches}"
        logDebug "inchesUsed is ${state?.inchesUsed}"
        BigDecimal percentFull = 100*((fullInches - state?.inchesUsed) / fullInches)
        logDebug "percentFull BD is ${percentFull}"
        state.percentFull = Integer.valueOf(percentFull.intValue())
        logDebug "percentFull is ${state?.percentFull}"
        setCalcLevel(state?.percentFull)
    }
    setLevelTimer()
}

/*
def countDeviceMinutes() {
    setOuncesPerInch()
    if (device.currentValue("operatingState") == "humidifying") {
        humidifierPower.refresh()       // refesh energyDuration
        pauseExecution(500)             // time to refresh
        String duration = humidifierPower.currentValue("energyDuration")
        def durationMin = duration.replace(" Mins", "")
        BigDecimal minutes = new BigDecimal(durationMin)
        def runDuration = minutes
        state.runDuration = runDuration
        BigDecimal inchesPerHour = new BigDecimal(settings?.inchesPerHour)
        def inchesUsed = (inchesPerHour * (runDuration/60))
        BigDecimal fullInches = new BigDecimal(settings?.fullInches)
        def inchesRemaining = fullInches - inchesUsed
        logDebug("Minutes is ${minutes}")
        logDebug "inchesRemaining is ${state?.inchesRemaining}"
        logDebug "ouncesPerInch is ${state?.ouncesPerInch}"
    }
}*/

def setOuncesPerInch() {
    BigDecimal fullOunces = new BigDecimal(settings?.fullOunces) 
    BigDecimal fullInches = new BigDecimal(settings?.fullInches) 
    state?.ouncesPerInch = fullOunces / fullInches
    logDebug "ouncesPerInch is ${state?.ouncesPerInch}"
}

def setWaterFull() {
    setOuncesPerInch()
    state.runMinutes = 0
    state.inchesUsed = 0
    state.inchesRemaining = settings?.fullInches
    logDebug "fullOunces is ${settings?.fullOunces}"
    logDebug "fullInches is ${settings?.fullInches}"
    BigDecimal fullOunces = new BigDecimal(settings?.fullOunces) 
    BigDecimal fullInches = new BigDecimal(settings?.fullInches) 
    state.ouncesPerInch = fullOunces / fullInches
    BigDecimal ouncesPerInch = new BigDecimal(state?.ouncesPerInch)
    BigDecimal inchesRemaining = new BigDecimal(state?.inchesRemaining)
    state.ouncesRemaining = ouncesPerInch * inchesRemaining
    state.percentFull = 100.0
    setLevel(state?.percentFull)
}

/* Check power use to see if humidifier is not in sync with expected on-off status  */
def checkError() {
    logDebug "checkError() was called"
    def humidity = (device.currentValue("humidity"))
    def humidifyingSetpoint = (device.currentValue("humidifyingSetpoint"))
    def hysteresis = (device.currentValue("hysteresis")).toBigDecimal()    
    double power = Double.parseDouble(device.currentValue("power"))
    def humidifyingOn = (humidity <= (humidifyingSetpoint - hysteresis))
    if (!humidifyingOn && power > 10.0) {
        logDebug "ERROR: Humidifier on when it should be off - turning off"
        setOperatingState("idle")
        off()
        if (device.currentValue("lock") == "locked") {
            sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
        }  
        setOperatingState("idle")
    }   
    if (humidifyingOn && power < 10.00) {
        logDebug "ERROR: Humidifier off when it should be on - turning on"
        setOperatingState("humidifying")
        on()
        if (device.currentValue("lock") == "locked") {
            sendEvent(name: "motion", value: "active", isStateChange: forceUpdate)
        }  
    }       
    runIn(1,setDisplay)
    if (device.currentValue("checkError") == "on") {
        runIn(1,runCheckError)       // contine error checking every 10 min if checking is on
    }    
}

def runCheckError() { 
    runIn(600,checkError)
}

// Commands needed to change internal attributes of virtual device.
def setHumidity(humidity) {
    logDebug "setHumidity(${humidity}) was called"
    sendEvent(name: "humidity", value: humidity, unit: "%", descriptionText: getDescriptionText("humidity set to ${humidity}%"))
    runIn(1, manageCycle)
}

def setCheckError(state) {
    logDebug "setCheckError(${state}) was called"
    sendEvent(name: "checkError", value: state, descriptionText: getDescriptionText("checkError set to ${state}"))
    if (state == "on") {
        runIn(1,checkError)
    }
}

def setEnergyDuration(duration) {
    logDebug "setEnergyDuration(${duration}) was called"
    sendEvent(name: "energyDuration", value: duration, descriptionText: getDescriptionText("energyDuration set to ${duration}"))
}

def setHysteresis(setpoint) {
    logDebug "setHysteresis(${setpoint}) was called"
    sendEvent(name: "hysteresis", value: setpoint, descriptionText: getDescriptionText("hysteresis set to ${setpoint}"))
    runIn(1, manageCycle)
}

def mute() {
    logDebug "mute() was called"
    sendEvent(name: "mute", value: "muted", descriptionText: getDescriptionText("mute set to muted"))
}

def unmute() {
    logDebug "unmute() was called"
    sendEvent(name: "mute", value: "unmuted", descriptionText: getDescriptionText("mute set to unmuted"))
}

def setMotion(setpoint) {
    logDebug "setMotion(${setpoint}) was called"
    sendEvent(name: "motion", value: setpoint, descriptionText: getDescriptionText("motion set to ${setpoint}"))
}

def setMotionInactive() {
    logDebug "setMotionInactive() was called"
    sendEvent(name: "motion", value: "inactive", descriptionText: getDescriptionText("motion set to inactive"))
}

def lock(setpoint) {
    logDebug "lock() was called"
    sendEvent(name: "lock", value: "locked", descriptionText: getDescriptionText("lock set to locked"))
}

def unlock(setpoint) {
    logDebug "unlock() was called"
    sendEvent(name: "lock", value: "unlocked", descriptionText: getDescriptionText("lock set to unlocked"))
}

private logDebug(msg) {
    if (settings?.logEnable) log.debug "${msg}"
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

private getDescriptionText(msg) {
    def descriptionText = "${device.displayName} ${msg}"
    if (settings?.txtEnable) log.info "${descriptionText}"
    return descriptionText
}