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
 *  Version 1/24/24 - heatswitch2 is now a Pre-heat switch, so the turn-off delay is now from start of pre-heat instead of from heat off. 
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

        section("<b>Use a Device with Heat On?</b>") {
            input (
              name: "selectController", 
              type: "bool", 
              title: "Use Switch With heat On", 
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
                }
                section("") {
                    input (
                        name: "startDelay1",
                        type: "enum",
                        title: "<font style='font-size:14px; color:#1a77c9'>Switch Start Delay (Start delay after Heat On)</font>",
                        options: [0:"0", 1:"1", 3:"3", 5:"5", 10:"10",15:"15",20:"20"],
                        multiple: false,
                        defaultValue: 0,
                        required: true
                    )
                }
                section("") {
                    input (
                        name: "stopDelay1",
                        type: "enum",
                        title: "<font style='font-size:14px; color:#1a77c9'>Switch Stop Delay (Stop delay after Heat Off)</font>",
                        options: [0:"0", 1:"1", 3:"3", 5:"5", 10:"10",15:"15",20:"20"],
                        multiple: false,
                        defaultValue: 0,
                        required: true
                    )
                }
            } else {
                state.useSwitch = false             
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

        section("<b>Use Pre-Heat Device with Heat On?</b>") {
            input (
              name: "selectController2", 
              type: "bool", 
              title: "Use Pre-Heat with Heat On", 
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
                        title: "Select Pre-Heat Switch Device", 
                        required: true, 
                        multiple: false
                    )
                }
                section("") {
                    input (
                        name: "startDelay2",
                        type: "enum",
                        title: "<font style='font-size:14px; color:#1a77c9'>Start Delay for Pre-Heat after Heat On</font>",
                        options: [0:"0", 1:"1", 3:"3", 5:"5", 10:"10",15:"15",20:"20"],
                        multiple: false,
                        defaultValue: 0,
                        required: true
                    )
                }
                section("") {
                    input (
                        name: "stopDelay2",
                        type: "enum",
                        title: "<font style='font-size:14px; color:#1a77c9'>Stop Delay for Pre-Heat after Pre-Heat On</font>",
                        options: [0:"0", 1:"1", 3:"3", 5:"5", 10:"10",15:"15",20:"20"],
                        multiple: false,
                        defaultValue: 0,
                        required: true
                    )
                }
            } else {
                state.useSwitch2 = false             
            }
        }   

/*         section("<b>Use Heat Maintenance Cycles?</b>") {
            input (
              name: "cycle", 
              type: "bool", 
              title: "Cycle Heat to Maintain Temperature?", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.cycle) {
                section("") {
                    input (
                        name: "cyclesPerHour",
                        type: "enum",
                        title: "<font style='font-size:14px; color:#1a77c9'>Cycles Per Hour</font>",
                        options: [1:"1", 2:"2", 3:"3", 4:"4",5:"5",6:"6"],
                        multiple: false,
                        defaultValue: 0,
                        required: true
                    )
                }
                section("") {
                    input (
                        name: "cycleMinutes",
                        type: "enum",
                        title: "<font style='font-size:14px; color:#1a77c9'>Minutes to Cycle</font>",
                        options: [5:"5", 6:"6", 7:"7", 8:"8", 9:"9",10:"10",11:"11",12:"12"],
                        multiple: false,
                        defaultValue: 0,
                        required: true
                    )
                }
            } else {
                state.useSwitch2 = false             
            }
        }    */

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
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {

    // sync thermostats
    subscribe(physicalThermostat, "coolingSetpoint", physicalThermoCoolPointHandler)  // set virtual cooling setpoint   
    subscribe(physicalThermostat, "heatingSetpoint", physicalThermoHeatPointHandler)  // set virtual heating setpoint
    subscribe(physicalThermostat, "thermostatMode", physicalModeHandler) // set virtual mode
    subscribe(physicalThermostat, "thermostatOperatingState", physicalStateHandler)
    subscribe(virtualThermostat, "heatingSetpoint", virtualThermoHeatPointHandler)  // set virtual heating setpoint
    subscribe(virtualThermostat, "thermostatOperatingState", virtualStateHandler)
    subscribe(virtualThermostat, "thermostatFanMode", virtualFanHandler)

    state.physicalThermoState = physicalThermostat.currentValue("thermostatOperatingState")

    subscribe(outdoorSensor, "temperature", outdoorTempHandler)

    if (state?.useSwitch) subscribe(heatSwitch, "switch", heatSwitchHandler)
    if (state?.useSwitch2) subscribe(heatSwitch2, "switch", heatSwitch2Handler)

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

    runIn(1,checkStates)
}

