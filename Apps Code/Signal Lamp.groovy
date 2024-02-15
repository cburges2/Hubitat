/**
 *  ****************  Signal Lamp  ****************
 *
 *  Usage:
 *  This was designed to sync an RGB bulb to outdoor temperture by changing hue
 *  
 *  
 *  
 *  
**/
import groovy.json.JsonSlurper

definition (
    name: "Signal Lamp",
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

        section("<b>RGB Bulb Device</b>") {
            input (
              name: "rgbBulb", 
              type: "capability.colorControl", 
              title: "Select Color Control Bulb", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Weather Station</b>") {
            input (
              name: "weatherStation", 
              type: "capability.temperatureMeasurement", 
              title: "Select Weather Station Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }  

        section("<b>Illuminance Data Device</b>") {

            input (
              name: "illuminanceData", 
              type: "capability.actuator",  
              title: "Select Illuminance Data Device", 
              required: true, 
              multiple: false,     
              submitOnChange: true
            )
        }

        section("<b>Signal Lamp Driver</b>") {
            input (
              name: "signalDriver", 
              type: "capability.actuator", 
              title: "Select Signal Lamp Driver Device", 
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

        section("") {
            input (
                name: "logLevel",
                type: "enum",
                title: "<font style='font-size:14px; color:#1a77c9'>Logging Level</font>",
                options: [1:"Info", 2:"Warning", 3:"Debug"],
                multiple: false,
                defaultValue: 2,
                required: true
            )
        }        
    }
}

def installed() {

  initialize()
  state.green = 120
  state.orange = 30
  state.blue = 240
  state.red = 0
  state.purple = 264

}

def updated() {

    if (settings?.debugMode) {
        if (settings?.logLevel == "3") {
            runIn(3600, logDebugOff)   // one hour
            logDebug("Debug log level enabled",3)
            logDebug("Log Level will change from Debug to Info after 1 hour",2)
        } else if (settings?.logLevel == "1") {
            logDebug("Info logging Enabled",1)
        } else logDebug("Warning log level enabled",2)
    }
    initialize()

}
def initialize() {

    subscribe(weatherStation, "temperature", tempColorHandler)
    subscribe(illuminanceData, "lightIntensity", intensityLevelController)
    subscribe(signalDriver, "appAction", appActionHandler)
    //schedule('0 30 9 * * ?', scheduleHandler)   

    state.tempHue = 180
    state.luxLevel = 50

    state.green = 120
    state.orange = 26
    state.blue = 235
    state.red = 10
    state.purple = 250
    state.violet = 275
    state.yellow = 42
}

def setSchedule(evt) {
    logDebug("Setting Schedule...")
    def scheduler = evt.value.toString()
    schedule('${scheduler}', scheduleHandler)
}

// Handle actions set by driver
def appActionHandler(evt) {

    if (evt.value != "Idle") {

        String jsonText = evt.value.toString()
        parser = new groovy.json.JsonSlurper() 
        def data = parser.parseText(jsonText)
        logDebug("json data is ${data}")

        // perform action
        if (data.name == "Color Fade") {
            logDebug ("Action Receved is Color Fade")
            colorFade(data.number.toInteger(),data.color,data.seconds.toInteger())
        } else if (data.name == "Color Flash") {
            logDebug ("Action Receved is Color Flash")
            colorFlash(data.number.toInteger(), data.color,data.seconds.toInteger())
        } else if (data.name == "Level Fade") {
            logDebug ("Action Receved is Level Fade")
            levelFade(data.number.toInteger(),data.level.toInteger(),data.seconds.toInteger())
        } else if (data.name == "White Flash") {
            logDebug ("Action Receved is White Flash")
            whiteFlash(data.number.toInteger(),data.seconds.toInteger())
        } else if (data.name == "Flash") {
            logDebug ("Action Receved is Flash")
            flash(data.number.toInteger(), data.seconds.toInteger())  
        } else if (data.name == "Level Color Fade") {
            logDebug ("Action Receved is Level Color Fade")
            levelColorFade(data.number.toInteger(),data.level.toInteger(),data.color,data.seconds.toInteger())                      
        } else if (data.name == "Temperature Hue") {
            logDebug ("Action Receved is Temperature Hue")
            setTempColor()
        } else if (data.name == "Lux Level") {
            logDebug ("Action Receved is Lux Level")
            setIntensityLevel()        
        } else if (data.name == "Color Wheel") {
            logDebug ("Action Receved is Color Wheel")
            colorWheel(data.spins.toInteger(),data.speed.toInteger(),data.run)                
        } else logDebug ("Action Receved is Invalid ${evt.value}")
  
    }
}

// fade to another color and back, number times
def colorFade(number, color, seconds) {
    def currentHue = rgbBulb.currentValue("hue").toInteger()
    def millis = seconds * 1000
    logDebug("${color} ${number} ${currentHue}")

    for (f=0; f < number; f++) {
        rgbBulb.setHue(getHue(color))
        pauseExecution(millis)
        rgbBulb.setHue(currentHue)
        pauseExecution(millis)
    }
    rgbBulb.setHue(state?.tempHue)
}

// turn off then flash a color number times
def colorFlash(number, color, seconds) {
    def millis = seconds * 1000
    def level = rgbBulb.currentValue("level").toInteger()
    logDebug("${color} ${number} ${currentHue}")

    rgbBulb.off()
    pauseExecution(100)   
    rgbBulb.setHue(getHue(color))  
    for (f=0; f < number; f++) {
        rgbBulb.on()
        pauseExecution(millis)
        rgbBulb.off()
        pauseExecution(1000) 
    }
    rgbBulb.on()
    pauseExecution(millis)
    rgbBulb.setLevel(level)
    rgbBulb.setHue(state?.tempHue)  
}

// fade level number times.  Brigher or dimmer based on current level
def levelFade(number,level,seconds) {
    def currentLevel = rgbBulb.currentValue("level").toInteger()
    logDebug("Fade number is ${number}")

    fadeLevel(number,level,seconds)
}

// fade level number times.  Brigher or dimmer based on current level
def levelColorFade(number,level,color,seconds) {
    logDebug("${number} ${level} ${color} ${seconds}")
    def millis = seconds * 1000
    logDebug("Fade number is ${number}")

    rgbBulb.setHue(getHue(color))
    fadeLevel(number,level,seconds)
    pauseExecution(millis)
    setTempColor()
}

def fadeLevel(number, signalLevel, seconds) {
    def currentLevel = rgbBulb.currentValue("level").toInteger()
    def millis = seconds * 1000

    for (f=0; f < number; f++) {
        rgbBulb.setLevel(signalLevel)
        pauseExecution(millis)
        rgbBulb.setLevel(currentLevel)
        pauseExecution(millis)
    }
    rgbBulb.setLevel(state?.luxLevel)
}

// Flash white number times
def whiteFlash(number, seconds) {
    def millis = seconds * 1000
    for (f=0; f < number; f++) {
        rgbBulb.setColorTemperature(2500)
        pauseExecution(millis)
        rgbBulb.setHue(state?.tempHue)
        pauseExecution(millis)
    }
    setTempColor() // Done - return to temp color
}

// Flash current color number times
def flash(number,seconds) {
    def millis = seconds * 1000

    for (f=0; f < number; f++) {
        rgbBulb.on()
        pauseExecution(millis)
        rgbBulb.off()
        pauseExecution(1000)
    }
    rgbBulb.on()
    setTempColor() // Done - return to temp color
}

// Color Wheel
def colorWheel(spins, speed, run) {

    if (run == "start") {
        state.spinCount = 0
        state.spins = spins
        state.wheelSpeed = speed
        state.wheelHue = 1
        startColorWheel()
    } else if (run == "stop") {
        unschedule("setWheelHue")
        setTempColor() // Done - return to temp color
    }
}

// Start Color wheel timer between color cahnges
def startColorWheel() {
    if (state?.wheelHue < 361) {
        runIn(1,setWheelHue)
    }
    if (state?.wheelHue >= 360) {
        pauseExecution(1000)
        state.spinCount = state?.spinCount + 1
        if (state?.spinCount < state?.spins) {
            state.wheelHue = 1
            spinAgain()
        }
        else setTempColor() // Done - return to temp color
    }
}

def spinAgain() {
    startColorWheel()
}

// set the color wheel hue and start again
def setWheelHue() {
    def hue = state?.wheelHue
    def step = state?.wheelSpeed
    rgbBulb.setHue(hue)
    state.wheelHue = state?.wheelHue + step
    startColorWheel()
}

// get the hue value for color string
int getHue(color) {

    def hue = 0
    if (color == "Red") hue = state?.red
    else if (color == "Orange") hue = state?.orange
    else if (color == "Blue") hue = state?.blue
    else if (color == "Green") hue = state?.green
    else if (color == "Purple") hue = state?.purple
    else if (color == "Yellow") hue = state?.yellow
    else if (color == "Violet") hue = state?.violet
    else logDebug("No hue for ${color}")

    logDebug("Hue for ${color} is ${hue}")
    return hue

}

// trigger temp color from outdoor temp change
def tempColorHandler(evt) {
    logDebug("tempColorhandler Called ${evt.value}")
    setTempColor()
}

// set hue based on outside temperature
def setTempColor() {

    double temp = weatherStation.currentValue("temperature")
    //double temp = evt.value

    def percentInTempRange = (((temp + 20) * 100.0) / 120)
    logDebug("% in Range is ${percentInTempRange}")
    def percentHue = (percentInTempRange * 360.0 / 100.0)
    def tempHue = 360 - percentHue
    //tempHue = tempHue.round()
    def hue = tempHue.toInteger()

    logDebug("hue is ${hue}")

    rgbBulb.setHue(hue)
    state.tempHue = hue
}

// set Level to light intensity
def intensityLevelController(evt) {
    logDebug("intensityLevelController Called ${evt.value}")

    state.luxLevel = evt.value.toInteger()
    setIntensityLevel()
}

def setIntensityLevel() {

    rgbBulb.setLevel(state?.luxLevel)
}

def testColorHandler(evt) {
}
    
// log debug if no logLevel added
def logDebug(txt) {
    try {
        if (settings?.debugMode) {
            log.debug("${app.label} - ${txt}")   // debug
        }
    } catch(ex) {
        log.error("bad debug message")
    }    
}

// log by level when lvl supplied
def logDebug(txt, lvl){
    try {
        logLevel = settings?.logLevel.toInteger()
        if (settings?.debugMode) {
            if (lvl == 3 && logLevel == 3) log.debug("${app.label} - ${txt}")       // debug
            else if (lvl >= 2 && logLevel >= 2) log.warn("${app.label} - ${txt}")   // warn
            else if (lvl >= 1 && logLevel >= 1) log.info("${app.label} - ${txt}")   // info
        }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("logLevel",[value:"1",type:"enum"])
}