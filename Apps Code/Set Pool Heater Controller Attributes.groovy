/**
 *  **************** Set Pool Heater Controller Attributes  ****************
 *
 *  Usage:
 *  Set a device attribute using a dashboard variable tile
 *    
 *  v.1.0 5/3/23  Initial code
 *  

**/

definition (
    name: "Set Pool Heater Controller Attributes",
    namespace: "Hubitat",
    author: "cburgess",
    description: "Set Pool Heater Controller Device Attributes from dashboard variable connectors",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>On By Temperature Variable Device</b>") {

            input (
              name: "setOnTemp", 
              type: "capability.actuator", 
              title: "Select On by Temperature Setting Variable Device", 
              required: true, 
              multiple: false,
              submitOnChange: true  
            )
            if (setOnTemp) {
                input (
                    name: "trackOnTempeSetting", 
                    type: "bool", 
                    title: "Track On Temperature Setting Device", 
                    required: true, 
                    defaultValue: "true"
                )
            }      
        }

        section("<b>On By Illumination Variable Device</b>") {

            input (
              name: "setOnIllum", 
              type: "capability.actuator", 
              title: "Select On by Illumination Setting Variable Device", 
              required: true, 
              multiple: false,
              submitOnChange: true  
            )
            if (setOnIllum) {
                input (
                    name: "trackOnIllumSetting", 
                    type: "bool", 
                    title: "Track On Illumination Setting Device", 
                    required: true, 
                    defaultValue: "true"
                )
            }      
        }        

        section("<b>Solar Pool Heater Controller Device</b>") {

            input (
              name: "setDevice", 
              type: "capability.actuator", 
              title: "Select Pool Heater Controller Device", 
              required: true, 
              multiple: false,
              submitOnChange: true
            )
            if (setDevice) {
                input (
                    name: "trackController", 
                    type: "bool", 
                    title: "Track Solar Heater Device", 
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
    state.onTemp = 85.0
    state.onIllum = 30000
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(setOnTemp, "variable", tempController)
    subscribe(setOnIllum, "variable", illumController)
    subscribe(setDevice, "onIlluminance", setIlluminance)
    subscribe(setDevice, "onTemperature", setTemperature)
}

def tempController(evt) {  
    state.onTemp = evt.value.toBigDecimal()
    def ot = evt.value.toBigDecimal()

    setDevice.setOnTemperature(ot)
}

def illumController(evt) {  
    state.onIllum = evt.value.toInteger()
    def oi = evt.value.toInteger()

    setDevice.setOnIlluminance(oi)
}

def setIlluminance(evt) {
    logDebug("setIlluminance Event = $evt.value")
 
    if (state.onIllum != evt.value) {
        state.onIllum = evt.value.toInteger()
        def si = evt.value.toInteger()
        setOnIllum.setVariable(si)   
    }
}

def setTemperature(evt) {
    logDebug("setTemperature Event = $evt.value")

    if (state.onTemp != evt.value) {
        state.onTemp = evt.value.toBigDecimal()
        def st = evt.value.toBigDecimal()
        setOnTemp.setvariable(st)
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}