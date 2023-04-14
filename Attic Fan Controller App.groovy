/**
 *  ****************  Attic Fan Controller App ****************
 *
 *  Usage:
 *  Uses companion driver "Attic Fan Controller"
 *  This was designed to update a virtual attic fan controller's humidity and temp from external sensors
 *  The temp and humidity sensors are Outside and in the Attic.
 *  The driver does the cycle logic, and the app controls a fan speed controller or a switch device
 *   
 *  Turns on/off a switch, and/or sets the fan speed for the fan when the driver's operatingState changes 
 * 
 *    
 * v. 1.0 4/3/23   - Initial code
 * v. 1.2 4/7/23   - updated to use a speed controller device
 * v. 1.5 4/14/23  - added choice for switch or speed controller at setup

**/

definition (
    name: "Attic Fan Controller App",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync Attic and Outside Humidity and Temp to an Attic Fan Controller Device, and control the attic fan",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Attic Fan Controller</b>") {

            input (
              name: "atticFanController", 
              type: "capability.actuator", 
              title: "Select Attic Fan Controller Device", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            
            if (atticFanController) {
                input (
                    name: "trackAtticFan", 
                    type: "bool", 
                    title: "Track Attic Fan Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }

        section("<b>Outside Humidity Sensor Device</b>") {
            input (
                name: "outsideHumidity", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Outside Humidity Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (outsideHumidity) {
                input (
                    name: "trackOutsideHumidity", 
                    type: "bool", 
                    title: "Track Outside Humidity Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }

        section("<b>Outside Temperature Sensor Device</b>") {
            input (
                name: "outsideTemperature", 
                type: "capability.temperatureMeasurement", 
                title: "Select Outside Temperature Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (outsideTemperature) {
                input (
                    name: "trackOutsideTemperature", 
                    type: "bool", 
                    title: "Track Outside Temperature Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }
        
        section("<b>Attic Humidity Sensor Device</b>") {
            input (
                name: "atticHumidity", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Attic Humidity Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (atticHumidity) {
                input (
                    name: "trackAtticHumidity", 
                    type: "bool", 
                    title: "Track Attic Humidity changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }

        section("<b>Attic Temperature Sensor Device</b>") {
            input (
                name: "atticTemperature", 
                type: "capability.temperatureMeasurement", 
                title: "Select Attic Temperature Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (atticTemperature) {
                input (
                    name: "trackAtticTemperature", 
                    type: "bool", 
                    title: "Track Attic Temperature changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }

        section("<b>Speed Controller or Switch</b>") {
            input (
              name: "selectController", 
              type: "bool", 
              title: "Use fan Speed Controller to operate the fan", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController) {
                state.useSpeed = true
                section("<b>Attic Fan Speed Controller Device</b>") {
                    input (
                        name: "fan", 
                        type: "capability.fanControl", 
                        title: "Select Attic Fan Speed Controller Device", 
                        required: true, 
                        multiple: false
                    )
                }
            } else {
                state.useSpeed = false
                input (
                    name: "switch", 
                    type: "capability.switch", 
                    title: "Select Attic Fan Switch Device", 
                    required: true, 
                    multiple: false
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
    state.outsideHumidity = 50
    state.atticHumidity = 50
    state.outsideTemp = 75.0
    state.atticTemp = 75.0
    state.operatingState = "idle"
    state.fanSwitch = "off"
    state.fanSpeed = "off"
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    subscribe(outsideHumidity, "humidity", setOutsideHumidity)
    subscribe(atticHumidity, "humidity", setAtticHumidity)
    subscribe(outsideTemperature, "temperature", setOutsideTemp)
    subscribe(atticTemperature, "temperature", setAtticTemp)    
    subscribe(atticFanController, "operatingState", setOperatingState)
    subscribe(atticFanController, "fanSpeed", setSpeed)
}

def setOutsideHumidity(evt) {

    state.outsideHumidity = evt.value.toInteger()
    logDebug("Outside Humidity Event = $state.outsideHumidity")
    def lvl = evt.value.toInteger()

    atticFanController.setOutsideHumidity(lvl)     
}

def setAtticHumidity(evt) {

    state.atticHumidity = evt.value.toInteger()
    logDebug("Attic Humidity Event = $state.atticHumidity")
    def lvl = evt.value.toInteger()

    atticFanController.setAtticHumidity(lvl) 
}

def setOutsideTemp(evt) {
    state.outsideTemp = evt.value.toBigDecimal()
    logDebug("Outside Temp Event = $state.outsideTemp")
    def lvl = evt.value.toBigDecimal()

    atticFanController.setOutsideTemp(lvl) 
}

def setAtticTemp(evt) {
    state.atticTemp = evt.value.toBigDecimal()
    logDebug("Attic Temp Event = $state.atticTemp")
    def lvl = evt.value.toBigDecimal()

    atticFanController.setAtticTemp(lvl) 
}

def setOperatingState(evt) {
    state.operatingState = evt.value  
    logDebug("Attic Operating State Event = $state.operatingState")
    def state = evt.value

    if (state == "venting") {
        fanOn()
    }
    if (state == "idle") {
        fanOff()
    }
}

def fanOff() {
    if (state.useSpeed) fan.setSpeed("off")
        else fan.off
    state.fanSwitch = "off"  
}

def fanOn() {
    if (state.useSpeed) fan.setSpeed(state.fanSpeed)
        else fan.on
    state.fanSwitch = "on"
}

def setSpeed(evt) {
    state.fanSpeed = evt.value.toString()
    if (state.fanSwitch == "on") {
        fan.setSpeed(state.fanSpeed)                   // update current speed
    } 
}

def logDebug(txt) {
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}