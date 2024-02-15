/*
    Timer Irrigation Controller

    Copyright 2023 -> C. Burgess
    v 1.0 - 2/16/23

    This driver acts as an irrigation Controller, watering for a set time at a set interval. 
    When watering, the switch is turned on and the contact sensor is set to open, to trigger an Alexa Routine to run a pump/valve
    When watering is done, the switch is turned off and the contact sensor is set to closed. 

    The cycle will start to run after timer is started, and continue running until the timer is stopped. 

*/

metadata {
    definition (
            name: "Timer Irrigation Controller",
            namespace: "hubitat",
            author: "Chris B"
    ) {
        capability "Switch"
        capability "Actuator"
        capability "Contact Sensor"  //"open", "closed"

        // Internal Attributes for virtual device 
        attribute "runSeconds", "ENUM"        // max minutes to run between intervals
        attribute "runIntervalHours", "ENUM"  // interval to run water at
        attribute "timerStatus", "ENUM"

        attribute "operatingState", "ENUM"    // idle, waiting, watering, target
        attribute "display", "STRING"         // Dashboard icon to display status as attribute tile
        attribute "display2", "STRING"        // Dashboard icon to display additional status as attribute tile

        attribute "contact", "ENUM"           // Virtual contact sensor to run a pump or valve using Alexa routine (or other trigger method)
        
        attribute "lastWater", "STRING"
        attribute "nextWater", "STRING"


        // Commands needed to change internal attributes of virtual device.
        command "setRunSeconds", ["ENUM"]
        command "setRunIntervalHours", ["ENUM"]

        command "startTimer"
        command "stopTimer"
} 
        
    preferences {
        input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
        input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)
    }
}

def installed() {
    log.warn "installed..." 
    device.updateSetting("txtEnable",[type:"bool",value:true]) 
    initialize()        
    setRunSeconds(10)
    setRunIntervalHours(4)
    setOperatingState("idle")
    sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
    sendEvent(name: "lastWater", value: "none", descriptionText: getDescriptionText("lastWater set to none"))
    sendEvent(name: "nextWater", value: "none", descriptionText: getDescriptionText("nextWater set to none"))
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
               
        state.lastRunningMode = "idle"
        updateDataValue("lastRunningMode", "idle")
        setOperatingState("idle")
        sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
        off()
        setDisplay()
    }   
    if (device.currentValue("timerStatus") == "Running") {
        startTimer()   
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

}

Integer limitIntegerRange(value,min,max) {
    Integer limit = value.toInteger()
    return (limit < min) ? min : (limit > max) ? max : limit
}

def setDisplay() {
    logDebug "setDisplay() was called"
    String display = "Water Seconds: "+ device.currentValue("runSeconds")+"<br>Run Interval: "+ device.currentValue("runIntervalHours")+"hrs<br>State: "+device.currentValue("operatingState")
    sendEvent(name: "display", value: display, descriptionText: getDescriptionText("display set to ${display}"))
    String display2 = "Last Run: "+ device.currentValue("lastWater")+"<br>Next Run: "+ device.currentValue("nextWater")+"<br>Timer: "+device.currentValue("timerStatus")+"<br>"
    sendEvent(name: "display2", value: display2, descriptionText: getDescriptionText("display2 set to ${display2}"))
}

def setOperatingState (state) {
    logDebug "setOperatingState(${state}) was called"
    sendEvent(name: "operatingState", value: state, descriptionText: getDescriptionText("operatingState set to ${state}")) 
    runIn(1, setDisplay)
}

def setRunSeconds(secs) {
    logDebug "setRunSeconds(${secs}) was called"
    sendEvent(name: "runSeconds", value: secs, descriptionText: getDescriptionText("runSeconds set to ${secs}"))   
    runIn(1, setDisplay)
}

