/**
 *  ****************  Broadlink Fan Connector  ****************
 *
 *  Usage:
 *  This was designed to sync 
 *
**/

definition (
    name: "Broadlink Fan Connector",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for Virtual ",
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

        section("<b>Broadlink Device</b>") {

            input (
              name: "broadlinkDevice", 
              type: "capability.actuator", 
              title: "Select BroadLink Device", 
              required: true, 
              multiple: false           
            )
        }    

        section("<b>Virtual Fan Device</b>") {

            input (
              name: "fan", 
              type: "capability.fanControl", 
              title: "Select Virtual Fan", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Fan Power Meter</b>") {

            input (
              name: "meter", 
              type: "capability.powerMeter", 
              title: "Select Fan Power Meter", 
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
    initialize()
}

def updated() {
    initialize()
    state.mode = "normal"
}

def initialize() {

    subscribe(fan, "switch", switchHandler) 
    subscribe(fan, "speed", speedHandler)
    subscribe(fan, "oscillate", oscillateHandler)
    subscribe(fan, "mode", modeHandler)

    subscribe(meter, "power", meterHandler)

} 

def oscillateHandler(evt) {
    logDebug("oscillate event ${evt.value}")

    setOscillate(evt.value)

}

def setOscillate(value) {

    if (value == "on" && state?.oscillate == "off") {
        broadlinkDevice.SendStoredCode("Fan Osillate")        
        state.oscillate = "on"
    } else if (value == "off" && state?.oscillate == "on") {
        broadlinkDevice.SendStoredCode("Fan Osillate")
        state.oscillate = "off"       
    }
}

def meterHandler(evt) {
    logDebug("Power Meter Event ${evt.value}")

    def power = evt.value.toInteger()

    if (power > 10 && fan.currentValue("switch") != "on") {
        state.switch = "on"
        fan.on()
    } else if (power <= 10 && fan.currentValue("switch") != "off") {
        state.switch = "off"
        fan.off()
    }   

    if (power > 10) {runIn(5,syncFanSpeed)}
}

def syncFanSpeed() {
    def power = meter.currentValue("power").toInteger()

    if (power >= 27 && power <= 34) {
        if (fan.currentValue("speed") != "low") {fan.setSpeed("low")}
    } else if (power >= 35 && power <= 40) {
        if (fan.currentValue("speed") != "medium") {fan.setSpeed("medium")}
    } else if (power >= 41 && power <= 48) {
        if (fan.currentValue("speed") != "high") {fan.setSpeed("high")}
    }
}

String getFanSpeed() {

    def power = meter.currentValue("power").toInteger() 
    def speed = ""

    if (power >= 27 && power <= 34) {
        speed = "low"
    } else if (power >= 35 && power <= 41) {
        speed = "medium"
    } else if (power >= 41 && power <= 48) {
        speed = "high"
    }

    return speed
}

def switchHandler(evt) {    
    logDebug("Switch Event = ${evt.value}")   
    def power = evt.value

    if (power == "on" && state?.switch == "off") {
        state.switch = "on"
        broadlinkDevice.SendStoredCode("Fan Power")                     
        runIn(7,setLastState)   
    } else if (power == "off" && state?.switch == "on") {
        broadlinkDevice.SendStoredCode("Fan Power")
        state.switch = "off"
    }
}

def switchPower(power) {

    def switchState = fan.currentValue("switch")

    if (power == "on" && state?.switch == "off" && switchState == "off") {
        state.switch = "on"
        broadlinkDevice.SendStoredCode("Fan Power")                     
        runIn(7,setLastState)   
    } else if (power == "off" && state?.switch == "on" && switchState == "on") {
        broadlinkDevice.SendStoredCode("Fan Power")
        state.switch = "off"
    }
}

def modeHandler(evt) {
    //state.mode = evt.value
    setMode(evt.value)
}

def setMode(mode) {
    def fanMode = state?.mode
    logDebug("fanMode is ${fanMode}")

    if (mode != fanMode) {
        def modeValue = getModeValue(mode)
        def currentValue = getModeValue(fanMode)
        logDebug("modeValue is ${modeValue}")
        logDebug("currentValue is ${currentValue}")
        
        def steps = modeValue + (3 - currentValue)
        
        if (steps > 2) {steps = steps - 3}  
        logDebug("steps is ${steps}")
        if (steps > 0) {broadlinkDevice.SendStoredCode("Fan Mode",steps)}

        state.mode = mode
    } 
    
}

int getModeValue(mode) {

    def modeValue = 0
    if (mode == "normal") modeValue = 1
    if (mode == "breeze") modeValue = 2
    if (mode == "night") modeValue = 3

    return modeValue
}

def setLastState() {
    def speed = fan.currentValue("speed")
    def lastSpeed = fan.currentValue("lastSpeed")
    if (lastSpeed != speed) {setSpeed(lastSpeed)}  
    if (fan.currentValue("oscillate") == "on") {broadlinkDevice.SendStoredCode("Fan Osillate")} 
    pauseExecution(500)
    if (fan.currentValue("mode") == "breeze") {
        broadlinkDevice.SendStoredCode("Fan Mode")
        state.mode = "breeze"
    } 
    if (fan.currentValue("mode") == "night") {
        broadlinkDevice.SendStoredCode("Fan Mode",2)
        state.mode = "night"
    }
}

def speedHandler(evt) {
    logDebug("Speed Handler Event = ${evt.value}")   
    def speed = evt.value

    if (speed == "on") {switchPower("on")}
    else if (speed == "off") {switchPower("off")}
    else if (speed == "medium-low") {setMode("normal")}
    else if (speed == "medium-high") {setMode("breeze")}
    else if (speed == "auto") {
        logDebug("Setting Oscillate")
        if (state?.oscillate == "on") {
            fan.setOscillate("off")
            state.oscillate = "off"
        }
        else {
            fan.setOscillate("on")
            state.oscillate = "on"
        }
    }
    else {setSpeed(speed)}
    
}

def setSpeed(speed) {
    logDebug("setSpeed ${speed}")

    def fanSpeed = getFanSpeed()
    logDebug("fanSpeed is ${fanSpeed}")

    if (speed != fanSpeed) {
        def speedValue = getSpeedValue(speed)
        def currentValue = getSpeedValue(fanSpeed)
        logDebug("speedValue is ${speedValue}")
        logDebug("currentValue is ${currentValue}")
        
        def steps = speedValue + (3 - currentValue)
        
        if (steps > 2) {steps = steps - 3}  
        logDebug("steps is ${steps}")
        if (steps > 0) {broadlinkDevice.SendStoredCode("Fan Speed",steps)}
    }
}

int getSpeedValue(speed) {

    def speedValue = 0
    if (speed == "low") speedValue = 1
    if (speed == "medium") speedValue = 2
    if (speed == "high") speedValue = 3

    return speedValue
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}