/**
 *  **************** Set Attic Fan Controller Attributes  ****************
 *
 *  Usage:
 *  Set a device attribute using a dashboard variable tile
 *    
 *  v.1.0 5/3/23  Initial code
 *  

**/

definition (
    name: "Set Attic Fan Controller Attributes",
    namespace: "Hubitat",
    author: "cburgess",
    description: "Set Attic Controller Device Attributes from dashboard variable connectors",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Humidity Setting Variable Device</b>") {

            input (
              name: "setHumidity", 
              type: "capability.actuator", 
              title: "Select Humidity Setting Variable Device", 
              required: true, 
              multiple: false,
              submitOnChange: true  
            )
            if (setHumidity) {
                input (
                    name: "trackHumiditySetting", 
                    type: "bool", 
                    title: "Track Humidity Setting Device", 
                    required: true, 
                    defaultValue: "true"
                )
            }      
        }

        section("<b>Temperature Setting Variable Device</b>") {

            input (
              name: "setTemperature", 
              type: "capability.actuator", 
              title: "Select Temperature Setting Variable Device", 
              required: true, 
              multiple: false,
              submitOnChange: true  
            )
            if (setTemperature) {
                input (
                    name: "trackTemperatureSetting", 
                    type: "bool", 
                    title: "Track Temperature Setting Device", 
                    required: true, 
                    defaultValue: "true"
                )
            }      
        }        

        section("<b>Attic Fan Controller Device</b>") {

            input (
              name: "setDevice", 
              type: "capability.actuator", 
              title: "Select Attic Fan Controller Device", 
              required: true, 
              multiple: false,
              submitOnChange: true
            )
            if (setDevice) {
                input (
                    name: "trackController", 
                    type: "bool", 
                    title: "Track Attic Fan Controller Device", 
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
    state.water = "off"
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(setHumidity, "variable", humidityController)
    subscribe(setTemperature, "variable", temperatureController)
    subscribe(setDevice, "atticTempSetpoint", setTemperature)
    subscribe(setDevice, "atticHumidSetpoint ", setHumidity)
}

def humidityController(evt) {  
    state.humidSetting = evt.value
    def hs = evt.value.toInteger()

    setDevice.setAtticHumidSetpoint(hs)
}

def temperatureController(evt) {  
    state.tempSetting = evt.value
    def ts = evt.value.toBigDecimal()

    setDevice.setAtticTempSetpoint(ts)
}

def setHumidity(evt) {
    logDebug("setIHumidity Event = $evt.value")
 
    if (state.humidSetting != evt.value) {
        state.humidSetting = evt.value.toInteger()
        def sh = evt.value.toInteger()
        setHumidity.setVariable(sh)   
    }
}

def setTemperature(evt) {
    logDebug("setTemperature Event = $evt.value")

    if (state.tempSetting != evt.value) {
        state.tempSetting = evt.value.toBigDecimal()
        def st = evt.value.toBigDecimal()
        setTemperature.setvariable(st)
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}