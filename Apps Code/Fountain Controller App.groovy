/**
 *  **************** Fountain Controller App ****************
 *
 *  Usage:
 *  This was designed to keep a fountain water reseviour filled, using a smart valve and a leak sensor to detect fill level. 
 *  It also turns the fountain on and off based on a motion sensor in the fountain area. 
 *  
 *  
 *  v. 1.0 - 4/18/25 - Inital code
 *  v. 1.3 - 5/02/25 - Added log to google device option to log valve, sensor, and pump states on changes
 *  v. 1.5 - 5/10/25 - Added fill check while running, and overfill timer to go a bit above level when filling. 
**/

definition (
    name: "Fountain Controller App",
    namespace: "Hubitat",
    author: "Burgess",
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
            label title: "", required: false
        }        

        section("<b>Fountain Pump Switch</b>") {
            input (
              name: "fountainSwitch", 
              type: "capability.switch", 
              title: "Select Fountain Pump Switch Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Water Filler Valve Switch</b>") {
            input (
              name: "fillValve", 
              type: "capability.switch", 
              title: "Select Water Valve Switch Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Filled Sensor</b>") {
            input (
              name: "fillSensor", 
              type: "capability.contactSensor", 
              title: "Select Filled Leak Sensor Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }        

        section("<b>Back Door Motion Sensor</b>") {
            input (
              name: "motionSensor", 
              type: "capability.motionSensor", 
              title: "Select Back Door Motion Sensor Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }    

        section("<b>Log To Google Device</b>") {
            input (
                name: "googleLogs", 
                type: "capability.actuator",
                title: "Select Log to Google Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )        
        }

        if (googleLogs) {
            section("") {
                input (
                    name: "googleLogging", 
                    type: "bool", 
                    title: "Enable Google logging", 
                    required: false, 
                    defaultValue: false
                )
            }
        }

        section("") {
            input (
                name: "autoOff",
                type: "enum",
                title: "<font style='font-size:14px; color:#1a77c9'>Auto Off Minutes</font>",
                options: ["600":10,"900":15,"1200":20,"1800":30,"2700":45,"3600":60,"5400":90,"7200":120],
                multiple: false,
                defaultValue: 30,
                required: true
            )
        }

        section("") {
            input (
                name: "autoStop",
                type: "enum",
                title: "<font style='font-size:14px; color:#1a77c9'>Auto Fill Stop Seconds</font>",
                options: ["60":60, "90":90,"120":120,"180":180,"240":240,"300":300,"360":360,"420":420,"480":480,"540":540,"600":600],
                multiple: false,
                defaultValue: 60,
                required: true
            )
        }

        section("") {
            input (
                name: "autoFill", 
                type: "bool", 
                title: "Enable Auto Fill", 
                required: true, 
                defaultValue: false
            )
        }

        section("") {
            input (
                name: "onMotion", 
                type: "bool", 
                title: "Enable Fountain On with Motion", 
                required: true, 
                defaultValue: false
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

        section("") {
            input (
                name: "logLevel",
                type: "enum",
                title: "<font style='font-size:14px; color:#1a77c9'>Logging Level</font>",
                options: [1:"Info", 2:"Warning", 3:"Debug"],
                multiple: false,
                defaultValue: 2,
                required: true
            )
        }        
    }
}

def installed() {
  initialize()
}

def updated() {

    if (settings?.debugMode) {
        if (settings?.logLevel == "3") {
            runIn(3600, logDebugOff)   // one hour
            logDebug("Debug log level enabled",3)
            logDebug("Log Level will change from Debug to Info after 1 hour",2)
        } else if (settings?.logLevel == "1") {
            logDebug("Info logging Enabled",1)
        } else logDebug("Warning log level enabled",2)
    }

    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {

    if (autoFill) {subscribe(fillSensor, "contact", waterSensorController)}
    if (onMotion) {subscribe(motionSensor, "motion", motionController)}
    subscribe(fillValve, "switch", valveController)
    subscribe(fountainSwitch, "switch", fountainPower)
    subscribe(fillValve, "waterConsumed", logWater)

    //if (autoFill) {schedule('0 0/50 * * * ?', checkFilled)}   // check every hour at :50
}

def logToGoogle() {

    if (googleLogging) {
        def valveState = 0
        def sensorState = 0
        def pumpState = 0

        def water = fillValve.currentValue("waterConsumed")
        if (fillValve.currentValue("switch") == "on") {valveState = 2}
        if (fillSensor.currentValue("contact") == "closed") {sensorState = 1.5}  // closed is dry
        if (fountainSwitch.currentValue("switch") == "on") {pumpState = 1}

        logParams = "Valve="+valveState+"&Sensor="+sensorState+"&Pump="+pumpState+"&water="+water

        // log to google 
        logDebug("Fountain Log params are: ${logParams}")
        googleLogs.sendLog("Fountain", logParams)
    }
}

def logWater(evt) {
    logToGoogle()
}

// log valve
def valveController(evt) {
    logToGoogle()
}

// check filled 30 seconds after fountain turns on
def fountainPower(evt) {

    if (evt.value == "on") {
        runIn(30, checkFilled)
    }
    runIn(1,logToGoogle)
}

// trigger a stop fill when sensor open (filled)
def waterSensorController(evt) {
    logDebug("waterSensorController called with ${evt.value}")
    def sensor = evt.value

    if (settings?.autoFill) {
        if (sensor == "open") {
            runIn(30, stopFill)  // fill a bit over the sensor for 30 sec
        } else if (sensor == "closed") {
            logToGoogle()
        }
    }
}

// start auto fill
def startFill() {
    logDebug("startFill Called")
    fillValve.on()
    runIn(60, stopFill)
    def secs = (settings?.autoStop).toInteger()
    logDebug("Stopping fill in ${secs} seconds")
    runIn(secs, stopFill)
    runIn(1,logToGoogle)
}

// stop auto fill
def stopFill() {
    logDebug("stopFill Called")
    fillValve.off()
    runIn(1,logToGoogle)
}

// call start fountain with motion
def motionController(evt) {
    logDebug("Motion event ${evt.value}")
    if (settings?.onMotion) {
        def sensor = evt.value
        if (sensor == "active") {
            startFountain()
        } 
    }
}

// start fountain 
def startFountain() {
    logDebug("Starting Fountain for ${settings?.autoOff} minutes")
    if (fountainSwitch.currentValue("switch") == "off") {
        fountainSwitch.on()   
        runIn(30, checkFilled)     
        runIn(1,logToGoogle)
    }
    def timerMinutes = (settings?.autoOff).toInteger()
    runIn(timerMinutes, stopFountain)
}

// stop fountain on timeout
def stopFountain() {
    logDebug("Turning off Fountain")
    fountainSwitch.off()
    unschedule()
    runIn(1,logToGoogle)
    runIn(2,confirmOff)
}

// send extra off to confirm
def confirmOff() { 
    if (fountainSwitch.currentValue("switch") == "on") {
        fountainSwitch.off()
    }
}

// check fill state to start Auto Fill
def checkFilled() {
    logDebug("Check Filled called")
    def powerOn = fountainSwitch.currentValue("switch") == "on"

    if (settings?.autoFill && powerOn) {
        def sensor = fillSensor.currentValue("contact")
        logDebug("Sensor is ${sensor}")
        if (sensor == "closed") {
            startFill()
        } else if (sensor == "open") {
            stopFill()
        }    
    } else {logDebug("autoFill is off")}

    if (powerOn) {
        runIn(600, checkFilled)   // check every 10 minutes for fill
    } else {unschedule()}
}
    

// log debug if no logLevel added
def logDebug(txt) {
    try {
        if (settings?.debugMode) {
            log.debug("${app.label} - ${txt}")   // debug
        }
    } catch(ex) {
        log.error("bad debug message")
    }    
}

// log by level when lvl supplied
def logDebug(txt, lvl){
    try {
        logLevel = settings?.logLevel.toInteger()
        if (settings?.debugMode) {
            if (lvl == 3 && logLevel == 3) log.debug("${app.label} - ${txt}")       // debug
            else if (lvl >= 2 && logLevel >= 2) log.warn("${app.label} - ${txt}")   // warn
            else if (lvl >= 1 && logLevel >= 1) log.info("${app.label} - ${txt}")   // info
        }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("logLevel",[value:"1",type:"enum"])
}