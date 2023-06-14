/*
    Irregation Controller with Fertilize

    Copyright 2023 -> C. Burgess

    Sync Moisture using the sync app

    Set Start Moisture to moisture to trigger watering at
    Set Minutes at Trigger for how long moisture must stay at target to start watering. 
    Set Stop Moisture to moisture target, will water at that target or above for Minutes at target. 
    Set Minutes at Target for how long to water once target moisture is reached
    Set Max Minutes for how long to water overall in case target is not reached

    Switch turns on and contact opens when watering, to trigger an alexa routine to turn on the pump/valve or for the app to control the pump
    Switch turns off and contact closes when watering is done, to trigger an Alexa routine to turn off the pump/valve, or for the app to control the pump
    
    Max Minutes is set after each watering to the moisture reached, then used as Stop Moisture setting for next run. 

    Fertilize switch is controlled with lock attribute, to trigger another device to fertilize every other watering for fertilizeMins minutes
*/

metadata {
    definition (
            name: "Irrigation Controller with Fertilize",
            namespace: "hubitat",
            author: "Chris B"
    ) {
        capability "Switch"
        capability "Actuator"
        capability "Contact Sensor"  //"open", "closed"
        capability "Lock"   //"locked", "unlocked"

        // Internal Attributes for virtual device
        attribute "moisture", "ENUM"          // Soil Moisture  

        attribute "startMoisture", "ENUM"
        attribute "stopMoisture", "ENUM"      // set to Max from last run
      
        attribute "minutesAtTarget", "ENUM"   // minutesAtTarget
        attribute "minutesAtTrigger", "ENUM"  // Minutes at Start Moisture
        attribute "maxMoisture", "ENUM"       // moisture at water stop
        attribute "maxMinutes", "ENUM"        // max minutes to run, if target not reached. 

        attribute "operatingState", "ENUM"    // idle, waiting, watering, target
        attribute "display", "STRING"         // Dashboard icon to display status as attribute tile
        attribute "display2", "STRING"

        attribute "contact", "ENUM"           // Virtual contact sensor to run a pump or valve using Alexa routine (or other trigger method)
        attribute "lock", "ENUM"            // Virtual lock to run a 2nd pump or valve to fertilize using Alexa routine (or other trigger method)

        attribute "fertilize", "ENUM"         // true/false to fertilize with watering.
        attribute "fertilizeMins", "ENUM"

        // Commands needed to change internal attributes of virtual device.
        command "setMoisture", ["ENUM"]

        command "setStartMoisture", ["ENUM"]
        command "setStopMoisture", ["ENUM"]

        command "setMinutesAtTarget", ["ENUM"]
        command "setMinutesAtTrigger", ["ENUM"]
        command "setMaxMoisture", ["ENUM"]
        command "setMaxMinutes", ["ENUM"]

        command "setOperatingState", [[name:"operatingState",type:"ENUM", description:"Set Operating State", constraints:["watering","idle","waiting","target"]]]
        command "setDisplay", ["STRING"]

        command "setFertilize", [[name:"fertilize",type:"ENUM", description:"Set Fertilize", constraints:["true","false","off"]]]
        command "setFertilizeMins", ["ENUM"]
        command "setUnlocked"
        command "setLocked"
       
        command "manageCycle"
        command "bumpCycle"
} 
        
    preferences {
        input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
    }
}


def installed() {
    log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true])
    setStartMoisture(40)
    setStopMoisture(50)    
    initialize()        
    setMoisture(55)
    setMaxMoisture(50)
    setMaxMinutes(5)
    setMinutesAtTrigger(5)
    setMinutesAtTarget(5)
    setFertilize("off")
    setFertilizeMins(0)
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
               
        state.lastRunningMode = "idle"
        updateDataValue("lastRunningMode", "idle")
        setOperatingState("idle")
        sendEvent(name: "contact", value: "open", isStateChange: forceUpdate)
        off()
        setDisplay()
        setUnlocked()
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
    sendEvent(name: "contact", value: "open", isStateChange: forceUpdate)
}

def off() {
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    eventSend("switch",verb,"off")
    sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
    sendEvent(name: "lock", value: "unlocked", isStateChange: forceUpdate) // turn off fertilze pump or valve
}

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}

