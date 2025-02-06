/**
 *  **************** Freezer Alarm  ****************
 *
 *  Usage:
 *  This was designed to detect when a freezer temperature is no longer updating. 
 *  
 *  
 *  
 *  
**/

definition (
    name: "Freezer Alarm",
    namespace: "Hubitat",
    author: "cb",
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

        section("<b>Freezer Temperature Device</b>") {
            input (
              name: "freezerTemp", 
              type: "capability.temperatureMeasurment", 
              title: "Select Freezer Temp Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Alarm Variable Device</b>") {
            input (
              name: "alarmVariable", 
              type: "capability.variable", 
              title: "Select Alarm Variable Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }        

        section("") {
            input (
                name: "alarmWait", 
                type: "number", 
                title: "Enter how many minutes to wait before new temp update", 
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
    initialize()

}
def initialize() {

    subscribe(freezerTemp, "temperature", tempHandler)

}

def tempHandler(evt) {
    logDebug("Received temperature ${evt.value}")
    def seconds = alarmWait.toInteger() * 60
    runIn(seconds, alarmHandler)
}

def alarmHandler() {
    logDebug("Alarm Handler Fired")
    alarmVariable.setVariable("ALARM")
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