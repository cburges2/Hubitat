/* 
Zigbee Tower Fan Driver using 4-channel Zigbee Baord

Zigbee board integration created by Deepseek AI - 4/10/26
All other code © Christopher Burgess 2026

This driver uses the switch endpoints of the Zigbee board for fan control:
1 = Oscillate on/off - attach to the oscillate wire on the tower fan
2 = Fan speed Low - attach to the low speed wire on the tower fan
3 = Fan speed Medium - attach to the medium speed wire of the tower fan
4 = Fan speed High - attach to the high speed wire of the tower fan

Board should be in the mode for indpendent control of the relays.  Changing speeds will turn off the current speed and turn on the new speed relay. 
This driver also works from the physical buttons on the board to set speeds and occilate, as well as working with the board's RF remote functions. 

04/13/26:  Intital Code for Fan Controller Device using the board

*/

import hubitat.device.HubAction
import hubitat.device.Protocol

metadata {
    definition(name: "Zigbee Tower Fan Driver", namespace: "Hubitat", author: "chrisbvt") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "Fan Control"
        capability "Switch"
        capability "Actuator"        
       
        attribute "speed", "ENUM"
        attribute "switch", "ENUM"   
        attribute "supportedFanSpeeds", "JSON_OBJECT"
        attribute "oscillate", "ENUM"

        command "setSpeed", [[name:"setSpeed",type:"ENUM", description:"Set Fan Speed", constraints:["off","low","medium","high"]]]
        command "setOscillate", [[name:"setOscillate",type:"ENUM", description:"Set Fan Oscillate", constraints:["off","on"]]]
        command "on"
        command "off"
        command "configure"
        command "initialize"
        command "refresh"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input(name: "autoOffHours", type:"enum", title: "Fan Auto Off Hours", defaultValue: "Disabled", options:["Disabled","1","2","3","4","5","6","7","8","9","10","12","18","24"]), defaultValue: "Disabled"
    }
}

def installed() {
	log.warn "installed..." 
    state.speed = "off"

    sendEvent(name: "speed", value: "off")
    sendEvent(name: "switch", value: "off")   
    sendEvent(name: "offMinutes", value: "60" )
    sendEvent(name: "supportedFanSpeeds", value: '["off","low","medium","high"]')
    sendEvent(name: "oscillate", value: "off")

    configure()

	updated()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	if (logEnable) runIn(1800,logsOff)

    if (settings?.autoOffHours == "Disabled") {
        unschedule("autoFanOff")
    } else if (device.currentValue("speed") != "off") {
        startAutoOff()
    } 
}

def refresh() {
    if (logEnable) log.debug "Refreshing device"
    configure()
}

def initialize() {
    state.speed = "off"

    sendEvent(name: "speed", value: "off")
    sendEvent(name: "switch", value: "off")   
    sendEvent(name: "offMinutes", value: "60" )
    sendEvent(name: "supportedFanSpeeds", value: '["off","low","medium","high"]')
    sendEvent(name: "oscillate", value: "off")
}


// ************************************ Methods for Fan Driver ****************************
def setOscillate(status) {
    logDebug("setOscillate(${status}) called")
    sendEvent(name: "oscillate", value: status, descriptionText: getDescriptionText("oscillate set to ${status}")) 

    if (status == "on") {
        oscillateOn()
    }
    if (status == "off") {
        oscillateOff()
    }
}

def on() {
    sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch on")) 
    sendEvent(name: "speed", value: state?.speed, descriptionText: getDescriptionText("speed set to ${state?.speed}"))
    fanSpeedHandler(state?.speed)
    if (state?.wasOscillate) {
        setOscillate("on")
        state.wasOscillate = false
    }    
    if (settings?.setAutoOffHours != "Disabled") {startAutoOff}
}

def off() {
    if (device.currentValue("oscillate") == "on") {state.wasOscillate = true}
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch off")) 
    sendEvent(name: "speed", value: "off", descriptionText: getDescriptionText("speed set to off"))
    fanSpeedHandler("off")
    unschedule("autoTimerOff") 
}

def cycleSpeed() {
    def speed = device.currentValue("speed")
    if (speed == "low") setSpeed("medium")
    else if (speed == "medium") setSpeed("high")
    else if (speed == "high") setSpeed("low")
}

