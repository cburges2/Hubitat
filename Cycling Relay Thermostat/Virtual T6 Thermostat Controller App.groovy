/**
 *  ****************  Virtual T6 Thermostat Controller App  ****************
 *
 *  Usage:
 *  This was designed to control a Honeywell T6 Pro thermostat from a virtual thermostat
 *  The sensor calibration feature of the T6 thermostat is utilized to force the physical T6 thermo to turn on and off heat. 
 *  Version 10/12/23 - Itital release
 *  Version 12/29/23 - Multiple External Temp Sensors based on motion
 *  Version 1/21/24 - Option to use relays to turn on heat at furnace, instead of pushing cal on the T6 Thermos. 
 *                    An additonal relay also switches between thermo control and relay control of the zone valve relays. 
 *  Version 1/24/24 - heatOnDevice is now a Pre-heat switch, so the turn-off delay is now from start of pre-heat instead of from heat off. 
 *  Versoin 2/23/24 - heatOnDevice is now a circulator fan to work with cycles rising temps.  Device with heat on turns on when heating, off when Idle
**/

definition (
    name: "Virtual T6 Thermostat Controller App",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for a Virtual Thermostat to control a T6 Physical Thermostat via the sensor calibration feature or ZigBee relays",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("App Name") {
            label title: "Optionally assign a custom name for this app", required: false
        }   
        
        section("<b>Virtual Thermostat Device</b>") {

            input (
              name: "virtualThermostat", 
              type: "capability.thermostat", 
              title: "Select Virtual Thermostat Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
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
        } 

        section("<b>External Temperature Sensor 2</b>") {
            input (
                name: "tempSensor2", 
                type: "capability.temperatureMeasurement", 
                title: "Select External Temp Sensor 2", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )
        }         

        section("<b>External Temperature Sensor 3</b>") {
            input (
                name: "tempSensor3", 
                type: "capability.temperatureMeasurement", 
                title: "Select External Temp Sensor 3", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )
        }         

        section("<b>Zone Valve Relay Device</b>") {
            input (
                name: "heatRelay", 
                type: "capability.switch",
                title: "Select Zone Valve Relay Switch Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )        
        } 

        section("<b>Bedroom AC Buttons</b>") {
            input (
                name: "acButtons", 
                type: "capability.pushableButton",
                title: "Select Bedroom AC Buttons Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )        
        } 

        section("<b>Living Room AC Switch</b>") {
            input (
                name: "acSwitch", 
                type: "capability.switch",
                title: "Select Living Room AC Switch Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )        
        }

        section("<b>Living Room AC Fan Switch</b>") {
            input (
                name: "acFanSwitch", 
                type: "capability.switch",
                title: "Select Living Room AC Fan Switch Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )        
        }

        section("<b>Living Room AC Status</b>") {
            input (
                name: "acStatusSwitch", 
                type: "capability.switch",
                title: "Select Living Room AC Status Switch Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )        
        }

        section("<b>Living Room AC Fan Status</b>") {
            input (
                name: "acFanStatusSwitch", 
                type: "capability.switch",
                title: "Select Living Room AC Fan Status Switch Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )        
        }

        section("<b>Living Room AC Fan Button</b>") {
            input (
                name: "acFan", 
                type: "capability.pushableButton",
                title: "Select Living Room AC Fan Button Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )        
        }        

        section("<b>Outdoor Temperature Sensor</b>") {
            input (
                name: "outdoorSensor", 
                type: "capability.temperatureMeasurement", 
                title: "Select Outdoor Temp Sensor", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )
        }    

        section("<b>Engage Heat Relays Device</b>") {
            input (
                name: "engageRelays", 
                type: "capability.switch",
                title: "Select Engage Heat Relays Switch Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
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

        section("<b>Use a Fan Device to Circulate Heat?</b>") {
            input (
              name: "selectController", 
              type: "bool", 
              title: "Use Fan Device to Circulate Heat", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController) {
                state.useFan = true
                section("<b>Switch Device</b>") {
                    input (
                        name: "fanSwitch", 
                        type: "capability.switch", 
                        title: "Select Switch Device", 
                        required: true, 
                        multiple: false
                    )
                }
            } else {
                state.useFan = false             
            }
        }        

        section("<b>Sync External Humidity Sensor?</b>") {
            input (
              name: "selectHumidity", 
              type: "bool", 
              title: "Sync External Humidity Sensor", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectHumidity) {
                state.useHumidity = true
                section("<b>Humidity Device</b>") {
                    input (
                        name: "humiditySensor", 
                        type: "capability.relativeHumidityMeasurement", 
                        title: "Select Humidity Sensor Device", 
                        required: true, 
                        multiple: false
                    )
                }
            } else {
                state.useHumidity = false             
            }
        }   

        section("<b>Switch On a Device when Heat On?</b>") {
            input (
              name: "selectController2", 
              type: "bool", 
              title: "Use Switch Device when Heat On", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController2) {
                state.useHeatSwitch = true
                section("<b>Switch Device</b>") {
                    input (
                        name: "heatOnDevice", 
                        type: "capability.switch", 
                        title: "Select On with Heat Switch Device", 
                        required: true, 
                        multiple: false
                    )
                }
            } else {
                state.useHeatSwitch = false             
            }
        }    

        section("") {
            input (
                name: "googleLogging", 
                type: "bool", 
                title: "Enable logging to Google Sheets", 
                required: true, 
                defaultValue: false
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

    state.tempDrift = 0.51
    state.externalSensor = "temp1"
    initialize()
}

def updated() {

    if (settings?.debugMode) runIn(3600, logDebugOff)   // one hour
    state.tempDrift = 0.51
    //state.tempState = "falling"
    unsubscribe()
    unschedule()

    state.fanState = "off"

    initialize()
}

def initialize() {

    // sync thermostats
    subscribe(physicalThermostat, "coolingSetpoint", physicalThermoCoolPointHandler)  // set virtual cooling setpoint   
    subscribe(physicalThermostat, "heatingSetpoint", physicalThermoHeatPointHandler)  // set virtual heating setpoint
    subscribe(physicalThermostat, "thermostatMode", physicalModeHandler)              // set virtual mode
    subscribe(physicalThermostat, "thermostatFanMode", physicalFanModeHandler)        // set virtual fan mode
    subscribe(physicalThermostat, "thermostatOperatingState", physicalStateHandler)

    subscribe(physicalThermostat, "coolingSetpoint", virtualThermoCoolPointHandler)  // set virtual cooling setpoint 
    subscribe(virtualThermostat, "heatingSetpoint", virtualThermoHeatPointHandler)    // set virtual heating setpoint
    subscribe(virtualThermostat, "thermostatOperatingState", virtualStateHandler)
    subscribe(virtualThermostat, "thermostatMode", virtualModeHandler)              // set virtual mode
    subscribe(virtualThermostat, "thermostatFanMode", virtualFanModeHandler)
    //subscribe(virtualThermostat, "cycleSeconds", cycleSecondsHandler)
    //subscribe(virtualThermostat, "waitSeconds", waitSecondsHandler)
    subscribe(virtualThermostat, "temperatureState", temperatureStateHandler)
    subscribe(virtualThermostat, "cycleState", cycleStateHandler)
    
    subscribe(virtualThermostat, "fanSpeed", fanSpeedHandler)

    state.physicalThermoState = physicalThermostat.currentValue("thermostatOperatingState")

    subscribe(outdoorSensor, "temperature", outdoorTempHandler)

    if (state?.useFan) subscribe(fanSwitch, "switch", fanSwitchHandler)
    if (state?.useHeatSwitch) subscribe(heatOnDevice, "switch", heatSwitchHandler)

    //if (asSwitch) subscribe(acSwitch, "switch", fanSwitchHandler)

    // sync the external temp sensor to thermostat
    subscribe(tempSensor, "temperature", externalTempHandler)
    if (tempSensor2) {subscribe(tempSensor2, "temperature", externalTempHandler2)}
    if (tempSensor2) {subscribe(tempSensor2, "motion", externalMotionHandler2)}
    if (tempSensor3) {subscribe(tempSensor3, "temperature", externalTempHandler3)}
    if (tempSensor3) {subscribe(tempSensor3, "motion", externalMotionHandler3)}
    
    // sync humidity sensor
    if (state?.useHumidity) {
        subscribe(humiditySensor, "humidity", externalHumidityHandler)
        subscribe(physicalThermostat, "humidity", physicalHumidityHandler)
    }

    state.lastOperatingState = physicalThermostat.currentValue("thermostatOperatingState")

    state.fanState = "off"

    runIn(1,checkStates)
}

def fanSpeedHandler(evt) {

    state?.fanSpeed = evt.value

}

// fan mode handler
def virtualFanModeHandler(evt) {
    logDebug("Fan Mode Changed to ${evt.value}")
    def cycleState = virtualThermostat.currentValue("cycleState")
    def mode = evt.value

    if (mode == "auto") {setCycleStateFan(cycleState)}
    if (mode == "circulate") {mode = "on"}
    if (mode == "on") {
        if (physicalThermostat.getLabel() == "Living Room Thermostat") {
            fanSwitchOn()
        }
        if (physicalThermostat.getLabel() == "Bedroom Thermostat") {
            //acSwitchOn()
        }        
    }
}

// Turn on fan switch with Wait Cycle or Ramping
def temperatureStateHandler(evt) {
    logDebug("temperatureStateHandler called with ${evt.value}")
    state.tempState = evt.value.toString()
    def mode = virtualThermostat.currentValue("thermostatMode")
    def fanMode = virtualThermostat.currentValue("thermostatFanMode")

    if (state?.useFan && fanMode == "auto") {
/*         if (evt.value == "rising") {
            logDebug("Turning on the Fan Rising")
            runIn(10,fanSwitchOn)
        }*/
        if (evt.value == "falling" && state?.cycleState != "Waiting" && mode == "heat") {
            logDebug("Turning Off the Fan falling")
            runIn(60,fanSwitchOff)
        }       
    } 
}

def cycleStateHandler(evt) {
    logDebug("cycleStateHandler called with ${evt.value}")
    state.cycleState = evt.value
    def fanMode = virtualThermostat.currentValue("thermostatFanMode")
    def mode = virtualThermostat.currentValue("thermostatMode")

    if (state?.useFan && fanMode == "auto" && mode == "heat") {
        setCycleStateFan(evt.value)          
    }
}

def setCycleStateFan(state) {
    def tempState = virtualThermostat.currentValue("temperatureState")

    if (state == "Waiting") {
        logDebug("Turning on the Fan Waiting")
        runIn(10,fanSwitchOn)
    }
    if (state == "Idle" && tempState != "rising") {
        logDebug("Turning Off the Fan Idle")
        runIn(60,fanSwitchOff)
    }   
    if (state == "Ramping") {
        logDebug("Turning on the Fan Ramping")
        runIn(60,fanSwitchOn)
    }       

}

def fanSwitchOn() {
    state.onByThermo1 = true
    if (!state?.switchAlreadyOn) fanSwitch.on()
}

def fanSwitchOff() {
    if (!state?.switchAlreadyOn) fanSwitch.off()
}

def heatSwitchOn() {
    // state.onByThermo2 = true
    heatOnDevice.on()          
}

def heatSwitchOff() {
    heatOnDevice.off()
}

def cycleSecondsHandler(evt) {
    logToGoogle()
}

def waitSecondsHandler(evt) {
    logToGoogle()
}

/* def virtualvirtualFanModeHandler(evt) {
    def fanMode = evt.value
    
    if (fanMode == "auto") {state.fanMode = fanMode}
    if (fanMode == "fan") {state.fanMode = fanMode}
    if (fanMode == "circulate") {state.fanMode = fanMode}
} */

def externalMotionHandler(evt) {
}

def restartCheckTimer() {
    runIn(60, checkStates)
}

def fanSwitchHandler(evt) {
    logDebug("Fan Switch Handler -Fan Swtich ${evt.value}")
    def waiting = virtualThermostat.currentValue("cycleState") == "Waiting"

    if (evt.value ==  "on" && !waiting) {
        state.fanAlreadyOn = true
    }
    if (evt.value == "off" && !waiting) {
        state.fanAlreadyOn = false
    }
}

def heatSwitchHandler(evt) {
    logDebug("Heat Switch Handler - Heat Swtich ${evt.value}")
    def heating = virtualThermostat.currentValue("thermostatOperatingState") == "heating"

    if (evt.value == "on" && heating) {
        state.heatSwitchAlreadyOn = true
    }
    if (evt.value == "off" && !heating) {
        state.heatSwitchAlreadyOn = false
    }
}

def outdoorTempHandler(evt) {
    virtualThermostat.setOutsideTemp(evt.value)
}

// check states every minute by Check Timer
def checkStates() {

    //logDebug("checkStates() is running")
    
    def physicalMode = physicalThermostat.currentValue("thermostatMode") 
    def virtualMode = virtualThermostat.currentValue("thermostatMode")  
    def virtualState = virtualThermostat.currentValue("thermostatOperatingState") 
    Float virtualTemp = new Float(virtualThermostat.currentValue("temperature"))
    Float physicalTemp = new Float(physicalThermostat.currentValue("temperature"))  
    logDebug("Virtual Mode is ${virtualMode}")

    if (virtualMode == "heat") {
        if (physicalMode != "heat") {physicalThermostat.setThermostatMode("heat")}
        logDebug("Checking Heat")
        def physicalState = physicalThermostat.currentValue("thermostatOperatingState") 
        def relaysDisengaged = false
        if (engageRelays) {relaysDisengaged = engageRelays.currentValue("switch") == "off"}

        if (physicalThermostat.getLabel() == "Bedroom Thermostat") {
            def acState = virtualThermostat.currentValue("presence")
            if (acState == "cool") {fanSwitch.setFan()}  // change to fan
        }
        Float physicalHP = new Float(physicalThermostat.currentValue("heatingSetpoint"))

        if (relaysDisengaged) {       
            // Fix physical thermo state to match virtual state
            if (physicalState != "heating" && virtualState == "heating" && physicalMode == "heat") {
                sensorCalHandler(-1)
                logDebug("Thermostat not heating")
            }
            if (physicalState == "heating" && virtualState != "heating" && physicalMode == "heat") {
                sensorCalHandler(1)
                logDebug("Thermostat heating when it should not be")
            }
        } else if (virtualMode == "heat") {    // relays engaged, check if heat valve turned off. 
            def relayState = heatRelay.currentValue("switch")
            if (virtualState == "heating" && relayState == "off") {heatRelay.on()}
            if ((virtualState == "idle" || virtualState == "fan") && relayState == "on") {heatRelay.off()}
        }

        //logDebug("checkStates() fix thermo state to match completed")

        // fix thermostat displaying wrong temp due to over/under calibration - only when not heating, unless relays engaged.
        if (virtualState != "heating" || !relaysDisengaged) {
            if (virtualMode == "cool" || physicalMode == "heat") {
                virtTemp = Double.valueOf(virtualTemp)
                def tempInt = Math.round(virtTemp)
                logDebug("tempInt is ${tempInt}")
                
                def pTemp = physicalTemp.intValue()
                logDebug("pTemp is ${pTemp}")
        
                if (pTemp > tempInt) {
                    logDebug("Making Physical read higher temp")
                    sensorCalHandler(-1)
                } else if (pTemp < tempInt) {
                    logDebug("Making Physical read lower temp")
                    sensorCalHandler(1)
                }
            }  
        }        
    }
    if (virtualMode == "cool") {
        logDebug("Virtual Mode is ${virtualMode}")
        if (physicalMode != "cool") {physicalThermostat.setThermostatMode("cool")}
        if (heatRelay.currentValue("switch") == "on") {heatRelay.off()}  // confirm a valve not left on from heating cycle
        // Bedroom
        if (physicalThermostat.getLabel() == "Bedroom Thermostat") {
            def acState = virtualThermostat.currentValue("presence")
            if (virtualState == "cooling" && acState != "cool") {fanSwitch.setCool()} // cool
            else if (virtualState != "cooling" && acState == "cool") {fanSwitch.setFan()} // fan
        }
        // Living Room
        if (physicalThermostat.getLabel() == "Living Room Thermostat") {
            logDebug("Checking Living Room Thermostat State")
            def acState = acStatusSwitch.currentValue("switch") 
            def acFanState = acFanStatusSwitch.currentValue("switch")
            def switchState = acSwitch.currentValue("switch")
            logDebug("${acState},${acFanState},${switchState}")
            if (virtualState == "cooling" && acState == "off") {
                logDebug("Turning AC Switch On")
                acSwitch.on()
            } 
            else if (virtualState != "cooling" && (switchState == "on" || acState == "on") && acFanState == "off") {  //
                logDebug("Turning AC Switch Off")
                acSwitch.off()              // totally off                          
            }             
        }

        // match thermo display to virtual temp
        if (heatRelay.currentValue("switch") == "on") {heatRelay.off()}

        logDebug("Checking Cool")
        virtTemp = Double.valueOf(virtualTemp)
        def tempInt = Math.round(virtTemp)
        logDebug("tempInt is ${tempInt}")
        
        def pAdjusted = physicalTemp + 1.1  // zwave mode themo reads higher than reported zwave temp
        def pTemp = pAdjusted.intValue()  
        logDebug("pTemp is ${pTemp}")

        if (pTemp > tempInt) {
            logDebug("Making Physical read higher temp")
            sensorCalHandler(-1)
        } else if (pTemp < tempInt) {
            logDebug("Making Physical read lower temp")
            sensorCalHandler(1)
        }
    }  

    // refresh temp sensors - living room cannot be refreshed (ecowitt sensor)
    if (physicalThermostat.getLabel() != "Living Room Thermostat") {tempSensor.refresh()}
    if (tempSensor2 && physicalThermostat.getLabel() != "Living Room Thermostat") {tempSensor2.refresh()}
    if (tempSensor3) {tempSensor3.refresh()}    
    logDebug("checkStates() completed")

    restartCheckTimer() // repeat timer
}

def acSwitchOff() {
    acSwitch.off()
    acFanSwitch.off()
    acFanStatusSwitch.off()
    acStatusSwitch.off()
}

// virtual operating state changed - so update state and push physical cal or flip relay if engaged
def virtualStateHandler(evt) {

    logDebug("Virtual State Change Event = ${evt.value}")
    def virtualState = evt.value

    def operatingBrightness = virtualThermostat.currentValue("operatingBrightness")
    def idleBrightness = virtualThermostat.currentValue("idleBrightness")
    def relaysDisengaged = true
    if (engageRelays) {relaysDisengaged = engageRelays.currentValue("switch") == "off"}
    logDebug("Relays Disengaged = ${relaysDisengaged}")
    def thermostatMode = virtualThermostat.currentValue("thermostatMode")
    def thermo = physicalThermostat.getLabel()

    // check if changed (not equal to current state)
    if (state?.virtualThermoState != virtualState) {
        if (thermostatMode == "heat") {

            if (virtualState == "heating") {
                state.onByThermo1 = true
                logDebug("Virtual Started Heating")

                // use relays if engaged, else lower cal to turn on physical
                if (relaysDisengaged) {
                    sensorCalHandler(-1)    // set calibration to lower physical temp
                } else {
                    heatRelay.on()          // Turn on Relay       
                    runIn(1, confirmOn)
                }
                
                // Turn on other devices' switch with heat (with delay)
                physicalThermostat.IdleBrightness(operatingBrightness)

                if (state?.useHeatSwitch) {               
                    runIn(1, heatSwitchOn)
                }          
            }
            if (virtualState == "idle" || virtualState == "fan only") {
                logDebug("Virtual Finished Heating")

                // use relays if engaged, else raise cal to turn off physical
                if (relaysDisengaged) {
                    sensorCalHandler(2)      // set calibration to increase physical temp
                } else {
                    heatRelay.off()         // turn off relay
                    runIn(1,confirmOff)
                }

                // Turn off other devices switch with heat off (with delay)
                physicalThermostat.IdleBrightness(idleBrightness)

                if (state?.useHeatSwitch) {           
                    runIn(1, heatSwitchOff)
                } 
            }
        }
        if (thermostatMode == "cool") {
            if (virtualState == "cooling") {
                state.onByThermo1 = true
                logDebug("Virtual Started Cooling")
                
                // Turn on other devices' switch with heat (with delay)
                physicalThermostat.IdleBrightness(operatingBrightness)

                // Bedroom
                if (thermo == "Bedroom Thermostat") {
                    def acState = virtualThermostat.currentValue("presence")
                    if (acState != "cool") {fanSwitch.setCool()} // cool
                }   
                // Living Room
                if (thermo == "Living Room Thermostat")  {
                    def acState = acStatusSwitch.currentValue("switch")                    
                    if (acState == "off") {
                        acSwitch.on()
                        unschedule("acSwitchOff")
                    }
                }                
            }
           
            if (virtualState == "idle" || virtualState == "fan only") {
                logDebug("Virtual Finished Cooling")
             
                physicalThermostat.IdleBrightness(idleBrightness) 

                if (thermo == "Bedroom Thermostat") {
                    def acState = virtualThermostat.currentValue("presence")
                    def acFanState = acFanStatusSwitch.currentValue("switch")
                    if (acState != "fan") {fanSwitch.setFan()} // fan
                }
                // Living Room
                if (thermo == "Living Room Thermostat") {
                    def acState = acStatusSwitch.currentValue("switch") 
                    def acFanState = acFanStatusSwitch.currentValue("switch")
                    if (acState == "on" && acFanState == "off") {                   
                        acFanSwitch.on()               // fan for ramp down
                        runIn(180,acSwitchOff)         // totally off in 3 min
                    }
                }                       
            }
        }

    }
    state.virtualThermoState = evt.value  // for changed check
    
    runIn(5, checkStates)
}

def confirmOn() {
    heatRelay.on()
}

def confirmOff() {
    heatRelay.off()
}

// Physical operating state change - update state
def physicalStateHandler(evt) {
    logDebug("Physical State Change Event = ${evt.value}")
    def relaysDisengaged = true
    if (engageRelays) {relaysDisengaged = engageRelays.currentValue("switch") == "off"}

    // Check if we need to adjust physical cal if relays disengaged
    if (relaysDisengaged) {
        def physicalState = evt.value
        def virtualState = virtualThermostat.currentValue("thermostatOperatingState")
    
        // check if changed (not equal to current state)
        if (state?.physicalThermoState != physicalState) {    // check if changed
            if (physicalState == "heating" && virtualState != "heating") {
                sensorCalHandler(1)      // set calibration to decrease 
            }
            if ((physicalState == "idle" || physicalState == "fan only") && virtualState == "heating") {
                sensorCalHandler(-1)      // set calibration to increase 
            }        
        }
    }
    state.physicalThermoState = evt.value   // for checking changed state
    runIn(1,logToGoogle)
}

// run thermostat from external sensor 2
def externalMotionHandler2(evt) {
    logDebug("External 2 Motion Event = ${evt.value}")

    if (evt.value == "active") {
        state.externalSensor = "temp2"
        def temp = tempSensor2.currentValue("temperature")
        virtualThermostat.setTemperature(temp)
        
        //runIn(21600, resetExternalSensor) - commented to keep in office. 
    }
}

// run thermostat from external sensor 3
def externalMotionHandler3(evt) {
    logDebug("External 3 Motion Event = ${evt.value}")
   
    if (evt.value == "active") {
        runIn(360,externalTempChangeCheck)
    }

}

def externalTempChangeCheck() {

    if (tempSensor3.currentValue("motion") == "active") {
        state.externalSensor = "temp3"
        def temp = tempSensor3.currentValue("temperature")
        virtualThermostat.setTemperature(temp)
        runIn(1200, resetExternalSensor)  // reset in an hour    
    }
}

// reset sensor back to defalt after a 1/2 hour of no motion
def resetExternalSensor() {
    state.externalSensor = "temp2"
    //def temp = tempSensor.currentValue("temperature")
    def temp = tempSensor2.currentValue("temperature")
    virtualThermostat.setTemperature(temp)
}

def externalTempHandler(evt) {
    logDebug("External Temp Change Event = ${evt.value}")

    if (state?.externalSensor == "temp1") {
        virtualThermostat.setTemperature(evt.value)
        runIn(1,logToGoogle)
        runIn(5, checkStates)
    }
}

def externalTempHandler2(evt) {
    logDebug("External 2 Temp Change Event = ${evt.value}")

    if (state?.externalSensor == "temp2") {
        virtualThermostat.setTemperature(evt.value)
        runIn(1,logToGoogle)
        runIn(5, checkStates)
    }
}

def externalTempHandler3(evt) {
    logDebug("External 3 Temp Change Event = ${evt.value}")

    if (state?.externalSensor == "temp3") {
        virtualThermostat.setTemperature(evt.value)
        runIn(1,logToGoogle)
        runIn(5, checkStates)
    }
}

def externalHumidityHandler(evt) {
    logDebug("External Humidity Change Event = ${evt.value}")
    syncHumidity(evt.value.toInteger(), physicalThermostat.currentValue("humidity").toInteger())
}

def physicalHumidityHandler(evt) {
    logDebug("Physical Humidity Change Event = ${evt.value}")
    syncHumidity(humiditySensor.currentValue("humidity").toInteger(),evt.value.toInteger())
}

def syncHumidity(external, physical) {
    
    if (physical != external) {
        def difference = external - physical
        def currentCal = physicalThermostat.currentValue("currentHumidityCal").toInteger()
        def newCal = currentCal + difference 
        physicalThermostat.HumidityCal(newCal)
        logDebug("New Humidity Cal is ${newCal}")
    }
}

def physicalThermoHeatPointHandler(evt) {
    logDebug("Physical Heat Point Handler Event = ${evt.value}")
    def value = evt.value
    def newHeatPoint = value.substring(0,2).toInteger() 

    def virtual = virtualThermostat.currentValue("heatingSetpoint").toInteger()
    def virtualMode = virtualThermostat.currentValue("thermostatMode")
    if (virtual != newHeatPoint && virtualMode == "heat") {
        virtualThermostat.setHeatingSetpoint(newHeatPoint)
    }
}

def physicalThermoCoolPointHandler(evt) {
    logDebug("Physical Cool Point Handler Event = ${evt.value}")
    def value = evt.value
    def newCoolPoint = value.substring(0,2).toInteger() 
    def virtualCool = virtualThermostat.currentValue("coolingSetpoint").toInteger()
    def virtualMode = virtualThermostat.currentValue("thermostatMode")

    logDebug("Cool point change is physcial")
    if ((virtualCool != newCoolPoint) && virtualMode == "cool") {
        virtualThermostat.setCoolingSetpoint(newCoolPoint) 
        logDebug("virtual cool point updated")
    }
}

def virtualThermoCoolPointHandler(evt) {
    logDebug("Virtual Cool Point Handler Event = ${evt.value}")
    def value = evt.value
    def newCoolPoint = value.substring(0,2).toInteger() 
    def physicalMode = physicalThermostat.currentValue("thermostatMode")
    def physical = physicalThermostat.currentValue("coolingSetpoint").toInteger()

    if (phsyical != newCoolPoint && physicalMode == "cool") {
        physicalThermostat.setCoolingSetpoint(newCoolPoint)   
    }  
    runIn(5, checkStates)
    runIn(1,logToGoogle)    
}

def virtualThermoHeatPointHandler(evt) {
    logDebug("Virtual Heat Point Handler Event = ${evt.value}")

    def newHeatPoint = evt.value.toInteger()
    def physical = physicalThermostat.currentValue("coolingSetpoint").toInteger()
    def physicalMode = physcialThermostat.currentValue("thermostatMode")
    if (physical != newHeatPoint && physicalMode == "heat") {
        physicalThermostat.setHeatingSetpoint(newHeatPoint)
    }
    runIn(5, checkStates)
    runIn(1,logToGoogle)
}

// switch virtual thermoMode to match physicalMode
def physicalModeHandler(evt) {
    logDebug("Physical Mode Change Event = ${evt.value}")
    def virtualMode = virtualThermostat.currentValue("thermostatMode")
    def physicalMode = evt.value

    if (evt.isPhysical()) {
        if (virtualMode != physicalMode) {
            virtualThermostat.setThermostatMode(physicalMode)
            if (physicalMode == "heat") {
                fanSwitch.setFan() // fan so AC not left running
            }       
        }
    }
}

def virtualModeHandler(evt) {
    logDebug("Virtual Mode Change Event = ${evt.value}")
    def physicalMode = physicalThermostat.currentValue("thermostatMode")
    def vitrualMode = evt.value

    if (virtualMode != physicalMode) {
        physicalThermostat.setThermostatMode(virtualMode)
        if (virtualMode == "heat") {
            fanSwitch.setCool()  // fan so AC not left running
        }
    }
}

// Send sensor calibatarion to physical thermostat
def sensorCalHandler(int change) {

    def currentCal = physicalThermostat.currentValue("currentSensorCal") as Integer
    def newCal = currentCal + change

    if (newCal > 3) newCal = 3
    if (newCal < -3) newCal = -3

    logDebug("New Sensor Cal is ${newCal}")

    if (currentCal != newCal) physicalThermostat.SensorCal(newCal)
}

// sync fan mode
def physicalFanModeHandler(evt) {
    virtualThermostat.setThermostatFanMode(evt.value)
}

def logToGoogle() {
    logDebug("Log to Google Called")
    def thermoTemp = physicalThermostat.currentValue("temperature")
    def sensorTemp = virtualThermostat.currentValue("temperature")
    def humidity = physicalThermostat.currentValue("humidity").toInteger()   
    def physicalState = physicalThermostat.currentValue("thermostatOperatingState")
    def virtualState = virtualThermostat.currentValue("thermostatOperatingState")
    def outsideTemp = virtualThermostat.currentValue("outsideTemp")
    def cycleSecs = virtualThermostat.currentValue("cycleSeconds")
    def thermostatMode = virtualThermostat.currentValue("thermostatMode")
    def thermostate = ""
    def logParams = ""

    if (thermostatMode == "heat") {
        def heatingSetpoint = virtualThermostat.currentValue("heatingSetpoint").toInteger()
        logDebug("Logging Heat")
        if (virtualState == "Fan Only" || virtualState == "idle") {
            thermoState = null
            logParams = "Heating Setpoint="+heatingSetpoint+"&Thermo Temp="+thermoTemp+"&Sensor Temp="+sensorTemp+"&Humidity="+humidity+"&Outside Temp="+outsideTemp+"&Cycle Secs="+cycleSecs
        } else {
            def thermoState = heatingSetpoint
            logParams = "Heating Setpoint="+heatingSetpoint+"&Thermo Temp="+thermoTemp+"&Sensor Temp="+sensorTemp+"&Thermo State="+thermoState+"&Humidity="+humidity+"&Outside Temp="+outsideTemp+"&Cycle Secs="+cycleSecs
        }
        state.lastOperatingState = physicalState

        def tab = ""
        def deviceName = physicalThermostat.getLabel()
        logDebug("deviceName for Logging is ${deviceName}")
        // if (googleLogging) {
        if (deviceName == "Bedroom Thermostat" || deviceName == "Living Room Thermostat" || deviceName == "Basement Thermostat") {
            if (deviceName == "Bedroom Thermostat") tab = "Bedroom Heat"
            if (deviceName == "Living Room Thermostat") tab = "Living Room Heat"
            if (deviceName == "Basement Thermostat") tab = "Basement Heat"
            logDebug("Sending Log Data to googleLogs...${tab}":"${logParams}")
            googleLogs.sendLog(tab, logParams)
        }
    }
    if (thermostatMode == "cool") {
        def coolingSetpoint = virtualThermostat.currentValue("coolingSetpoint").toInteger()
        logDebug("Logging AC")
        if (virtualState == "Fan Only" || virtualState == "idle") {
            thermoState = null
            logParams = "Cooling Setpoint="+coolingSetpoint+"&Thermo Temp="+thermoTemp+"&Sensor Temp="+sensorTemp+"&Humidity="+humidity+"&Outside Temp="+outsideTemp+"&Cycle Secs="+cycleSecs
        } else {
            def thermoState = coolingSetpoint
            logParams = "Cooling Setpoint="+coolingSetpoint+"&Thermo Temp="+thermoTemp+"&Sensor Temp="+sensorTemp+"&Thermo State="+thermoState+"&Humidity="+humidity+"&Outside Temp="+outsideTemp+"&Cycle Secs="+cycleSecs
        }
        state.lastOperatingState = physicalState

        def tab = ""
        def deviceName = physicalThermostat.getLabel()
        logDebug("deviceName for Logging is ${deviceName}")
        // if (googleLogging) {
        if (deviceName == "Bedroom Thermostat" || deviceName == "Living Room Thermostat") {
            if (deviceName == "Bedroom Thermostat") tab = "Bedroom AC"
            if (deviceName == "Living Room Thermostat") tab = "Living Room AC"
            logDebug("Sending Log Data to googleLogs...${tab}":"${logParams}")
            googleLogs.sendLog(tab, logParams)
        }
    }    
}

def logDebug(txt){
    def deviceName = physicalThermostat.getLabel()
    try {
        if (settings?.debugMode) log.debug("T6 Controller for ${deviceName} - ${txt}") 
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("debugMode",[value:"false",type:"bool"])
}