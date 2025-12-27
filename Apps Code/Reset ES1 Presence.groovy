/**
 *  **************** Reset ES1 Presence  ****************
 *
 *  Usage:To fix the issue with Linptech/MOES ES1 mm-wave presence sensors that require them to be power cycled after a hub reboot or some elapsed time being online
 *      This app requires all sensors to be on power switches so they can be remotely power cycled. 
        A power-cycle for all sensors will take place after the reboot delay setting after a hub reboot.  
 *      A scheduled Job is set for a timed reboot to avoid the sensors going unresponsive. 
**/

definition (
    name: "Reset ES1 Presence",
    namespace: "Hubitat",
    author: "ChrisB",
    description: "",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("App Name") {
            label title: "Optionally assign a custom name for this app", required: false
        } 

        section("<b>USB Switch Devices to Reset Sensors</b>") {

            input (
              name: "switches", 
              type: "capability.switch", 
              title: "Select All Sensor USB Switch Devices", 
              required: true, 
              multiple: true           
            )
        }    

        section("<b>Days are: SUN MON TUE WED THU FRI SAT. Use Ranges (MON-FRI) or comma separated Individual Days (TUE,THU)<br>Enter NONE to not schedule any days</b>"){}

        section("<b>Back Hall</b>") {
            input (
                name: "resetTime", 
                type: "time", 
                title: "Reset Schedule", 
                width: 2,
                required: true, 
                defaultValue: '2024-04-06T14:00:00.000-0400'
            )
            input (
                name: "resetDays", 
                type: "string", 
                title: "Reset Days", 
                width: 2,
                required: true, 
                defaultValue: "SUN"
            )
        }

        section("") {
            input (
                name: "rebootDelay", 
                type: "number", 
                title: "Seconds after Reboot to Reset Sensors ", 
                required: true, 
                defaultValue: 300
            )
        }

        section("") {
            input (
                name: "resetDelay", 
                type: "number", 
                title: "Seconds to keep sensor Off before turning back on", 
                required: true, 
                defaultValue: 5
            )
        }

        section("") {
            input (
                name: "executeDelay", 
                type: "number", 
                title: "Milliseconds between switch commands", 
                required: true, 
                defaultValue: 100
            )
        }

        section("") {
            input (
                name: "debugMode", 
                type: "bool", 
                title: "Enable logging", 
                required: true, 
                defaultValue: false
            )
        }
    }
}

def installed() {
    updated()
}

def updated() {
    if (logEnable) runIn(1800,logsOff)
    state.action = "idle"

    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(location, "systemStart", "hubRestartHandler")

    // subscribe to turn USB switch back on if it gets turned off (using resetDelay)
    subscribe(switches, "switch", switchEventHandler)

    // Set the schedule
    def timeStr = settings?.resetTime.toString()
    time = timeStr.substring(11,16)    // get time from date 
    def values = time.split(":")
    def hour = values[0]        
    def min = values[1]

    String scheduled = "0 "+min+" "+hour+" ? * "+settings?.resetDays           // create cron schedule    
    schedule(scheduled, scheduleHandler)
}    

// Turn switch back on if it gets turned off accidentially - but ignore this when they are turned off by this app dong the action
def switchEventHandler(evt) {
    
    if (evt.value == "off" && state?.action == "idle") {
        logDebug("${evt.displayName} turned off")
        runIn(resetDelay, turnOnSwitches)    
    }
}

def scheduleHandler() {
    state.action = "schedule"
    restartSensors()
    runIn(resetDelay+10, resetAction)
}

def hubRestartHandler(evt) {
    state.action = "reboot"
    runIn(rebootDelay, restartSensors)
    runIn(rebootDelay+10,resetAction)
}

def resetAction() {
    state.action = "idle"
}

def restartSensors() {
    turnOffSwitches()
    runIn(resetDelay,turnOnSwitches)  
}

def turnOnSwitches() {
    if (settings?.debugMode) logDebug("Turning on switches")
    for (t=0; t < switches.size(); t++) {
        pauseOn(t)
    }
}

def pauseOn(t) {
    pauseExecution(executeDelay.toInteger()) 
    switches[t].on()
}

def turnOffSwitches() {
    if (settings?.debugMode) logDebug("Turning off switches")
    for (t=0; t < switches.size(); t++) {
        pauseOff(t)
    } 
}

def pauseOff(t) {
    pauseExecution(executeDelay.toInteger()) 
    switches[t].off()
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}