def setSpeed(speed) {
    logDebug "setSpeed(${speed}) was called"
    if (speed == "off") {off()}
    else if (speed == "on") {on()}
    else if (speed == "low" || speed == "medium" || speed == "high") {  
        sendEvent(name: "speed", value: speed, descriptionText: getDescriptionText("speed set to ${speed}"))   
        state.speed = speed     
        if (device.currentValue("switch") == "off") {sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch turned on"))}       
        fanSpeedHandler(speed)  // Zigbee Call for low, medium, high switches
        state.speed = speed
    } else if (speed == "auto") {   // toggle oscillate with auto fan mode
        def osc = device.currentValue("oscillate")
        if (osc == "on") {setOscillate("off")}
        if (osc == "off") {setOscillate("on")}
    }
}

// update driver states only when command came from the baord itself (or rf remote via board)
def updateDriverState(name, value) {
    logDebug("updateDriverState(${name}, ${value}) called")
    if (name == "speed") {
        if (value == "low" || value == "medium" || value == "high") {     
            if (device.currentValue("switch") == "off") {sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch set to on"))}
            sendEvent(name: "speed", value: value, descriptionText: getDescriptionText("speed set to ${speed}"))
        } 
        else {
            if (value == "off") {sendEvent(name: "speed", value: value, descriptionText: getDescriptionText("speed set to ${speed}"))}
        }
    } else {sendEvent(name: name, value: value, descriptionText: getDescriptionText("${name} set to ${value}"))}
}

def startAutoOff() {
    def hours = settings?.autoOffHours.toInteger()
    if (device.currentValue("speed") != "off") {
        def secs = hours.toInteger() * 360
        logDebug "Auto Off in ${hours} hours"
        runIn(secs, autoFanOff)
    }
}

def autoFanOff() {
    logDebug "autoFanOff() was called"
    if (device.currentValue("speed") != "off") {
        off()
    }
}

// ***************************** Methods for Fan Controller from parent ****************************

def fanSwitchHandler(status, speed) {
    if (status == "off") {
        fanRelay.off()
    }
    else if (status == "on") {
        
    }
}

// set the fan to the driver speed from child device
def fanSpeedHandler(speed) {

    def action = ""
    if (speed == "low" && currentFanSpeed() != "low") {fanOnLow();}
    if (speed == "medium" && currentFanSpeed() != "medium") {fanOnMed();}
    if (speed == "high" && currentFanSpeed() != "high") {fanOnHigh();}
    if (speed == "off" && currentFanSpeed() != "off") {fanOff();}
}

// Turn on speed and turn other speeds off
def fanOnLow() {
    endpointOn(2)    
    turnOffOtherSpeeds(false, true, true)
}

def fanOnMed() {
    endpointOn(3)
    turnOffOtherSpeeds(true, false, true)    
}

def fanOnHigh() {
    endpointOn(4)
    turnOffOtherSpeeds(true, true, false)
}

def fanOff() {
    endpointOff(4) // low off
    endpointOff(2) // med off
    endpointOff(3) // high off
    endpointOff(1) // osc off
}

def oscillateOn() {
    endpointOn(1)
}

def oscillateOff() {
    endpointOff(1)
}

def turnOffOtherSpeeds(low, medium, high) {
    logDebug("turnOffOtherSpeeds(${low}, ${medium}, ${high}) called")

    if (low && state?.switch2 == "on") {endpointOff(2)}          // low off
    if (medium && state?.switch3 == "on") {endpointOff(3)}    // med off
    if (high && state?.switch4 == "on") {endpointOff(4)}        // high off
}

// get current speed
String currentFanSpeed() {

    if (state?.switch2 == "on") {return "low"}
    if (state?.switch3 == "on") {return "medium"}
    if (state?.switch4 == "on") {return "high"}
    else {return "off"}
}

// ********** From Board Buttons (and RF remote) ***********

def fanSpeedChangeHandler(ep, value) {
    logDebug("fanSpeedChangeHandler(${ep}, ${value}) called")
    def speed = device.currentValue("speed")

    if (ep == 2) {
        if (value == "on" && device.currentValue("speed") != "low") {
            turnOffOtherSpeeds(false, true, true)
            updateDriverState("speed","low")
            state.speed = "low"
        }
        if (value == "off") {  // && checkOtherSpeedsOff("low")
            if (speed == "low") {
                state.speed = "off"
                updateDriverOff()
            }
        }
    }
    if (ep == 3) {
        if (value == "on"  && device.currentValue("speed") != "medium") {
            turnOffOtherSpeeds(true, false, true)
            updateDriverState("speed","medium")
            state.speed = "medium"
        }    
        if (value == "off") {  // && checkOtherSpeedsOff("medium")
            if (speed == "medium") {
                state.speed = "off"
                updateDriverOff()
            }
        }   
    }
    if (ep == 4) {
        if (value == "on"  && device.currentValue("speed") != "high") {
            turnOffOtherSpeeds(true, true, false)
            updateDriverState("speed","high")
            state.speed = "high"
        }
        if (value == "off") {    // && checkOtherSpeedsOff("high")
            if (speed == "high") {
                state.speed = "off"
                updateDriverOff()
            }
        } 
    }

}

def updateDriverOff() {
    updateDriverState("switch","off")
    //pauseMillis(100)
    updateDriverState("speed","off")    
}

// For rf remote or board buttons, lets a speed button act as off when pressed while current speed matches button
// could be done by checking driver speed, but hardware check is safer
def checkOtherSpeedsOff(speed) {
    logDebug("checkOtherSpeedsOff(${speed}) called")

    def othersOff = false
    lowOff = state?.switch1 == "off"
    medOff = state?.switch2 == "off"
    highOff = stae?.switch3 == "off"
    logDebug("lowOff=${lowOff} medOff=${medOff} highOff=${highOff}")

    if (speed == "low") {if (medOff && highOff) {othersOff = true}}
    if (speed == "medium") {if (lowOff && highOff) {othersOff = true}}
    if (speed == "high") {if (lowOff && medOff) {othersOff = true}}

    logDebug("returning ${othersOff}")
    return othersOff
}

// ****************** Board Control Methods ******************************

def configure() {
    if (logEnable) runIn(1800, logsOff)
    log.info "Configuring ${device.displayName}"

    // Configure endpoints 1-4 directly (most multi-endpoint switches use these)
    (1..4).each { ep ->
        // Bind the On/Off cluster (0x0006) for this endpoint
        sendZigbeeCommands(zigbee.bind(0x0006, ep))
        // Configure reporting: send state change immediately, min interval 0, max 300s, change 1
        sendZigbeeCommands(zigbee.configureReporting(0x0006, ep, 0x0000, 0x10, 0, 300, 1))
        // Read current state so we know initial value
        sendZigbeeCommands(zigbee.readAttribute(0x0006, ep, 0x0000))
    }
    log.info "Configuration commands sent for endpoints 1-4"
}

// Parse incomming messages
def parse(description) {
    if (logEnable) log.debug "Parsing: ${description}"
    def map = zigbee.parseDescriptionAsMap(description)
    if (!map) return
        if (map.clusterInt == 0x0006 && map.attrInt == 0x0000) {   
            if (map.value) {
                def ep = Integer.parseInt(map.endpoint, 16)
                if (ep == null || ep < 1 || ep > 4) return  // ignore invalid endpoints
                def value = map.value
                logDebug("Value is ${value}")
                def stateVal = (value == "01") ? "on" : "off"
                logDebug("stateVal = ${stateVal}")
                //def attrName = "switch${ep}"

                if (txtEnable) log.info "${device.displayName} endpoint ${ep} is ${stateVal}"
                state."switch${ep}" = stateVal
                if (ep != 1) fanSpeedChangeHandler(ep, stateVal) else {fanOscChangeHandler(stateVal)}
                return
            }
        }
    if (logEnable) log.debug "Unhandled message: ${map}"
}

def fanOscChangeHandler(value) {
    logDebug("fanOscHandler called with ${value}")

    if (value == "on") {
        sendEvent(name: "oscillate", value: "on", descriptionText: getDescriptionText("${name} set to ${value}"))
    }
    if (value == "off") {
        sendEvent(name: "oscillate", value: "off", descriptionText: getDescriptionText("${name} set to ${value}"))
    }    
}

// Turn on a specific endpoint (1-4)
def endpointOn(endpointNumber) {
    // Convert to integer in case it's passed as string (e.g., from rule)
    def ep = endpointNumber as Integer
    if (ep < 1 || ep > 4) {
        log.warn "endpointOn: invalid endpoint number ${endpointNumber} (must be 1-4)"
        return
    }
    sendEndpointCommand(ep, 0x01)  // 0x01 = ON
    state."switch${ep}" = "on"
    if (logEnable) log.debug "Turned ON endpoint ${ep}"
}

// Turn off a specific endpoint (1-4)
def endpointOff(endpointNumber) {
    def ep = endpointNumber as Integer
    if (ep < 1 || ep > 4) {
        log.warn "endpointOff: invalid endpoint number ${endpointNumber} (must be 1-4)"
        return
    }
    sendEndpointCommand(ep, 0x00)  // 0x00 = OFF
    state."switch${ep}" = "off"
    if (logEnable) log.debug "Turned OFF endpoint ${ep}"
}

// Helper to send Zigbee On/Off command to a specific endpoint
private void sendEndpointCommand(int endpoint, int onOff) {
    def cmd = "he cmd 0x${device.deviceNetworkId} 0x${endpoint.toString().padLeft(2,'0')} 0x0006 0x${onOff.toString().padLeft(2,'0')} {}"
    sendHubCommand(new HubAction(cmd, Protocol.ZIGBEE))
}

// Helper to send multiple Zigbee commands
private void sendZigbeeCommands(List<String> cmds) {
    cmds.each { cmd ->
        sendHubCommand(new HubAction(cmd, Protocol.ZIGBEE))
    }
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	if (settings?.txtEnable) log.info "${descriptionText}"
	return descriptionText
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}