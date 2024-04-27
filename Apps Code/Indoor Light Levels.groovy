/**
 *  ****************  Indoor Light Levels App  ****************
 *
 *  Usage:
 *  This was designed to use an indoor light sensor to set a room light level based on a target dimmer device to set the level. 
 *  
**/
definition (
    name: "Indoor Light Levels",
    namespace: "Hubitat",
    author: "Burgess",
    description: "",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Living Room Light Sensor</b>") {

            input (
              name: "lightSensor", 
              type: "capability.illuminanceMeasurement", 
              title: "Select Light Sensor Devices", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
            if (lightSensor) {
                input (
                    name: "trackLightSensor", 
                    type: "bool", 
                    title: "Track Light Sensor Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 

        }

        section("<b>Auto Light Target Dimmer</b>") {
            input (
              name: "lightTarget", 
              type: "capability.switch", 
              title: "Select Light Target Dimmer", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )

            if (lightTarget) {
                input (
                    name: "trackLightTarget", 
                    type: "bool", 
                    title: "Track Light Target Dimmer", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }

        section("<b>Front Sensor Data Device</b>") {
            input (
              name: "frontData", 
              type: "capability.actuator", 
              title: "Select Front Sensor Data Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )     
        }

         section("<b>Illuminance Data Device</b>") {
            input (
              name: "illuminanceData", 
              type: "capability.actuator", 
              title: "Select Illuminance Data Actuator Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }        

         section("<b>Scene Activator</b>") {
            input (
              name: "sceneActivator", 
              type: "capability.actuator", 
              title: "Select Scene Activator Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }   

        section("Target Inputs:") {
            input (
                name: "dawnTarget", 
                type: "number", 
                title: "Dawn Target", 
                required: true, 
                defaultValue: false
            )
            input (
                name: "morningTarget", 
                type: "number", 
                title: "Morning Target", 
                required: true, 
                defaultValue: false
            )
            input (
                name: "dayTarget", 
                type: "number", 
                title: "Day Target", 
                required: true, 
                defaultValue: false
            )
            input (
                name: "eveningTarget", 
                type: "number", 
                title: "Evening Target", 
                required: true, 
                defaultValue: false
            )
            input (
                name: "dinnerTarget", 
                type: "number", 
                title: "Dinner Target", 
                required: true, 
                defaultValue: false
            )
            input (
                name: "lateEveningTarget", 
                type: "number", 
                title: "Late Evening Target", 
                required: true, 
                defaultValue: false
            )
            input (
                name: "tvTimeTarget", 
                type: "number", 
                title: "TV Time Target", 
                required: true, 
                defaultValue: false
            )
            input (
                name: "brightTarget", 
                type: "number", 
                title: "Bright Target", 
                required: true, 
                defaultValue: false
            )
            input (
                name: "dimTarget", 
                type: "number", 
                title: "Dim Target", 
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
   initialize()
}

def updated() {

    if (settings?.debugMode) runIn(3600, logDebugOff)   // one hour
    initialize()

}
def initialize() {
    subscribe(lightSensor, "illuminance", illuminanceController)
    subscribe(lightTarget, "switch", targetSwitchController)
    subscribe(llghtTarget, "level", targetLevelController)

    subscribe(frontData, "frontMode", lightTargetHandler)

    state.lastLux = lightSensor.currentValue("illuminance")
    state.lastHigh = illuminanceData.currentValue("highLightLevel")
    startCheckTimer()
    runIn(10,runAutoAdjust)
}

// Auto target Adjust
def autoTargetAdjust() {
    logDebug("Auto Target Adjust Running")
    def frontMode = frontData.currentValue("frontMode")
    def target = lightTarget.currentValue("level").toInteger()
    def light = lightSensor.currentValue("illuminance").toInteger()

    if ((frontMode == "Dawn" || frontMode == "Morning" || frontMode == "Day") && target <= 10) { 
        lightTarget.setLevel(light + 1)
    }
    if (frontMode == "Morning" && target <= 35) {
        if ((light > target + 2) && light < 34) {
            lightTarget.setLevel(light + 2)
        } else lightTarget.setLevel(light + 1)
    }
    if (frontMode == "Day" && target <= 35)  {
        if (light > target + 1 && light <= 34) lightTarget.setLevel(light) 
        else if (light <= 35 && light > target) {
            lightTarget.setLevel(light)
            if (light >= 31 && light <=35 && light <= target) lightTarget.setLevel(light)
        }
    }
    runAutoAdjust()
}

// auto adjust running
def runAutoAdjust() {
    runIn(300, autoTargetAdjust)
}

// set Light Target on Front Scene Change
def lightTargetHandler(evt) {
    logDebug("llightTargetHandler called with ${evt.value}")

    def target = 0
    def frontMode = evt.value

    if (frontMode == "Dawn") target = settings?.dawnTarget.toInteger()
    else if (frontMode == "Morning") target = settings?.morningTarget.toInteger()
    else if (frontMode == "Day") target = settings?.dayTarget.toInteger()
    else if (frontMode == "Evening") target = settings?.eveningTarget.toInteger()
    else if (frontMode == "Dinner") target = settings?.dinnerTarget.toInteger()
    else if (frontMode == "Late Evening") target = settings?.lateEveningTarget.toInteger()
    else if (frontMode == "TV Time") target = settings?.tvTimeTarget.toInteger()
    else if (frontMode == "Bright") target = settings?.brightTarget.toInteger()
    else if (frontMode == "Dim") target = settings?.dimTarget.toInteger()

    pauseExecution(5000)
    if (target != 0) lightTarget.setLevel(target)
    logDebug("Light Target Updated to ${target}")
}


def illuminanceController(evt) {
    // light changed - calc
    illuminanceData.setIndoorIlluminance(evt.value)
    runIn(15,checkLightLevels)
}


def targetSwitchController(evt) {
    // if on, set target and calc
    if (evt.value == "on") {
        logDebug("Target switch turned ${evt.value}")
        pauseExecution(3000)
        runIn(15,checkLightLevels)
    }
}

def targetLevelController(evt) {
    logDebug("Target Changed to ${evt.value}")
    illuminanceData.setLightTarget(evt.value)
    runIn(5,checkLightLevels)
}

// check light levels - runs every 65 seconds
def checkLightLevels() {
    logDebug("Checking Light Levels")

    def light = lightSensor.currentValue("illuminance").toInteger()
    def level = lightTarget.currentValue("level").toInteger()
    def lowLevel = illuminanceData.currentValue("lowLightLevel").toInteger()
    def highLevel = illuminanceData.currentValue("highLightLevel").toInteger()

    if ((light > level && lowLevel > 1) || (light < level && highLevel < 100)) {
        calcLightLevels()
        logDebug("Calling Calcs")
    } else logDebug("No Calcs needed")
    startCheckTimer()
}

def startCheckTimer() {
    runIn(65,checkLightLevels)
}

// Calculate level changes

def calcLightLevels() {
    
    def mode = frontData.currentValue("frontMode")

    // check Conditions for rooms update
    def modeOK =  (mode != "Night" && mode != "TV Time")
    def targetOn = lightTarget.currentValue("switch") == "on"
    def sensorOn = frontData.currentValue("frontSensor") == "On"

    if (modeOK && targetOn && sensorOn) {
        logDebug("Calculating Light Levels")
        def currentLux = lightSensor.currentValue("illuminance")
        def target = lightTarget.currentValue("level").toInteger()
        def highLevel = illuminanceData.currentValue("highLightLevel").toInteger()
        def updateLevels = false

        Float luxDiff = new Float(target - currentLux)
        def changeFactor = Math.abs(luxDiff * 2)
        logDebug("changeFactor is ${changeFactor}")

        // set changeFactor
        if (changeFactor > 20) changeFactor = 20
        logDebug("Set changeFactor is ${changeFactor}")

        // Decrease Levels
        if (highLevel != 0 && currentLux > target) {  //changeDiff < 0-swing && 
            logDebug("Decreaseing Levels")
            if (changeFactor > highLevel) changeFactor = highLevel
            if (highLevel >= changeFactor) {
                highLevel = highLevel - changeFactor
                logDebug("New High Level Decrease is ${highLevel}")
                updateLevels = true
                if (highLevel - changeFactor > 0) changeFactor = highLevel 
            }
        } else logDebug("Levels do not need decrease")

        // Increase Levels
        if (highLevel != 100 && currentLux < target) { //changeDiff >= swing && 
            logDebug("Increaseing Levels")
            if (highLevel < 100) {
                if ((highLevel + changeFactor) > 100) {
                    changeFactor = 100 - highLevel
                }
            }    
            highLevel = highLevel + changeFactor
            logDebug("New High Level Increade is ${highLevel}")
        
            if (highLevel > 100) highLevel = 100
            updateLevels = true
        } else logDebug("Levels do not need increase")

        // Update Levels
        if (updateLevels && state?.lastHigh != highLevel) {  

            logDebug("Updating Light Levels")
            def lowLevel
            def medLevel

            // zero will turn off device, causing a level burst when turned back on
            if (highLevel == 0) {
                lowLevel = 1
                medLevel = 1
                highLevel = 1
            } else {
                Float lowFloat = new Float(highLevel * 0.4)
                Float medFloat = new Float(highLevel * 0.6)
                lowLevel = Math.round(lowFloat)
                medLevel = Math.round(medFloat)
            }
            if (lowLevel == 0) lowLevel = 1

            logDebug("highLevel is ${highLevel.toInteger()}")
            logDebug("medLevel is ${medLevel}")
            logDebug("lowLevel is ${lowLevel}")

            // Update Illuminance Data
            illuminanceData.setLowLightLevel(lowLevel)
            illuminanceData.setMedLightLevel(medLevel)
            illuminanceData.setHighLightLevel(highLevel.toInteger())
            illuminanceData.setLightLevels()
            state.lastHigh = highLevel

            // update rooms if Active
            def frontMode = frontData.currentValue("frontMode")
            def kitchenActive = frontData.currentValue("kitchenMotionStatus") != "Timeout"
            def livingRoomActive = frontData.currentValue("livingRoomMotionStatus") != "Timeout"     
            def druNotHome = frontData.currentValue("druHome") == "No"       

            logDebug("Front Mode is ${frontMode}")
            logDebug("Dru Not Home is is ${druNotHome}")
            if (kitchenActive && druNotHome) {
                logDebug("Kitchen Being Updated")    // trigger kitchen
                sceneActivator.setKitchenScene(frontMode)
            } else {logDebug("Kitchen is not Active or Dru Home")}
            pauseExecution(500)
            if (livingRoomActive && druNotHome) {
                logDebug("Living Room Being Updated")  // trigger Living Room
                sceneActivator.setLivingRoomScene(frontMode)
            } else {logDebug("Living Room is not Active or Dru Home")}
            if (targetOn) runIn(45,checkLightLevels)
        } else logDebug("Levels do not need updating")

    } else {
        logDebug("Not updating during ${mode} mode")
        if (targetOn && sensorOn) runIn(45,checkLightLevels)         
    }
}

def logDebug(txt){
    try {
        if (settings?.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("debugMode",[value:"false",type:"bool"])
}