def virtualFanHandler(evt) {
    def fanMode = evt.value
    
    if (fanMode == "auto") {state.fanMode = fanMode}
    if (fanMode == "fan") {state.fanMode = fanMode}
    if (fanMode == "circulate") {state.fanMode = fanMode}
}

def externalMotionHandler(evt) {
}

def restartCheckTimer() {
    runIn(60, checkStates)
}

def heatSwitchHandler(evt) {
    logDebug("Heat Switch Handler - Heat Swtich ${evt.value}")
    def heating = virtualThermostat.currentValue("thermostatOperatingState") == "heating"

    if (evt.value ==  "on" && !heating) {
        state.switchAlreadyOn = true
    }
    if (evt.value == "off" && !heating) {
        state.switchAlreadyOn = false
    }
}

def heatSwitch2Handler(evt) {
    logDebug("Heat Switch2 Handler - Heat Swtich2 ${evt.value}")
    def heating = virtualThermostat.currentValue("thermostatOperatingState") == "heating"

    if (evt.value == "on" && heating) {
        state.switch2AlreadyOn = true
    }
    if (evt.value == "off" && !heating) {
        state.switch2AlreadyOn = false
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
    def physicalState = physicalThermostat.currentValue("thermostatOperatingState") 
    def virtualState = virtualThermostat.currentValue("thermostatOperatingState") 
    Float virtualTemp = new Float(virtualThermostat.currentValue("temperature"))
    Float physicalTemp = new Float(physicalThermostat.currentValue("temperature"))
    def relaysDisengaged = false
    if (engageRelays) {relaysDisengaged = engageRelays.currentValue("switch") == "off"}

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
        if (physicalMode == "cool" || physicalMode == "heat") {
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

    // refresh temp sensors - living room cannot be refreshed (ecowitt sensor)
    if (physicalThermostat.getLabel() != "Living Room Thermostat") {
        tempSensor.refresh()
        if (tempSensor2) {tempSensor2.refresh()}
        if (tempSensor3) {tempSensor3.refresh()}
    }
    logDebug("checkStates() completed")

    restartCheckTimer() // repeat timer
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

    // check if changed (not equal to current state)
    if (state?.virtualThermoState != virtualState) {
        if (virtualState == "heating") {
            state.onByThermo1 = true
            logDebug("Virtual Started Heating")

            unschedule("heatSwitch1Off")  // in case heat turned back on before switch scheduled off

            // use relays if engaged, else lower cal to turn on physical
            if (relaysDisengaged) {
                sensorCalHandler(-1)    // set calibration to lower physical temp
            } else {
                heatRelay.on()          // Turn on Relay       
                runIn(5, confirmOn)
            }
            
            // Turn on other devices' switch with heat (with delay)
            physicalThermostat.IdleBrightness(operatingBrightness)
            if (state?.useSwitch) {              
                def startDelay1 = settings?.startDelay1.toInteger() * 60               
                runIn(startDelay1, heatSwitch1On)
            }
            if (state?.useSwitch2) {               
                def startDelay2 = settings?.startDelay2.toInteger() * 60
                runIn(startDelay2, heatSwitch2On)
            }          
        }
        if (virtualState == "idle" || virtualState == "fan only") {
            logDebug("Virtual Finished Heating")
            
            unschedule("heatSwitch1On")  // in case heat turned off before switch scheduled on

            // use relays if engaged, else raise cal to turn off physical
            if (relaysDisengaged) {
                sensorCalHandler(2)      // set calibration to increase physical temp
            } else {
                heatRelay.off()         // turn off relay
                runIn(5,confirmOff)
            }

            // Turn off other devices switch with heat off (with delay)
            physicalThermostat.IdleBrightness(idleBrightness)
            if (state?.useSwitch2) { 
                def stopDelay2 = settings?.stopDelay2.toInteger() * 60              
                runIn(stopDelay2, heatSwitch2Off)
            } 
            if (state?.useSwitch) {
                def stopDelay1 = settings?.stopDelay1.toInteger() * 60
                runIn(stopDelay1,heatSwitch1Off)
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

def heatSwitch1On() {
    state.onByThermo1 = true
    if (!state?.switchAlreadyOn) heatSwitch.on()
}

def heatSwitch1Off() {
    pauseExecution(2000)
    if (!state?.switchAlreadyOn) heatSwitch.off()
}

def heatSwitch2On() {
    // state.onByThermo2 = true
    heatSwitch2.on()  
    def stopDelay2 = settings?.stopDelay2.toInteger() * 60              
    runIn(stopDelay2, heatSwitch2Off) 
}

def heatSwitch2Off() {
    heatSwitch2.off()
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
        state.externalSensor = "temp3"
        def temp = tempSensor3.currentValue("temperature")
        virtualThermostat.setTemperature(temp)
        runIn(21600, resetExternalSensor)
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
    BigDecimal newHeatPoint = new BigDecimal(evt.value)
    virtualThermostat.setHeatingSetpoint(newHeatPoint)
    runIn(5, checkStates)
    runIn(1,logToGoogle)
}

def physicalThermoCoolPointHandler(evt) {
    logDebug("Physical Cool Point Handler Event = ${evt.value}")
    BigDecimal newCoolPoint = new BigDecimal(evt.value)
    virtualThermostat.setCoolingSetpoint(newCoolPoint) 
}

def virtualThermoHeatPointHandler(evt) {
    logDebug("Virtual Heat Point Handler Event = ${evt.value}")
    runIn(5, checkStates)
}

// switch virtual thermoMode to match physicalMode
def physicalModeHandler(evt) {
    logDebug("Physical Mode Change Event = ${evt.value}")
    def physicalMode = evt.value
    virtualThermostat.setThermostatMode(physicalMode)

/*     // switch relays with thermostatMode
    def relaysOn = engageRelays.currentValue("switch") == "on"
    if (physicalMode == "heat" && engageRelays && !relaysOn) {engageRelays.on()}
    else if (physicalMode == "cool" && engageRelays && relaysOn) {engageRelays.off()} */
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

def logToGoogle() {
    logDebug("Log to Google Called")
    def thermoTemp = physicalThermostat.currentValue("temperature")
    def sensorTemp = tempSensor.currentValue("temperature")
    def humidity = physicalThermostat.currentValue("humidity").toInteger()
    def heatingSetpoint = physicalThermostat.currentValue("heatingSetpoint").toInteger()
    def physicalState = physicalThermostat.currentValue("thermostatOperatingState")
    def thermostate = ""
    def logParams = ""

    if (physicalThermostat.currentValue("thermostatMode") == "heat") {
        if (physcialState == "Fan Only" || physicalState == "idle") {
            if (state?.lastOperatingState == "heating") {
                thermoState = physicalThermostat.currentValue("heatingSetpoint")
                logParams = "Heating Setpoint="+heatingSetpoint+"&Thermo Temp="+thermoTemp+"&Sensor Temp="+sensorTemp+"&Thermo State="+thermoState+"&Humidity="+humidity
            } else {
                thermoState = null
                logParams = "Heating Setpoint="+heatingSetpoint+"&Thermo Temp="+thermoTemp+"&Sensor Temp="+sensorTemp+"&Humidity="+humidity
            }
        } else {
            thermoState = heatingSetpoint
            logParams = "Heating Setpoint="+heatingSetpoint+"&Thermo Temp="+thermoTemp+"&Sensor Temp="+sensorTemp+"&Thermo State="+thermoState+"&Humidity="+humidity
        }
        state.lastOperatingState = physicalState

        def tab = ""
        def deviceName = physicalThermostat.getLabel()
        logDebug("deviceName for Logging is ${deviceName}")
        if (deviceName == "Bedroom Thermostat" || deviceName == "Living Room Thermostat") {
            if (deviceName == "Bedroom Thermostat") tab = "Bedroom Heat"
            if (deviceName == "Living Room Thermostat") tab = "Living Room Heat"
            if (deviceName == "Basement Thermostat") tab = "Basement Heat"
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