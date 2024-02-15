/*

Log To Google

v 1.0 - 

*/

metadata {
	definition (
			name: "Log to Google",
			namespace: "hubitat",
			author: "Chris B."
	) { 
        
		capability "Actuator"

        //TYPES: ENUM, STRING, DYNAMIC_ENUM, JSON_OBJECT, NUMBER, DATE, VECTOR3
        attribute "googleUri", "string"
        attribute "cleanUri", "string"
        attribute "tab", "string"
        attribute "params", "string"
        attribute "lastCleaned", "string"
        attribute "deleteDays", "string"


		// Commands needed to change internal attributes of virtual device.
        command "setGoogleUri", ["string"]
        command "setCleanUri", ["string"]
        command "sendLog", [[name:"tab",type:"STRING", description:"Set Sheet Tab"],[name:"params",type:"STRING", description:"Set Log Params"]]
        command "cleanLog", [[name:"tab",type:"STRING", description:"Set Sheet Tab"],[name:"deleteDays",type:"STRING", description:"Set days to keep"]]
        command "startCleanTimer", "number"
	}

	preferences {
		input( name: "logEnable", type:"bool", title: "Enable logging",defaultValue: true)
		//input( name: "txtEnable", type:"bool", title: "Enable descriptionText logging", defaultValue: true)     
        input (name: "logLevel",  type: "enum", title: "<font style='font-size:14px; color:#1a77c9'>Logging Level</font>", options: [1:"Info", 2:"Warning", 3:"Debug"], defaultValue: 2) 
	}
}


def installed() {
	log.warn "installed..."
	initialize()
    state.deleteIndex = 0
}

def updated() {
	log.info "updated..."
	//log.warn "debug logging is: ${logEnable == true}"
	//log.warn "description logging is: ${txtEnable == true}"
	//if (logEnable) runIn(1800,logsOff)
	initialize()
    //state.deleteIndex = 0

    if (settings?.logEnable) {
        if (settings?.logLevel == "3") {
            runIn(3600, logDebugOff)   // one hour
            logDebug("Debug log level enabled",3)
            logDebug("Log Level will change from Debug to Info after 1 hour",2)
        } else if (settings?.logLevel == "1") {
            logDebug("Info logging Enabled",1)
        } else logDebug("Warning log level enabled",2)
    }    
}

def initialize() {
	if (state?.lastRunningMode == null) {
        
	}
    runIn(60,cleanLogs)
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

// Commands needed to change internal attributes of virtual device.

def setGoogleUri(uri) {
	logDebug("setGoogleUri(${uri}) was called",3)
    sendEvent(name: "googleUri", value: uri)
}

def setCleanUri(uri) {
	logDebug("setCleanUri(${uri}) was called",3)
    sendEvent(name: "cleanUri", value: uri)
}

def sendLog(tab,params) {
    sendEvent(name: "tab", value: tab)
    sendEvent(name: "params", value: params)

    def googleUri = device.currentValue("googleUri") + tab + "&" + params
    googleUri = googleUri.replace(" ","%20")
    logDebug("Send Uri is ${googleUri}",3)

    def getParams = [
		uri: googleUri,
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['CustomHeader':'CustomHeaderValue'],
	]
    asynchttpGet('logCallbackMethod', getParams, [dataitem1: "datavalue1"])

}

def logCallbackMethod(response, data) {
    def result = response.status.toInteger()
    logDebug("status of get call to log is: ${result}",3)
    if (result != 200) logDebug("Failed to Log: ${result}",2)
}

def cleanLog(String tab, String days) {
    def log = "sheet=" + tab + "&days=" + days
    getDelete(log)
}

def cleanLogs() {

    def logs = [
        "sheet=Light Levels&days=2",
        "sheet=Illuminance&days=5",
        "sheet=Grow Water&days=14",
        "sheet=Flower Water&days=14",
        "sheet=Clone Water&days=14",
        "sheet=Indoor Humidity&days=7",
        "sheet=Intensity Level&days=2",
        "sheet=Attic&days=5",
        "sheet=Bedroom Heat&days=1",
        "sheet=Living Room heat&days=1",
        ]

    def summerLogs = [
        "sheet=Shed Garden Water&days=7",
        "sheet=Yard Garden Water&days=7",
        "sheet=Bedroom AC&days=5",
        "sheet=Living Room AC&days=5",
        "sheet=Solar Heater&days=5",
        "sheet=Pool Temperature&days=7",
        "sheet=Strawberry Garden Water&days=14",
    ]

    def inactive = [
        "sheet=Avg Hourly Illuminance&days=7"
    ]

    def size = logs.size()
    int index = state?.deleteIndex

    def log = logs[index]
    getDelete(log)

    if (state?.deleteIndex == size) state.deleteIndex = 0
    else state.deleteIndex = state?.deleteIndex + 1

    def minutes = 720/size
    startCleanTimer(minutes)
}

def getDelete(log) {
    sendEvent(name: "lastCleaned", value: log)

    def googleUri = device.currentValue("cleanUri") + "?" + log
    googleUri = googleUri.replace(" ","%20")
    logDebug("Clean Uri is: ${googleUri}")

    def cleanParams = [
		uri: googleUri,
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['CustomHeader':'CustomHeaderValue'],
	]
    asynchttpGet('cleanCallbackMethod', cleanParams, [dataitem1: "datavalue1"])
}

def cleanCallbackMethod(response, data) {
    def resultCode = response.status.toInteger()
    logDebug("status of get call to clean is: ${result}",3)
    if (result != 200) logDebug("Failed Clean: ${result}",2)
}

def startCleanTimer(mins) {

    int minutes = mins * 60
    runIn(minutes, cleanLogs)

}

def parse(String description) {
	logDebug "$description"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}

// log debug if no logLevel added
def logDebug(txt) {
    try {
        if (settings?.logEnable) {
            log.debug("${device.getLabel()} - ${txt}")   // debug
        }
    } catch(ex) {
        log.error("bad debug message")
    }    
}

// log by level when lvl supplied
def logDebug(txt, lvl){
    try {
        logLevel = settings?.logLevel.toInteger()
        if (settings?.logEnable) {
            if (lvl == 3 && logLevel == 3) log.debug("${device.getLabel()} - ${txt}")       // debug
            else if (lvl >= 2 && logLevel >= 2) log.warn("${device.getLabel()} - ${txt}")   // warn
            else if (lvl >= 1 && logLevel >= 1) log.info("${device.getLabel()} - ${txt}")   // info
        }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off logDebug to warning",3)
    app.updateSetting("logLevel",[value:"2",type:"enum"])
}