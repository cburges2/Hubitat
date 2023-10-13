/**
 *  ****************  Virtual Thermostat Sync Echo Speaks  ****************
 *
 *  Usage:
 *  This was designed to sync a virtual thermostat with a physical thermostat and sync temp from external sensor
*  Also will flip a switch OR send an Echo Speaks Text command  to turn on and off the AC
 *
**/

definition (
    name: "Virtual Thermostat Sync Echo Speaks",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for a Virtual Thermostat to sync with a Physical Thermostat and a Temp Sensor",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {
lock
    dynamicPage(name: "mainPage") {

        section("<b>Virtual Thermostat Device</b>") {

            input (
              name: "virtualThermostat", 
              type: "capability.thermostat", 
              title: "Select Virtual Thermostat Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )

            if (virtualThermostat) {
                input (
                    name: "trackVirtual", 
                    type: "bool", 
                    title: "Track Virtual Thermo Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }

        section("<b>Physical Thermostat Device</b>") {
            input (
                name: "physicalThermostat", 
                type: "capability.thermostat", 
                title: "Select Physical Thermostat Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (physicalThermostat) {
                input (
                    name: "trackPhysical", 
                    type: "bool", 
                    title: "Track Physical Thermo Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }

        section("<b>Temperature Sensor</b>") {
            input (
                name: "tempSensor", 
                type: "capability.temperatureMeasurement", 
                title: "Select Physical Temp Sensor", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )
            if (tempSensor) {
                input (
                    name: "trackTemperature", 
                    type: "bool", 
                    title: "Track Sensor Temp Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }   

        section("<b>Use AC Switch Device </b>") {
            input (
              name: "selectController", 
              type: "bool", 
              title: "Use Switch for AC on/off", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController) {
                state.useSwitch = true
                section("<b>AC Switch Device</b>") {
                    input (
                        name: "coolSwitch", 
                        type: "capability.switch", 
                        title: "Select AC Switch Device", 
                        required: true, 
                        multiple: false
                    )
                }
            } else {
                state.useSwitch = false             
            }
        }

        section("<b>Use Echo Speaks Device</b>") {
            input (
              name: "selectEcho", 
              type: "bool", 
              title: "Use Echo Speaks Commands for AC on/off", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectEcho) {
                state.useEcho = true
                section("<b>Echo Device</b>") {
                    input (
                        name: "echoDevice", 
                        type: "capability.speechRecognition", 
                        title: "Select Echo Device", 
                        required: true, 
                        multiple: false
                    )
                }
                section("<b>Echo Speaks On Command</b>") {
                    input (
                    name: "esOn", 
                    type: "String", 
                    title: "Enter On Command", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true               
                    )
                }        
                state.echoOn = esOn

                section("<b>Echo Speaks Off Command</b>") {
                    input (
                    name: "esOff", 
                    type: "String", 
                    title: "Enter Off Command", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true               
                    )
                }  
                state.echoOff = esOff 
            } else {
                state.useEcho = false             
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

    subscribe(physicalThermostat, "coolingSetpoint", physicalThermoCoolPointHandler) // set virtual from physical
    subscribe(physicalThermostat, "heatingSetpoint", physicalThermoHeatPointHandler) // set virtual from physical
    subscribe(physicalThermostat, "thermostatMode", physicalThermoModeHandler) // set virtual from physical
    subscribe(virtualThermostat, "thermostatOperatingState", virtualStateHandler)
    subscribe(tempSensor, "temperature", tempSensorTempHandler) // set virtual from sensor

}

def physicalThermoCoolPointHandler(evt) {

    state.physicalCoolPoint = evt.value
    logDebug("Physical Cool Point Event = $state.physicalCoolPoint")
    def lvl = evt.value.toInteger()

    virtualThermostat.setCoolingSetpoint(lvl)
}

def physicalThermoHeatPointHandler(evt) {

    state.physicalHeatPoint = evt.value
    logDebug("Physical Heat Point Event = $state.physicalHeatPoint")
    def lvl = evt.value.toInteger()

    virtualThermostat.setHeatingSetpoint(lvl)   
}

def physicalThermoModeHandler(evt) {

    state.physicalMode = evt.value
    logDebug("Physical Mode Change Event = $state.physicalMode")
    def mode = evt.value

    virtualThermostat.setThermostatMode(mode)   
}

def tempSensorTempHandler(evt) {

    state.sensorTemp = evt.value
    logDebug("Sensor Temp Change Event = $state.sensorTemp")
    def temp = evt.value

    virtualThermostat.setTemperature(temp)

}

def virtualStateHandler(evt) {
    def thermoState = evt.value.toString()
    logDebug("State Change Event = $thermoState")

        if (thermoState == "cooling") {
            logDebug("State Change is $thermoState")
            state.acState = "on"
            if (state.useSwitch) {
                coolSwitch.on()
            } 
            if (state.useEcho) {
                echoDevice.voiceCmdAsText(state.echoOn)
                runIn(1,on)
            }
        }
        if (thermoState == "idle" || thermoState =="fan") {
            logDebug("State Change is $thermoState")
            state.acState = "off"
            if (state.useSwitch ) {
                coolSwitch.off()
            }
            if (state.useEcho) {
                echoDevice.voiceCmdAsText(state.echoOff)
                runIn(1,off)
            }
        }

}

def off() {
    echoDevice.voiceCmdAsText(state.echoOff)
}

def on() {
    echoDevice.voiceCmdAsText(state.echoOn)
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}