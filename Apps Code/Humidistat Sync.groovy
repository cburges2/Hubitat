/**
 *  ****************  Humidistat Sync  ****************
 *
 *  Usage:
 *  This was designed to update a virtual humidistat's humidity from an external humidiy sensor
 *    
**/

definition (
    name: "Humidistat Sync",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync a humidity sensor's humidity to a virtual humidistat device",
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
              name: "humidistatController1", 
              type: "capability.actuator", 
              title: "Select Virtual Humidistat Device", 
              required: true, 
              multiple: false               
            )
        }

        section("<b>Humidity Device</b>") {
            input (
                name: "humidity1", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Humidity Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (humidity1) {
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

            if (power1) {
                input (
                    name: "trackPower", 
                    type: "bool", 
                    title: "Track physical power usage", 
                    required: true, 
                    defaultValue: "true"
                )
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
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(humidity1, "humidity", humidistatHumidityController)
    subscribe(power1, "power", humidistatPowerController)

    state.sensorHumidity = humidity1.humidity
    state.humidistatPower = power1.power
}

def humidistatHumidityController(evt) {

    state.sensorHumidity = evt.value
    logDebug("Humidity Sensor Event = $state.sensorHumidity")
    def lvl = evt.value.toInteger()

    humidistatController1.setHumidity(lvl)  
}

def humidistatPowerController(evt) {

    state.humidistatPower = evt.value
    logDebug("Power Meter Event = $state.humidistatPower")
    def lvl = evt.value

    humidistatController1.setPower(lvl)  
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}