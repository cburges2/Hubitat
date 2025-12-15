/**
 *  **************** Office Fan Parent App ****************
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
*   Version 2.0 - 11/21/25 - Forked to be the child device of the parent app and call parent methods.  
**/

definition (
    name: "Office Fan Parent App",
    namespace: "Hubitat",
    author: "ChrisB",
    description: "Controller for a Converted Office Fan using Relays",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Office Fan Speed Low Device</b>") {

            input (
              name: "fanLow", 
              type: "capability.switch", 
              title: "Select Fan Speed Low Switch Device", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Office Fan Speed Medium Device</b>") {

            input (
              name: "fanMedium", 
              type: "capability.switch", 
              title: "Select Fan Speed Medium Switch Device", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Office Fan Speed High Device</b>") {

            input (
              name: "fanHigh", 
              type: "capability.switch", 
              title: "Select Fan Speed High Switch Device", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Office Fan Relay Device</b>") {

            input (
              name: "fanRelay", 
              type: "capability.switch", 
              title: "Select Fan Relay Switch Device", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Office Fan Occilate Device</b>") {

            input (
              name: "fanOsc", 
              type: "capability.switch", 
              title: "Select Office Fan OSC Switch Device", 
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

        section("<b>Fan Speed Indicator Light Driver</b>") {

            input (
              name: "speedLight", 
              type: "capability.actuator", 
              title: "Select Indicator Light Driver Device (optional)", 
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
    
    setOfficeFanDevice()
    unsubscribe()
    initialize()
}

def initialize() {

    if (fanSceneSwitch) {
        subscribe(fanSceneSwitch, "pushed", fanSceneSwitchHandler)  // control with a scene switch
        subscribe(fanSceneSwitch, "doubleTapped", fanSceneSwitchTimerHandler)  // control timer with a scene switch
    }

    // for changes made on the Zigbee relay baord or with the rf board remote
    subscribe(fanOsc, "switch", fanOscHandler) 
    subscribe(fanLow, "switch", fanLowHandler)
    subscribe(fanMedium, "switch", fanMediumHandler)
    subscribe(fanHigh, "switch", fanHighHandler)
} 

def setOscillate(status) {
    if (status == "on") {
        fanOsc.on()
    }
    if (status == "off") {
        fanOsc.off()
    }
}


def setOfficeFanDevice() {
    logDebug("settingOfficeFanDevice")
    if (!fanDriver) {
        def ID = createOfficeFanDevice()
        app.updateSetting("fanDriver",[value:getChildDevice(ID),type:"capability.actuator"])
    }
}

def createOfficeFanDevice() {
    logDebug("createOfficeFanDevice() called")
    def deviceNetworkId = "CD_${app.id}_${new Date().time}"
    
    try {
        def fanHoodDevice = addChildDevice(
            "Hubitat", 
            "Office Fan Child Driver", 
            deviceNetworkId,
            null,
            [name: "Office Fan Child Device", label: "Office Fan", isComponent: false]
        )       
        logDebug("Created Office Fan device in 'Hubitat' using driver 'Office Fan Child Driver' (DNI: ${deviceNetworkId})")
        state.fanHoodDevice = deviceNetworkId
        return deviceNetworkId

    } catch (Exception e) {
        log.error "Failed to create Office Fan device: ${e}"
    }
}

// driver speed change event - Change fan speed
def fanSpeedHandler(speed) {
    logDebug("fanSpeedHandler(${speed}) called")
    def status = fanDriver.currentValue("switch")

    if (speed == "off") {fanOff()}
    else if (speed == "low") {fanOnLow()}
    else if (speed == "medium") {fanOnMed()}
    else if (speed == "high") {fanOnHigh()}

    //else {setSpeed(speed)}
} 

def fanSwitchHandler(status, speed) {
    if (status == "off") {
        fanRelay.off()
    }
    else if (status == "on") {
        
    }
}

// set the fan hood fan to the driver speed from child device
def setSpeed(speed) {

    def action = ""
    if (speed == "low" && currentFanSpeed() != "low") {fanOnLow(); action = "low"}
    if (speed == "medium" && currentFanSpeed() != "medium") {fanOnMed(); action = "medium"}
    if (speed == "high" && currentFanSpeed() != "high") {fanOnHigh(); action = "high"}
    if (speed == "off" && currentFanSpeed() != "off") {fanOff(); action = "off"}
    
    // set light color with speed, if using indicator
    if (speedLight) {
        if (speed == "low") {
            speedLight.setColor("Yellow")  // yellow
        }
        if (speed == "medium") {
            speedLight.setColor("Orange")  // orange
        }
        if (speed == "high") {
            speedLight.setColor("Red")  // red
        }
        if (speed == "off") {
            speedLight.setColor("Black")  // none
        }
    }
}

// Turn on speed and turn other speeds off
def fanOnLow() {
    turnOffOtherSpeeds(false, true, true)
    pauseExecution(100)
    fanLow.on()
}

def fanOnMed() {
    turnOffOtherSpeeds(true, false, true) 
    pauseExecution(100)
    fanMedium.on()
}

def fanOnHigh() {
    turnOffOtherSpeeds(true, true, false)
    pauseExecution(100)
    fanHigh.on()
}

def fanOff() {
    turnOffOtherSpeeds(true, true, true)
}

def turnOffOtherSpeeds(low, medium, high) {

    if (low && fanLow.currentValue("switch") == "on") {fanLow.off()}
    if (medium && fanMedium.currentValue("switch") == "on") {fanMedium.off()}
    if (high && fanHigh.currentValue("switch") == "on") {fanHigh.off()}
}

// get current speed
String currentFanSpeed() {

    if (fanLow.currentValue("switch") == "on") {return "low"}
    if (fanMedium.currentValue("switch") == "on") {return "medium"}
    if (fanHigh.currentValue("switch") == "on") {return "high"}
    else {return "off"}
}

// Scene Switch Multi button Handler for fan speeds
def fanSceneSwitchHandler(evt) {
    logDebug("fanSceneSwitchHandler(${evt.value}) called")

    def pressed = evt.value.toInteger()
    def speed = ""

    if (pressed == 1) {speed = "off"} //turnFanOff();
    if (pressed == 2) {speed = "low"} //fanLow.on(); 
    if (pressed == 3) {speed = "medium"}//fanMedium.on(); 
    if (pressed == 4) {speed = "high"} //fanHigh.on();

    fanDriver.setSpeed(speed)
}

// activate fan power off switch (4th relay clears all speed relays on the board when on)
def turnFanOff() {
    fanOff()
}

// start timer when fan off relay triggers to off (meaning the fan was turned on)
def fanOffHandler(evt) {
    logDebug("fanOffHandler called with ${evt.value}")
    
    if (evt.value == "off") {         // fan On
        def timerMinutes = (settings?.autoOff).toInteger()
        logDebug("timerMinutes is ${timerMinutes}")
        if (settings?.useAutoOff) runIn(timerMinutes,turnFanOff)
        if (stoveLight && useStoveLight) {stoveLight.on()}
    }
    if (evt.value == "on") {        // Fan Off
        setSpeed("off")
        unschedule()
    }  
}


// ********** From Board Buttons (and RF remote) ***********
def fanLowHandler(evt) {
    logDebug("fanLowHandler called with ${evt.value}")

    if (evt.value == "on") {
        fanOnLow()
        fanDriver.setSpeedAttribute("low")
    }
    if (evt.value == "off" && checkOtherSpeedsOff("low")) {
        fanDriver.off()
    }
}

def fanMediumHandler(evt) {
    logDebug("fanHighHandler called with ${evt.value}")
    if (evt.value == "on") {
        fanOnMed()
        fanDriver.setSpeedAttribute("medium")
    }    
    if (evt.value == "off" && checkOtherSpeedsOff("medium")) {
        fanDriver.off()
    }    
}

def fanHighHandler(evt) {
    logDebug("fanHighHandler called with ${evt.value}")

    if (evt.value == "on") {
        fanOnHigh()
        fanDriver.setSpeedAttribute("high")
    }
    if (evt.value == "off" && checkOtherSpeedsOff("high")) {
        fanDriver.off()
    }    
}

def fanOscHandler(evt) {
    logDebug("fanOscHandler called with ${evt.value}")

    if (evt.value == "on") {
        fanDriver.setOscillateAttribute("on")
    }
    if (evt.value == "off") {
        fanDriver.setOscillateAttribute("off")
    }    
}

def checkOtherSpeedsOff(speed) {
    logDebug("checkOtherSpeedsOff(${speed}) called")
    def othersOff = false
    lowOff = fanLow.currentValue("switch") == "off"
    medOff = fanMedium.currentValue("switch") == "off"
    highOff = fanHigh.currentValue("switch") == "off"
    logDebug("lowOff=${lowOff} medOff=${medOff} highOff=${highOff}")

    if (speed == "low") {if (medOff && highOff) {othersOff = true}}
    if (speed == "medium") {if (lowOff && highOff) {othersOff = true}}
    if (speed == "high") {if (lowOff && medOff) {othersOff = true}}

    logDebug("returning ${othersOff}")
    return othersOff
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
