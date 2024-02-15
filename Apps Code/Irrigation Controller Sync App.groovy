/**
 *  ****************  Irrigation Controller Sync  ****************
 *
 *  Usage:
 *  This was designed to update a virtual irrigation controller virtual device from an external soil moisture sensor
 *  It also syncs settings attributes with hub variable devices to be used in dashboards. 
 *    
 *  v.1.0 04/19/23  Initial code
 *  v.1.5 04/26/23  Added water and fertilize pumps activation option
 *  v.2.0 05/24/23  Add Sync for hub variables to attributes and vice-versa
 *  v.2.1 11/17/23  Added google logging (needs google logs virtual driver device installed, and scripts added to the google sheet)
 *  v.2.2 12/15/23  Changed Fertilize to use concentrated mix, for only x seconds, to run when min at target time elapses after watering starts.  
 *  v.2.3 01/09/24  Change Sync for variables to use a single settings variable and pushed buttons to update each attribute with settings value. 

**/
import groovy.time.*

definition (
    name: "Irrigation Controller Sync",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync soil moisture sensor's moisture to a virtual irrigation controller device, and sync attributes to hub variables",
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

        section("<b>Virtual Irrigation Controller</b>") {

            input (
              name: "irrigationController", 
              type: "capability.actuator", 
              title: "Select Virtual Irrigation Controller Device", 
              required: true, 
              multiple: false,
              submitOnChange: true  
            ) 
        }

        section("<b>Moisture Sensor Device</b>") {
            input (
                name: "moistureSensor", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Soil Moisture Sensor Device", 
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
                required: true, 
                multiple: false,
                submitOnChange: true
            )        
        }

        section("<b>Dehydrator</b>") {
            input (
                name: "dehydrator", 
                type: "capability.switch",
                title: "Select Dehydrator Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )     
        }

        section("<b>Use Water Pump Switch Device</b>") {
            input (
              name: "selectController", 
              type: "bool", 
              title: "Activate Water Pump Device when Watering", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController) {
                state.useWaterPump = true
                section("<b>Water Pump Switch Device</b>") {
                    input (
                        name: "waterPump", 
                        type: "capability.switch", 
                        title: "Select Water Pump Switch Device", 
                        required: true, 
                        multiple: false
                    )
                }
            } else {
                state.useWaterPump = false            
            }
        }        

        section("<b>Use Fertilize Pump Switch Device</b>") {
            input (
              name: "selectFertilize", 
              type: "bool", 
              title: "Activate Fertilize Pump Device when Fertilizing", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectFertilize) {
                state.useFertilizePump = true
                section("<b>Water Pump Switch Device</b>") {
                    input (
                        name: "fertilizePump", 
                        type: "capability.switch", 
                        title: "Select Fertilize Pump Switch Device", 
                        required: true, 
                        multiple: false
                    )
                }
            } else {
                state.useFertilizePump = false            
            }
        }   

        // optional use Settings variable?
        section("<b>Use Settings Buttons and Variable</b>") {
            input (
              name: "selectSettings", 
              type: "bool", 
              title: "Use Settings Buttons and Variable?", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectSettings) {
                state.useSettings = true
                section("<b>Settings Number Variable Device</b>") {
                    input (
                    name: "settingsVariable", 
                    type: "capability.actuator", 
                    title: "Select Settings Variable Number Device", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true  
                    )
                }
                section("<b>Virtual Buttons Device</b>") {
                    input (
                    name: "settingsButtons", 
                    type: "capability.pushableButton", 
                    title: "Select Virtual Buttons Device", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true  
                    )
                } 
            }  else {
                state.useSettings = false            
            }     
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
    state.sensorMoisture = 50
    state.water = "off"
    state.fertilize = "off"
    initialize()
}

def updated() {

    if (settings?.debugMode) runIn(3600, logDebugOff)   // one hour

    initialize()
}

def initialize() {

    subscribe(moistureSensor, "humidity", moistureSync)  // also does logs
    subscribe(irrigationController, "contact", waterController)
    subscribe(irrigationController, "lock", fertilizeController)
    subscribe(irrigationController, "startMoisture", startMoistureController)

    subscribe(irrigationController, "operatingState", logOnStateChange)  // For logs

    if (state.useSettings) subscribe(settingsButtons, "pushed", settingsController)
    startOffTimer()   // confirms pumps are off every 15 minutes when state is idle
}

def moistureSync(evt) {

    state.sensorMoisture = evt.value
    logDebug("Moisture Sensor Event = $state.sensorMoisture")
    def percent = evt.value.toInteger()

    irrigationController.setMoisture(percent)
    logMoisture(evt.value.toInteger())
}

// log moisture event to google
def logMoisture(moisture) {
    def state = irrigationController.currentValue("operatingState")
    def logParams = ""
    def startMoisture = irrigationController.currentValue("startMoisture")

    if (state == "watering" || state =="target") {
        def stopMoisture = irrigationController.currentValue("stopMoisture")       
        //state.stopMoisture = stopMoisture
        logParams = "Moisture="+moisture+"&Start Moisture="+startMoisture+"&Watering="+stopMoisture
    } else {
        logParams = "Moisture="+moisture+"&Start Moisture="+startMoisture
    }

    // log to google 
    logDebug("Moisture Log params are: ${logParams}")
    googleLogs.sendLog(getSheetName(), logParams)
}

// log moisture when start moisture change
def startMoistureController(evt) {
    def moisture = irrigationController.currentValue("moisture") 
    logMoisture(moisture)
}

def waterController(evt) {  
    def contact = evt.value

    if (contact == "open") {
        state.water = "on"
        moistureSensor.resetHumidity()
        if (state.useWaterPump) waterPump.on()
    } else {
        state.water = "off"
        def max = moistureSensor.currentValue("maxHum")
        irrigationController.setStopMoisture(max)
        irrigationController.setMaxMoisture(max)
        if (state.useWaterPump) waterPump.off()
    }
}

def fertilizeController(evt) {
    def lock = evt.value

    if (lock == "locked") {
        state.fertilize = "on"
        if (state.useFertilizePump) fertilizePump.on()
    } else if (lock == "unlocked") {
        state.fertilize = "off"
        if (state.useFertilizePump) fertilizePump.off()
    }
}

def settingsController(evt) {  
    def setting = evt.value.toInteger()
    def value = settingsVariable.currentValue("variable").toInteger()

    // Set Start Moisture
    if (setting == 1) {irrigationController.setStartMoisture(value)}
    // set min at target
    if (setting == 2) {irrigationController.setMinutesAtTarget(value)}
    // set fertilize seconds
    if (setting == 3) {irrigationController.setFertilizeSec(value)}
    // max minutes
    if (setting == 4) {irrigationController.setMaxMinutes(value)}
    // toggle next Fertilize value
    if (setting == 5) {
        def current = irrigationController.currentValue("fertilize")
        if (current == "false") {irrigationController.setFertilize("off")}
        if (current == "true") {irrigationController.setFertilize("false")}
        if (current == "off")  {irrigationController.setFertilize"true"}
    }
    // set minutes at trigger
    if (setting == 6) {irrigationController.setMinutesAtTrigger(value)}
    if (setting == 7 && dehydrator) {dehydrator.setAutoOff(value)}
}


// Log with operating state changes
def logOnStateChange(evt) {
    logDebug("Operating State changed to ${evt.value}")
    def operatingState = evt.value
    def logParams = ""
    def startMoisture = irrigationController.currentValue("startMoisture")
    def stopMoisture = irrigationController.currentValue("stopMoisture")

    if (operatingState == "watering") {
        def now = new Date()
        state.startTime = now.toString()
        state.startMoisture = startMoisture       
        logParams = "Moisture="+stopMoisture+"&Start Moisture="+startMoisture+"&Watering="+stopMoisture
        logDebug("watering log params: ${logParams}")
        googleLogs.sendLog(getSheetName(), logParams)   
        logDebug("logging with Watering")    
    }
    if (operatingState == "idle") {
        logDebug("Logging Idle")
        def end = new Date()
        def start = Date.parse("E MMM dd H:m:s z yyyy", state?.startTime)
        use(TimeCategory)
        {
            def waterDuration = end - start
            state.waterDuration = waterDuration
        }
        def wateringMinutes = (state?.waterDuration.getHours() * 60) + state?.waterDuration.getMinutes()
        state.wateringMinutes = wateringMinutes
        logDebug("Watering Minutes is ${wateringMinutes}")
        
        state.stopMoisture = stopMoisture
        def moisture = moistureSensor.currentValue("humidity")
        logParams = "Moisture="+moisture+"&Start Moisture="+startMoisture+"&Watering="+stopMoisture+"&Duration="+wateringMinutes
        logDebug("idle log params: ${logParams}")
        googleLogs.sendLog(getSheetName(), logParams)    
    }
}

String getSheetName() {
    String sheet = ""

    // Set sheet by controller
    if (irrigationController.displayName == "Grow Water Controller") sheet = "Grow Water"
    if (irrigationController.displayName == "Flowering Water Controller") sheet = "Flower Water"
    if (irrigationController.displayName == "Garden Water Controller Shed") sheet = "Shed Garden Water"
    if (irrigationController.displayName == "Garden Water Controller Strawberries") sheet = "Strawberry Garden Water"
    if (irrigationController.displayName == "Garden Water Controller Yard") sheet = "Yard Garden Water"
    logDebug("Sheet name is ${sheet}")

    return sheet
}

def startOffTimer() {
   runIn(900,pumpsOff)  // 15 minutes
}

// Turn pumps off every 15 min if idle - in case one gets left on (as fail-safe)
def pumpsOff() {    
    logDebug("Confirming Pumps Off")
    def state = irrigationController.currentValue("operatingState")

    if (state == "idle") {
        logDebug("Turning off pumps")
        irrigationController.off()
        waterPump.off()
        fertilizePump.off()
    }
    startOffTimer()
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("debugMode",[value:"false",type:"bool"])
}