/**
 *  **************** Fan Hood Controller App  ****************
 *
 *  Usage:
 *  This was designed to run a converted stove hood fan using relays to control the fan with three speeds 
 *  Each speed switch device is part of a 4-channel relay board, set to jog mode so that any other switches turn off when one is turned on
 *  The last relay is used as off, since the board will turn off any other speed relays when it is turned on in jog mode. 

 *  Requirements:  
 *  A four channel smart relay baord with jog mode
*   Three of the relays have been physically attached to supply voltage to the appropriate motor wires for low, medium and high.  
*   The board I am using needs 5v usb, or 7-48v).  An external power supply may be needed if the required voltage is not avaialbe on the hood's board. 
*
*   Version 1.0 - 11/14/24
*   Version 1.1 - 11/15/24 - Added optional stove light device which if added in prefrences, will turn on when the fan turns on
**/

definition (
    name: "Fan Hood Controller App",
    namespace: "Hubitat",
    author: "ChrisB",
    description: "Controller for a Converted Stove Fan Hood using Relays",
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

        section("<b>Hood Fan Speed Low Device</b>") {

            input (
              name: "fanLow", 
              type: "capability.switch", 
              title: "Select Fan Speed Low Switch Device", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Hood Fan Speed Medium Device</b>") {

            input (
              name: "fanMedium", 
              type: "capability.switch", 
              title: "Select Fan Speed Medium Switch Device", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Hood Fan Speed High Device</b>") {

            input (
              name: "fanHigh", 
              type: "capability.switch", 
              title: "Select Fan Speed High Switch Device", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }


        section("<b>Hood Fan Speed Off Device</b>") {

            input (
              name: "fanOff", 
              type: "capability.switch", 
              title: "Select Fan Speed Off Switch Device", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Fan Hood Driver Device</b>") {

            input (
              name: "hoodDriver", 
              type: "capability.actuator", 
              title: "Select Fan Hood Driver Fan Device", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Stove Hood Light</b>") {

            input (
              name: "stoveLight", 
              type: "capability.switch", 
              title: "Select Stove Hood Light Switch Device (optional)", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Fan Doubletap Switch</b>") {

            input (
              name: "fanSwitch", 
              type: "capability.switch", 
              title: "Select Fan Doubletap Switch Device (optional)", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Fan Control with 4-button Scene Switch</b>") {

            input (
              name: "fanSceneSwitch", 
              type: "capability.actuator", 
              title: "Select Fan Scene Switch Device (optional)", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("") {
            input (
                name: "useAutoOff", 
                type: "bool", 
                title: "<font style='font-size:14px; color:#1a77c9'>Enable Auto-Off</font>", 
                required: true, 
                defaultValue: false
            )
        } 

        section("") {
            input (
                name: "autoOff",
                type: "enum",
                title: "<font style='font-size:14px; color:#1a77c9'>Auto Off Minutes</font>",
                options: ["600":10, "1200":20,"1800":30,"2700":45,"3600":60,"5400":90,"7200":120],
                multiple: false,
                defaultValue: 60,
                required: false
            )
        }

        if (stoveLight) {
            section("") {
                input (
                    name: "useStoveLight", 
                    type: "bool", 
                    title: "<font style='font-size:14px; color:#1a77c9'>Turn On Stove Light with Fan On</font>", 
                    required: false, 
                    defaultValue: true
                )
            } 
        }

        section("") {
            input (
                name: "debugMode", 
                type: "bool", 
                title: "<font style='font-size:14px; color:#1a77c9'>Enable logging</font>", 
                required: true, 
                defaultValue: false
            )
        }
    }
}

def installed() {
    
    updated()
}

def updated() {
    
    unsubscribe()
    initialize()
}

def initialize() {

    if (fanSwitch) subscribe(fanSwitch, "doubleTapped", fanSwitchHandler)           // control with existing switch doubletap
    if (fanSceneSwitch) subscribe(fanSceneSwitch, "pushed", fanSceneSwitchHandler)  // control with a scene switch

    if (hoodDriver) {
        subscribe(hoodDriver, "speed", hoodSpeedHandler)      // control from the driver events
        subscribe(hoodDriver, "switch", hoodSwitchHandler)    // control from the driver events
    }

    subscribe(fanOff, "switch", fanPowerHandler)  // to catch an "on" event to start timer when fanOff is turned off
} 

// driver switch change event
def hoodSwitchHandler(evt) {
    if (evt.value == "on") {
        def speed = hoodDriver.currentValue("speed")
        setSpeed(speed)
    }
    if (evt.value == "off") {
        turnFanOff()
    }
}

// driver speed change event
def hoodSpeedHandler(evt) {
    setSpeed(evt.value)
} 

// set fan to the driver speed event value
def setSpeed(speed) {

    def action = ""
    if (speed == "low" && currentFanSpeed() != "low") {fanLow.on(); action = "low"}
    if (speed == "medium" && currentFanSpeed() != "medium") {fanMedium.on(); action = "medium"}
    if (speed == "high" && currentFanSpeed() != "high") {fanHigh.on(); action = "high"}
    if (speed == "off" && currentFanSpeed() != "off") {turnFanOff(); action = "off"}

    updateDriver(action)
}

// get current speed
String currentFanSpeed() {

    if (fanLow.currentValue("switch") == "on") {return "low"}
    if (fanMedium.currentValue("switch") == "on") {return "medium"}
    if (fanHigh.currentValue("switch") == "on") {return "high"}
    else {return "off"}
}

// doubletap switch handler for fan speeds
def fanSwitchHandler(evt) {
    logDebug("fanSwitchHandler got ${evt.value}")
    def doubletapped = (evt.value).toInteger()
    def button = ""

    if (doubletapped == 1) {button = "up"}
    else {button = "down"}

    def action = ""

    fanOn = fanIsOn()  // off relay on means fan is off

    // Turn on fan to either high or low with switch buttons when it is off
    if (button == "down" && !fanOn) {
        logDebug("fanSwitchHandler got Down and fan Off")
        fanLow.on()
        action = "low"
    }
    if (button == "up" && !fanOn) {
        logDebug("fanSwitchHandler got Up and fan Off")
        fanHigh.on()
        action = "high"
    }

    def fanSpeed = currentFanSpeed()
    
    // Change speed when fan is alreay on based on current speed 
    if (button == "down" && fanOn) {                      // DOWN doubletap
        logDebug("fanSwitchHandler got down and fan On")
        if (fanSpeed == "low") {turnFanOff(); action = "off"}  // turn off from low
        if (fanSpeed == "medium") {fanLow.on(); action = "low"}  // turn low from medium
        if (fanSpeed == "high") {fanMedium.on(); action = "medium"}  // turn medium from high
    }
    
    if (button == "up" && fanOn) {                    // UP doubletap
        logDebug("fanSwitchHandler got up and fan On")
        if (fanSpeed == "high") {turnFanOff(); action = "off"}  // turn off from high
        if (fanSpeed == "medium") {fanHigh.on(); action = "high"}  // turn High from med
        if (fanSpeed == "low") {fanMedium.on(); action = "medium"}  // turn medium from low      
    }

    updateDriver(action)
}

// Scene Switch Handler for fan speeds
def fanSceneSwitchHandler(evt) {

    def pressed = evt.value.toInteger()
    def action = ""

    if (pressed == 1) {turnFanOff(); action = "off"}
    if (pressed == 2) {fanLow.on(); action = "low"}
    if (pressed == 3) {fanMedium.on(); action = "medium"}
    if (pressed == 4) {fanHigh.on(); action = "high"}

    updateDriver(action)
}

// activate fan power off switch (4th relay clears all speed relays on the board when on)
def turnFanOff() {
    fanOff.on()
    updateDriver("off")
}

// update the driver device attributes with speed being set
def updateDriver(speed) {
    logDebug("updateDriver called with ${speed}")
    if (speed != "") {
        def driverSpeed = hoodDriver.currentValue("speed")
        if (driverSpeed != speed) {hoodDriver.setSpeed(speed)}
    }
}

// start timer when fan off relay triggers to off (meaning the fan was turned on)
def fanPowerHandler(evt) {
    logDebug("fanPowerHandler called with ${evt.value}")
    
    if (evt.value == "off") {      
        def timerMinutes = (settings?.autoOff).toInteger()
        logDebug("timerMinutes is ${timerMinutes}")
        if (settings?.useAutoOff) runIn(timerMinutes,turnFanOff)
        if (stoveLight && useStoveLight) {stoveLight.on()}
    }
    if (evt.value == "on") {unschedule()}
    
}

// get bool to check if fan is on true/false
boolean fanIsOn() {
    return fanOff.currentValue("switch") == "off"
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}