def setDisplay() {
    logDebug "setDisplay() was called"
    String display = "Moisture: "+ device.currentValue("moisture")+"%<br> Watering at: "+ device.currentValue("startMoisture")+"%<br> Stopping at: "+device.currentValue("stopMoisture")+"%<br> State: "+device.currentValue("operatingState")
    sendEvent(name: "display", value: display, descriptionText: getDescriptionText("display set to ${display}"))
    String display2 = "Next Fertilize: "+ device.currentValue("fertilize")+"<br> Mins at Trigger: "+ device.currentValue("minutesAtTrigger")+"<br> Max Minutes: "+device.currentValue("maxMinutes")+"<br> Mins at Target: "+device.currentValue("minutesAtTarget")
    sendEvent(name: "display2", value: display2, descriptionText: getDescriptionText("display2 set to ${display2}"))
}

def setOperatingState (state) {
    logDebug "setOperatingState(${state}) was called"
    sendEvent(name: "operatingState", value: state, descriptionText: getDescriptionText("operatingState set to ${state}"))
    runIn(1,setDisplay)
}

def setMaxMoisture(max) {
    logDebug "setMaxMoisture(${max}) was called"
    sendEvent(name: "maxMoisture", value: max, unit: "%",, descriptionText: getDescriptionText("maxMoisture set to ${max}%"))  
    runIn(1,setDisplay)
}

