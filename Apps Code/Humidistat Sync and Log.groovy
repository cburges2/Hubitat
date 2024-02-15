/**
 *  ****************  Humidistat Sync and Log  ****************
 *
 *  Usage:
 *  This was designed to update a virtual humidistat's humidity from an external humidiy sensor
 *    
**/

definition (
    name: "Humidistat Sync and Log",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync a humidity sensor's humidity to a virtual humidistat device and log to Google",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Virtual Humidistat Controller</b>") {
            input (
              name: "humidistatController", 
              type: "capability.actuator", 
              title: "Select Virtual Humidistat Device", 
              required: true, 
              multiple: false               
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

        section("<b>Humidity Device</b>") {
            input (
                name: "humidity", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Humidity Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (humidity) {
                input (
                    name: "trackHumidity", 
                    type: "bool", 
                    title: "Track physical humidity changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }

        section("<b>Power Meter Device</b>") {
            input (
                name: "power1", 
                type: "capability.powerMeter", 
                title: "Select Power Meter Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
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
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(humidity, "humidity", humidityHandler)
    subscribe(humidistatController, "operatingState", logToGoogle)
    subscribe(humidistatController, "humidifyingSetpoint", logToGoogle)
    subscribe(humidistatController, "level", logToGoogle)

    state.lastOperatingState = "idle"
    state.humidistatState = null
}

def humidityHandler(evt) {

    state.sensorHumidity = evt.value
    logDebug("Humidity Sensor Event = $state.sensorHumidity")
    def lvl = evt.value.toInteger()

    humidistatController.setHumidity(lvl)  
}

def humidistatPowerController(evt) {

    state.humidistatPower = evt.value
    logDebug("Power Meter Event = $state.humidistatPower")
    def lvl = evt.value

    humidistatController.setPower(lvl)  
}

def logToGoogle(evt) {
    def humidity = humidity.currentValue("humidity")
    def setpoint = humidistatController.currentValue("humidifyingSetpoint")
    def water = humidistatController.currentValue("level")
    def state = humidistatController.currentValue("operatingState")
    def logParams = ""

    if (state == "idle") {
        if (state?.lastOperatingState == "idle") {
            if (state?.lastOperatingState == "humidifying") {
                state.humidistatState = setpoint
                logParams = "Humidity Setpoint="+setpoint+"&Thermo Humidity="+humidity+"&Thermo State="+state?.humidistatState+"&Water="+water
            } else {
                state.humidistatState = null
                logParams = "Humidity Setpoint="+setpoint+"&Thermo Humidity="+humidity+"&Water="+water
            }
        } else {
            state.humidistatState = setpoint
            logParams = "Humidity Setpoint="+setpoint+"&Thermo Humidity="+humidity+"&Thermo State="+state?.humidistatState+"&Water="+water
        }
    }
    state.lastOperatingState = state
    logToGoogle.sendLog("Indoor Humidity",logParams)
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}