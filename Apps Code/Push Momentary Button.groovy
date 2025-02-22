/**
 *  ****************  Push Momentary Button  ****************
 *
 *  Usage:
 *  This was designed to push a momenatary button using a virtual pushableButton Device
 *  
 *  Reacts to pushed button from virtual device, set in settings, to push a momentary button device
 *  
 *  
**/


definition (
    name: "Push Momentary Button",
    namespace: "Hubitat",
    author: "Chris B",
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

        section("<b>Virtual Pushable Button Device</b>") {
            input (
              name: "virtualButtons", 
              type: "capability.pushableButton", 
              title: "Select Virtual Button Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Momentary Button Device</b>") {
            input (
              name: "momentaryButton", 
              type: "capability.momentary", 
              title: "Select Momentary Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }        

        section("") {
            input (
                name: "pushed",
                type: "enum",
                title: "<font style='font-size:14px; color:#1a77c9'>Button Number Pushed</font>",
                options: ["1","2","3","4","5",],
                multiple: false,
                defaultValue: "1",
                required: false
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
    initialize()
}

def initialize() {
    subscribe(virtualButtons, "pushed", pushedButton)
}

def pushedButton(evt) {
    logDebug("pushedButton got ${evt.value}")

    def pushedButton = evt.value.toInteger()
    def pushedMomentary = (pushed).toInteger()

    if (pushedButton == pushedMomentary) {
        logDebug("Pushing Momentary Button")
        momentaryButton.push()
    }
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