/**
 *  ****************  Solar Pool Heater Controller App ****************
 *
 *  Usage:
 *  Uses companion driver "Solar Pool Heater Controller"
 *  This was designed to update a virtual Solar Heater controller's temp and outside illuminance from external sensors
 *  The temp sensor is in the heater box, and the illuminance sensor is near or inside the heater.
 *  The driver does the cycle logic, and the app controls a pump switch device
 *   
 *  Turns on/off a switch, and/or sets the fan speed for the fan when the driver's operatingState changes 
 * 
 *    
 * v. 1.0 4/18/23   - Initial code
\

**/

definition (
    name: "Solar Pool Heater Controller App",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync illuminance and temperature to solar heater controller device, and control the heater pump switch",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Solar Heater Controller</b>") {

            input (
              name: "solarHeaterController", 
              type: "capability.actuator", 
              title: "Select Solar Heater Controller Device", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            
            if (solarHeaterController) {
                input (
                    name: "trackSolarHeater", 
                    type: "bool", 
                    title: "Track Solar Heater Device Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }

        section("<b>Solar Heater Temp Sensor Device</b>") {
            input (
                name: "heaterTemperature", 
                type: "capability.temperatureMeasurement", 
                title: "Select Solar Heater Temp Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (heaterTemperature) {
                input (
                    name: "trackHeaterTemp", 
                    type: "bool", 
                    title: "Track Heater Temperature Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }

        section("<b>Illluminance Sensor Device</b>") {
            input (
                name: "illuminance", 
                type: "capability.illuminanceMeasurement", 
                title: "Select Outside Illuminance Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (illuminance) {
                input (
                    name: "trackIlluminance", 
                    type: "bool", 
                    title: "Track Outside Illuminance Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }

        section("<b>Pool Temperature Sensor Device</b>") {
            input (
                name: "poolSensor", 
                type: "capability.temperatureMeasurement", 
                title: "Select Pool Temperature Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )
            if (poolSensor) {
                input (
                    name: "trackPoolTemp", 
                    type: "bool", 
                    title: "Track Pool Temperature Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }                   
        }        

        section("<b>Heater Pump Switch Device</b>") {
            input (
                name: "heaterPump", 
                type: "capability.switch", 
                title: "Select Heater Pump Switch Device", 
                required: true, 
                multiple: false
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
    state.illuminance = 0
    state.heaterTemp = 75.1
    state.poolTemp = 70.1
    state.operatingState = "idle"
    state.switch = "off"
    initialize()
}

def updated() {

    if (settings?.debugMode) runIn(3600, logDebugOff)   // one hour
    initialize()
}

def initialize() {
    subscribe(heaterTemperature, "temperature", setHeaterTemp)
    subscribe(illuminance, "illuminance", setIlluminance)
    subscribe(poolSensor, "temperature", setPoolTemp)
    subscribe(solarHeaterController, "operatingState", setOperatingState)
}

def setHeaterTemp(evt) {

    state.heaterTemp = evt.value.toBigDecimal()
    logDebug("Heater Temperature Event = $state.heaterTemp")
    def lvl = evt.value.toBigDecimal()

    solarHeaterController.setHeaterTemp(lvl)     
}

def setIlluminance(evt) {

    state.illuminance = evt.value.toInteger()
    logDebug("Illluminance Event = $state.illuminance")
    def lvl = evt.value.toInteger()

    solarHeaterController.setIlluminance(lvl) 
}

def setPoolTemp(evt) {

    state.poolTemp = evt.value.toBigDecimal()
    logDebug("Pool Temperature Event = $state.poolTemp")
    def lvl = evt.value.toBigDecimal()

    solarHeaterController.setPoolTemp(lvl) 
}

def setOperatingState(evt) {
    state.operatingState = evt.value 
    logDebug("Heater Operating State Event = $state.operatingState")
    def state = evt.value

    if (state == "heating") {
        switchOn()
    }
    if (state == "idle") {
        switchOff()
    }
}

def switchOff() {
    state.switch = "off" 
    heaterPump.off() 
}

def switchOn() {
    state.switch = "on"
    heaterPump.on()
}

def logDebug(txt) {
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