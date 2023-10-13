/**
 *  ****************  Virtual T6 Thermostat Controller App  ****************
 *
 *  Usage:
 *  This was designed to control a Honeywell T6 Pro thermostat from a virtual thermostat
 *  The sensor calibration feature of the T6 thermostat is utilized to force the physical T6 thermo to turn on and off heat. 
 *  Version 10/12/23 - Itital release
**/

definition (
    name: "Virtual T6 Thermostat Controller App",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for a Virtual Thermostat to control a T6 Physical Thermostat via the sensor calibration feature",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

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

        section("<b>External Temperature Sensor</b>") {
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

        section("<b>Switch Device with Heat On?</b>") {
            input (
              name: "selectController", 
              type: "bool", 
              title: "Use Switch for on/off", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController) {
                state.useSwitch = true
                section("<b>Switch Device</b>") {
                    input (
                        name: "heatSwitch", 
                        type: "capability.switch", 
                        title: "Select Switch Device", 
                        required: true, 
                        multiple: false
                    )
                    if (heatSwitch) {
                        input (
                        name: "trackSwitch", 
                        type: "bool", 
                        title: "Track Virtual Heat Switch", 
                        required: true, 
                        defaultValue: "true"
                        )
                    } 
                }
            } else {
                state.useSwitch = false             
            }
        }        

        section("<b>Switch Device2 with Heat On?</b>") {
            input (
              name: "selectController2", 
              type: "bool", 
              title: "Use Switch2 for on/off", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController2) {
                state.useSwitch2 = true
                section("<b>Switch Device</b>") {
                    input (
                        name: "heatSwitch2", 
                        type: "capability.switch", 
                        title: "Select Switch Device", 
                        required: true, 
                        multiple: false
                    )
                    if (heatSwitch) {
                        input (
                        name: "trackSwitch2", 
                        type: "bool", 
                        title: "Track Virtual Heat Switch2", 
                        required: true, 
                        defaultValue: "true"
                        )
                    } 
                }
            } else {
                state.useSwitch2 = false             
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
    state.virtualThemoState = "idle"
    state.physicalThermoState = "idle"
    state.virtualThemoMode = "heat"
    state.physicalThermoMode = "heat"
    state.currentPhysicalCal = "0"
    state.virtualTemp = "69.0"
    state.physicalTemp = "69.0"
    state.tempDrift = 0.51
    state.physicalHeatPoint = "69"
    state.heatSwitch = "on"
    state.heatSwitch2 = "off"
    state.switchAlreadyOn = false
    state.switch2AlreadyOn = false

    initialize()
}

def updated() {
    checkStates()
}

def initialize() {

    subscribe(physicalThermostat, "coolingSetpoint", physicalThermoCoolPointHandler)  // set virtual cooling setpoint
    subscribe(physicalThermostat, "heatingSetpoint", physicalThermoHeatPointHandler)  // set virtual heating setpoint
    subscribe(physicalThermostat, "thermostatMode", physicalThermoModeHandler) // set virtual mode
    subscribe(physicalThermostat, "thermostatOperatingState", physicalStateHandler)
    subscribe(physicalThermostat, "temperature", physicalTempHandler)
    subscribe(physicalThermostat, "currentSensorCal", setPhysicalCalibration)
    
    subscribe(virtualThermostat, "heatingSetpoint", virtualThermoHeatPointHandler)  // set virtual heating setpoint
    subscribe(virtualThermostat, "temperature", virtualTempHandler) 
    subscribe(virtualThermostat, "thermostatMode", physicalThermoModeHandler) 
    subscribe(virtualThermostat, "thermostatOperatingState", virtualStateHandler)

    if (state?.useSwitch) subscribe(heatSwitch, "switch", switchHandler)
    if (state?.useSwitch2) subscribe(heatSwitch2, "switch", switch2Handler)

    subscribe(tempSensor, "temperature", externalTempHandler)

    runIn(1,checkStates)
}

def startCheckTimer() {
    runIn(60, checkStates)
}

// check states every minute by Check Timer
def checkStates() {

    //logDebug("checkStates() is running")
    
    def physicalMode = state.physicalThermoMode
    def virtualMode = state?.virtualThermoMode
    def physicalState = state?.physicalThermoState
    def virtualState = state?.virtualThermoState
    Float virtualTemp = new Float(state?.virtualTemp)
    Float physicalTemp = new Float(state?.physicalTemp)
    Float physicalHP = new Float(state?.physicalHeatPoint)

    // Fix physical thermo state to match virtual state
    if (physicalState != "heating" && virtualState == "heating" && physicalMode == "heat") {
        sensorCalHandler(-1)
        logDebug("Thermostat not heating")
    }
    if (physicalState == "heating" && virtualState != "heating" && physicalMode == "heat") {
        sensorCalHandler(1)
        logDebug("Thermostat heating when it should not be")
    }

    //logDebug("checkStates() fix thermo state to match completed")

    // fix thermostat displaying wrong temp due to over/under calibration
    if (virtualState != "heating") {
        if (physicalMode == "cool" || physicalMode == "heat") {
            double virtTemp = Double.valueOf(state?.virtualTemp)
            def tempInt = Math.round(virtTemp)
            //logDebug("tempInt is ${tempInt}")
            
            def pTemp = physicalTemp.intValue()
            //logDebug("pTemp is ${pTemp}")

            if (pTemp > tempInt) {
                //logDebug("Physical Temp greater than Virtual Temp")
                sensorCalHandler(-1)
            } else if (pTemp < tempInt) {
                //logDebug("Physical Temp less than Virtual Temp")
                sensorCalHandler(1)
            }
        }  
    }
    logDebug("checkStates() completed")

    startCheckTimer() // repeat timer
}

// virtual operating state change - update state and push physical cal
def virtualStateHandler(evt) {

    logDebug("Virtual State Change Event = ${evt.value}")
    def virtualState = evt.value
    //def physicalState = state?.physicalThermoState

    // check if changed (not equal to current state)
    if (state?.virtualThermoState != virtualState) {
        if (virtualState == "heating") {
            logDebug("Virtual Started Heating")
            sensorCalHandler(-1)      // set calibration lower physical temp
            physicalThermostat.IdleBrightness(5)
            if (state?.useSwitch) {
                if (state?.heatSwitch == "on") state.switchAlreadyOn = true
                else state.switchAlreadyOn = false
                if (state?.useSwitch && !state?.switchAlreadyOn) heatSwitch.on()
            }
            if (state?.useSwitch2) {
                if (state?.heatSwitch2 == "on") state.switch2AlreadyOn = true
                else state.switch2AlreadyOn = false
                if (state?.useSwitch2 && !state?.switch2AlreadyOn) heatSwitch2.on()  
            }          
        }
        if (virtualState == "idle" || virtualState == "fan only") {
            logDebug("Virtual Finished Heating")
            sensorCalHandler(2)      // set calibration to increase physical temp
            physicalThermostat.IdleBrightness(2)
            if (state?.useSwitch && !state?.switchAlreadyOn) heatSwitch.off()
            if (state?.useSwitch2 && !state?.switch2AlreadyOn) heatSwitch2.off()
        }        
    }
    state.virtualThermoState = evt.value
    
    runIn(5, checkStates)
}

// Physical operating state change - update state
def physicalStateHandler(evt) {
    logDebug("Physical State Change Event = ${evt.value}")
    def physicalState = evt.value
    def virtualState = state?.virtualThermoState
  
    // check if changed (not equal to current state)
    if (state?.physicalThermoState != physicalState) {    // check if changed
        if (physicalState == "heating" && virtualState != "heating") {
            sensorCalHandler(1)      // set calibration to decrease 
        }
        if ((physicalState == "idle" || physicalState == "fan only") && virtualState == "heating") {
            sensorCalHandler(-1)      // set calibration to increase 
        }        
    }
    state.physicalThermoState = evt.value
}

def switchHandler(evt) {
    logDebug("Switch Handler Event = ${evt.value}")
    state.heatSwitch = evt.value
}

def switch2Handler(evt) {
    logDebug("Switch Handler Event = ${evt.value}")
    state.heatSwitch2 = evt.value
}

def externalTempHandler(evt) {
    logDebug("External Temp Change Event = ${evt.value}")
    virtualThermostat.setTemperature(evt.value)
    state.virtualTemp = evt.value
    runIn(5, checkStates)
}

/*
def virtualTempHandler(evt) {
    logDebug("Virtual Temp Handler Event = ${evt.value}")
    state.virtualTemp = evt.value
    runIn(5, checkStates)
}*/

def physicalTempHandler(evt) {
    logDebug("Physical Temp Handler Event = ${evt.value}")
    state.physicalTemp = evt.value
    runIn(5, checkStates)
}

def physicalThermoHeatPointHandler(evt) {
    logDebug("Physical Heat Point Handler Event = ${evt.value}")
    state.physicalHeatPoint = evt.value 
    virtualThermostat.setHeatingSetpoint(evt.value)  
    runIn(5, checkStates)
}

def physicalThermoCoolPointHandler(evt) {
    logDebug("Physical Cool Point Handler Event = ${evt.value}")
    virtualThermostat.setCoolingSetpoint(evt.value) 
}

def virtualThermoHeatPointHandler(evt) {
    logDebug("Virtual Heat Point Handler Event = ${evt.value}")
    state.virtualHeatPoint = evt.value    
    runIn(5, checkStates)
}

// switch virtual thermo to match physical 
def phyicalModeHandler(evt) {
    logDebug("Physical Mode Change Event = ${evt.value}")
    physicalMode = evt.value

    if (physicalMode != state?.virtualThermoMode) {
         virtualThermostat.setThermostatMode(physicalMode)
    }
    state.physicalThermoMode = evt.value
}

// update state of virtual thermo
def virutalModeHandler(evt) {
    logDebug("Virtual Mode Change Event = ${evt.value}")
    state.virtualThermoMode = evt.value
}

// update sensor calibration state from attribute controller
def setPhysicalCalibration(evt) {
    logDebug("Calibration Change Event = ${evt.value}")
    state.currentPhysicalCal = evt.value
}

// Send sensor calibatarion to physical thermostat
def sensorCalHandler(int change) {
    //logDebug("Sensor Cal Change Request of ${change}")

    def currentCal = state?.currentPhysicalCal as Integer
    def newCal = currentCal + change

    //logDebug("newCal is ${newCal}")

    if (newCal > 3) newCal = 3
    if (newCal < -3) newCal = -3

    logDebug("New Sensor Cal is ${newCal}")

    if (currentCal != newCal) physicalThermostat.SensorCal(newCal)
    state.currentPhysicalCal = newCal
}

def logDebug(txt){
    def deviceName = physicalThermostat.getLabel()
    try {
        if (settings?.debugMode) log.debug("T6 Controller for ${deviceName} - ${txt}") 
    } catch(ex) {
        log.error("bad debug message")
    }
}