def setMaxMinutes(max) {
    logDebug "setMaxMinutes(${max}) was called"
    sendEvent(name: "maxMinutes", value: max, unit: "%",, descriptionText: getDescriptionText("maxMinutes set to ${max}"))   
    runIn(1,setDisplay)
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def waterStop() {
    setMaxMoisture(device.currentValue("moisture"))
    setStopMoisture(device.currentValue("moisture"))
    fert = device.currentValue("fertilize")
    if (fert == "true") {
        setUnlocked()
        setFertilize("false")   // next water will not fertilize
    } else if (fert == "false") {
        setFertilize("true")  // next water will fertilize
    } 
    off()
    sendEvent(name: "lock", value: "unlocked", isStateChange: forceUpdate) // turn off fertilze pump or valve
    setOperatingState("idle")
    runIn(1, setDisplay)
}

def waterStart() {   
    def moisture = (device.currentValue("moisture")).toInteger()
    def startMoisture = (device.currentValue("startMoisture")).toInteger()
    def fertilize = (device.currentValue("fertilize"))
    def fertSecs = 0
    if (fertilize == "on") fertSecs = ((device.currentValue("fertilizeMins")).toInteger())*60
    def waterOn = (moisture <= startMoisture)   
    if (waterOn) {
        on()
        setOperatingState("watering") 
        fert = device.currentValue("fertilize")
        if (fert == "true") {
            setLocked()
            runIn(fertSecs, fertilizeStop)
        }
        def maxMins = ((device.currentValue("maxMinutes")).toInteger() * 60) 
        runIn(maxMins, maxTimeStop)  // stop if target not reached in maxMinutes
    } else {
        setOperatingState("idle")  // moisture no longer at or below start moisture after minAtTrigger
    }
     runIn(1, setDisplay) 
}

def maxTimeStop() {
    runIn(1,waterStop)
}

def fertilizeStop() {
    setUnlocked()
}

def manageCycle(){

    def operatingState = (device.currentValue("operatingState"))
    def startMoisture = (device.currentValue("startMoisture")).toInteger()
    def stopMoisture = (device.currentValue("stopMoisture")).toInteger()
    def moisture = (device.currentValue("moisture")).toInteger()

    def waterOn = (moisture <= startMoisture)
    def waterOff = (moisture >= stopMoisture)
    
    if ((waterOn && operatingState == "idle")) {  
        setOperatingState("waiting")
        def minutesAtTrigger = ((device.currentValue("minutesAtTrigger")).toInteger() * 60)
        runIn(minutesAtTrigger, waterStart)
    }
    if (waterOff && operatingState == "watering"){  
        setOperatingState("target")
        def minutesAtTarget = ((device.currentValue("minutesAtTarget")).toInteger() * 60)
        runIn(minutesAtTarget, waterStop)
    }
    runIn(1,setDisplay)
}

def bumpCycle() {
    state.startMoisture = (device.currentValue("startMoisture")).toInteger()
    if (moisture < (state.startMoisture + 5)) {
        def moisture = (device.currentValue("moisture")).toInteger()
        def startMoisture = moisture + 1
        def minutesAtTarget = (device.currentValue("minutesAtTarget")).toInteger()+5
        def maxMins = (device.currentValue("maxMinutes")).toInteger()+5 
        def maxStop = maxMins * 60
        
        setMinutesAtTarget(minutesAtTarget)
        setMaxMinutes(maxMins)
        setStartMoisture(startMoisture)
        runIn(maxStop, resetCycle)
    }
}

def resetCycle() {
    def startMoisture = state.startMoisture
    def minutesAtTarget = (device.currentValue("minutesAtTarget")).toInteger()-5
    def maxMins = (device.currentValue("maxMinutes")).toInteger()-5
    
    setMinutesAtTarget(minutesAtTarget)
    setMaxMinutes(maxMins)    
    setStartMoisture(startMoisture)
}

// Commands needed to change internal attributes of virtual device.
def setMoisture(moisture) {
    logDebug "setMoisture(${moisture}) was called"
    sendEvent(name: "moisture", value: moisture, unit: "%", descriptionText: getDescriptionText("moisture set to ${moisture}%"))
    max = (device.currentValue("maxMoisture")).toInteger()
    if (max < moisture.toInteger()) {
        setMaxMoisture(moisture)
        setStopMoisture(moisture)  // keep stop moisture at max moisture reached
    }    
    runIn(1, manageCycle)
}

def setStartMoisture(setpoint) {
    logDebug "setStartMoisture(${setpoint}) was called"
    sendEvent(name: "startMoisture", value: setpoint, unit: "%", descriptionText: getDescriptionText("startMoisture set to ${setpoint}%"))
    runIn(1, manageCycle)    
}

def setMinutesAtTarget(setpoint) {
    logDebug "setMinutesAtTarget(${setpoint}) was called"
    sendEvent(name: "minutesAtTarget", value: setpoint, descriptionText: getDescriptionText("minutesAtTarget set to ${setpoint}"))
    runIn(1,setDisplay)
}

def setStopMoisture(setpoint) {
    logDebug "setStopMoisture(${setpoint}) was called"
    sendEvent(name: "stopMoisture", value: setpoint, unit: "%", descriptionText: getDescriptionText("startMoisture set to ${startMoisture}%"))
    runIn(1, manageCycle)    
}

def setMinutesAtTrigger(setpoint) {
    logDebug "seMinutesAtTrigger(${setpoint}) was called"
    sendEvent(name: "minutesAtTrigger", value: setpoint, descriptionText: getDescriptionText("minutesAtTrigger set to ${setpoint}"))
    runIn(1,setDisplay)
}

def setFertilize(value) {
    logDebug "seFertilize(${value}) was called"
    sendEvent(name: "fertilize", value: value, descriptionText: getDescriptionText("fertilize set to ${value}"))
    runIn(1,setDisplay)
}

def setFertilizeMins(value) {
    logDebug "seFertilizeMins(${value}) was called"
    sendEvent(name: "fertilizeMins", value: value, descriptionText: getDescriptionText("fertilizeMins set to ${value}"))
    runIn(1,setDisplay)
}


def setUnlocked() {
    logDebug "seUnlocked() was called"
    sendEvent(name: "lock", value: "unlocked", isStateChange: forceUpdate) // turn off fertilze pump or valve
}

def setLocked() {
    logDebug "seLocked() was called"
    sendEvent(name: "lock", value: "locked", isStateChange: forceUpdate) // turn off fertilze pump or valve
}

def lock() {
    logDebug "seLocked() was called"
    sendEvent(name: "lock", value: "locked", isStateChange: forceUpdate) // turn off fertilze pump or valve
}

def unlock() {
    logDebug "seUnlocked() was called"
    sendEvent(name: "lock", value: "unlocked", isStateChange: forceUpdate) // turn off fertilze pump or valve
}

private logDebug(msg) {
    if (settings?.logEnable) log.debug "${msg}"
}

private getDescriptionText(msg) {
    def descriptionText = "${device.displayName} ${msg}"
    if (settings?.txtEnable) log.info "${descriptionText}"
    return descriptionText
}