def setRunIntervalHours(hrs) {
    logDebug "setRunIntervalHours(${hrs}) was called"
    sendEvent(name: "runIntervalHours", value: hrs, descriptionText: getDescriptionText("runIntervalHours set to ${hrs}"))   
    runIn(1, setDisplay)
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def waterStop() {
    def runInHours = ((device.currentValue("runIntervalHours")).toInteger() * 3600)
    String verb = (device.currentValue("switch") == "off") ? "is" : "was turned"
    eventSend("switch",verb,"off")
    sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
    setOperatingState("idle")
    runIn(1, setDisplay)
    
    def timerStat = (device.currentValue("timerStatus"))
    if (timerStat == "Running") {
        runIn(1, setNextWaterTime)
        runIn(runInHours, waterStart)
    } else {
        sendEvent(name: "nextWater", value: "none", descriptionText: getDescriptionText("nextWater set to none"))
    }
    runIn(1,setDisplay)
}

def waterStart() {   
    def timerStat = (device.currentValue("timerStatus"))

    if (timerStat == "Running") {
        def runSecs = (device.currentValue("runSeconds")).toInteger()
        
        String verb = (device.currentValue("switch") == "on") ? "is" : "was turned"
        eventSend("switch",verb,"on")    
        sendEvent(name: "contact", value: "open", isStateChange: forceUpdate)
        setOperatingState("watering") 
        fert = device.currentValue("fertilize")

        runIn(1, setDisplay) 
        runIn(1, setWaterTime)
        runIn(runSecs, waterStop)
    } else {
        sendEvent(name: "lastWater", value: "none", descriptionText: getDescriptionText("lastWater set to none"))
    }
}

def startTimer() {
    setTimerStatus("Running")
    runIn(1,waterStart)
}

def stopTimer() {
    setTimerStatus("Stopped")
}

def setTimerStatus(status) {
    logDebug "setTimerStatus(${status}) was called"
    sendEvent(name: "timerStatus", value: status, descriptionText: getDescriptionText("timerStatus set to ${status}"))  
    runIn(1,setDisplay)
    if (status == "Running") {
        runIn(1,waterStart)
    } else {   
        runIn(1,waterStop)
    }    
}

def setWaterTime() {
    logDebug "setWatertime() was called"
    Date date = new Date();   // given date
    Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
    calendar.setTime(date);   // assigns calendar to given date 
    def hour24 = calendar.get(Calendar.HOUR_OF_DAY).toInteger()
    def hour = calendar.get(Calendar.HOUR)
    if (hour.toInteger() == 0) {
        hour = "12"
    }    
    logDebug "hour is ${hour}"
    def meridian = "AM"
    if (hour24 >= 12) {
        meridian = "PM"
    } 
    def minute = calendar.get(Calendar.MINUTE)
    if (minute.toInteger() < 10) {
        minute = "0" + minute
    }       
    def last = hour+":"+minute+" "+meridian       // gets time in 12h format
    sendEvent(name: "lastWater", value: last, descriptionText: getDescriptionText("lastWater set to ${last}"))   
}

def setNextWaterTime() {
    logDebug "setNextWaterTime() was called"
    Date date = new Date();   // given date
    Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
    calendar.setTime(date);   // assigns calendar to given date 
    def interval = device.currentValue("runIntervalHours").toInteger()
    calendar.add(Calendar.HOUR_OF_DAY, interval); 
    def nextHour24 = calendar.get(Calendar.HOUR_OF_DAY).toInteger()
    logDebug "nextHour24 is ${nextHour24}"
    def nextMeridian = "AM"
    def nextHour = calendar.get(Calendar.HOUR)
    logDebug "nextHour is ${nextHour}"
    if (nextHour.toInteger() == 0) {
        nextHour = "12"
    }
    if (nextHour24 >= 12) {
        nextMeridian = "PM"
    }
    def nextMinute = calendar.get(Calendar.MINUTE)
    if (nextMinute.toInteger() < 10) {
        nextMinute = "0" + nextMinute
    }    
    def next = nextHour+":"+nextMinute+" "+nextMeridian       // gets time in 12h format
    sendEvent(name: "nextWater", value: next, descriptionText: getDescriptionText("nextWater set to ${next}"))       
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