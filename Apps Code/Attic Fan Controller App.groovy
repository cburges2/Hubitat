/**
 *  ****************  Attic Fan Controller App ****************
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
    name: "Attic Fan Controller App",
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
        
        section("<b>Attic Fan Switch Device</b>") {
            input (
                name: "fan", 
                type: "capability.switch", 
                title: "Select Attic Fan Switch Device", 
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
    startLogTimer()
    if (settings?.debugMode) runIn(3600, logDebugOff)   // one hour
}

def initialize() {

    subscribe(outsideHumidity, "humidity", setOutsideHumidity)
    subscribe(atticHumidity, "humidity", setAtticHumidity)
    subscribe(outsideTemperature, "temperature", setOutsideTemp)
    subscribe(atticTemperature, "temperature", setAtticTemp)    
    subscribe(atticFanController, "operatingState", setSwitch)

    startLogTimer()
}

def startLogTimer() {
    runIn(60,logToGoogle)
}

def logToGoogle() {

    startLogTimer()
    def atticTemp = atticFanController.currentValue("atticTemp")
    def atticHumidity = atticFanController.currentValue("atticHumidity")
    def outsideTemp = atticFanController.currentValue("outsideTemp")
    def outsideHumidity = atticFanController.currentValue("outsideHumidity")
    def atticHumidSetpoint = atticFanController.currentValue("atticHumidSetpoint")
    def atticTempSetpoint = atticFanController.currentValue("atticTempSetpoint")
    def overrideTemp = atticFanController.currentValue("overrideTemp")
    def presence = atticFanController.currentValue("presence")

    def onTemp = 0.0
    def onHumid = 0.0
    if (presence == "humidity" || presence == "both") onHumid = atticHumidSetpoint
    if (presence == "temperature" || presence == "both") onHumid = atticTempSetpoint

    def logParams = "Attic Temp="+atticTemp+"&Attic Humid="+atticHumidity+"&Out Temp="+outsideTemp+"&Out Humid="+outsideHumidity+"&On Temp="+onTemp+"&On Humid="+onHumid+"&Humid Setpoint="+atticHumidSetpoint+"&Temp Setpoint="+atticTempSetpoint+"&Override Temp="+overrideTemp
    googleLogs.sendLog("Attic", logParams) 
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
        fanOn()
    }
    if (state == "idle") {
        fanOff()
    }
}

def fanOff() {
    state.fanSwitch = "off"
    fan.off()
}

def fanOn() {
    state.fanSwitch = "on"
    fan.on()
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