/**
 *  ****************  Attic Fan Controller Sync  ****************
 *
 *  Usage:
 *  This was designed to update a virtual attic controller's humidity and temp from external sensors
 *  The temp and humidity sensors are Outside and in the Attic.
 *  Uses companion driver Attic Fan Controller, syncs the data, and controls a fan device switch
 *   
 *  Turns on/off a switch for the fan when the driver's operatingState changes 
 * 
 *    
 * v. 1.0 4/3/23 - Itital code
**/

definition (
    name: "Attic Fan Controller Sync",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync Attic and Outside Humidity and Temp to Attic Fan Controller Device",
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

        section("<b>Outside Temp/Humidity Sensor Device</b>") {
            input (
                name: "outside", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Outside Temp/Humidity Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (outside) {
                input (
                    name: "trackOutside", 
                    type: "bool", 
                    title: "Track Outside Temp/Humidity Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }
        
        section("<b>Attic Temp/Humidity Sensor Device</b>") {
            input (
                name: "attic", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Attic Temp/Humidity Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (attic) {
                input (
                    name: "trackAttic", 
                    type: "bool", 
                    title: "Track Attic Temp/Humidity changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }
        
        section("<b>Attic Fan Switch Device</b>") {
            input (
                name: "fan", 
                type: "capability.switch", 
                title: "Select Attic Fan Switch Device", 
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
    state.outsideHumidity = 50
    state.atticHumidity = 50
    state.outsideTemp = 75.0
    state.atticTemp = 75.0
    state.operatingState = "idle"
    state.fanSwitch = "off"
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(outside, "humidity", setOutsideHumidity)
    subscribe(attic, "humidity", setAtticHumidity)
    subscribe(outside, "temperature", setOutsideTemp)
    subscribe(attic, "temperature", setAtticTemp)    
    subscribe(atticFanController, "operatingState", setSwitch)

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
    logDebug("Outside Temp Event = $state.outsdieTemp")
    def lvl = evt.value.toBigDecimal()

    atticFanController.setOutsideTemp(lvl) 

}

def setAtticTemp(evt) {
    state.atticTemp = evt.value.toBigDecimal()
    logDebug("Attic Temp Event = $state.atticTemp")
    def lvl = evt.value.toBigDecimal()

    atticFanController.setAtticTemp(lvl) 

}

def setSwitch(evt) {
    state.operatingState = evt.value  
    logDebug("Attic Operating State Event = $state.operatingState")
    def state = evt.value

    if (state == "venting") {
        state.fanSwitch = "on"
        fan.on()
    }
    if (state == "idle") {
        state.fanSwitch = "off"
        fan.off()
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}