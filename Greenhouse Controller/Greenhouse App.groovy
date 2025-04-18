/**
 *  ****************  Greenhouse App ****************
 *
 *  Usage:
 *  This was designed to update a virtual greenHouse with humidity and temp from external sensors, and turn on a heater, heating pads, a circulator, and/or vent fan based on temp. 
 *  The temp and humidity sensors are both Outside and inside the GreenHouse.
 *  Uses companion driver Greenhouse Driver.  This app syncs the data to the driver, and controls the vent fan device switch and speed, a heater switch, a circulator fan switch,
 *  and a seedling heating pad switch
 *   
 *  Turns on/off and sets speed for the multi-speed fan device when the driver's operatingState changes to cooling, and turns on a heater based on setpoint when operatingState
 *  is heating. 
 *  
 *    
 * v. 1.0 3/16/25 - Initital code
 * v. 1.2 3/23/25 - Added Heater pad controls and added a setpoint to the driver to control when they turn on or off 
 * v. 1.3 3/26/25 - Added circulator fan control
 * v. 1.4 3/29/25 - Added state update for components in the driver if the physical device state changes (device is turned on/off directly)
 * v. 2.0 4/02/25 - The app now creates the greenhouse driver child device, and sets it as a setting as if were chosen manually with a user input.  Removed the subscriptions to 
                    the greenhouse driver attributes in the app, and added direct calls to the app parent menthods from this driver.
**/

definition (
    name: "Greenhouse App",
    namespace: "Hubitat",
    author: "C.Burgess",
    description: "Parent app for Greenjouse Driver Device to sync device changes and to control the devices based on changes in the driver",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {   
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {
    
    dynamicPage(name: "mainPage") {

        section("<b>Outside Humidity Sensor Device</b>") {
            input (
                name: "outsideHumidity", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Outside Humidity Sensor Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )          
        }

        section("<b>Outside Temperature Sensor Device</b>") {
            input (
                name: "outsideTemperature", 
                type: "capability.temperatureMeasurement", 
                title: "Select Outside Temperature Sensor Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )            
        }
        
        section("<b>GreenHouse Humidity Sensor Device</b>") {
            input (
                name: "greenHouseHumidity", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select GreenHouse Humidity Sensor Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )      
        }

        section("<b>GreenHouse Temperature Sensor Device</b>") {
            input (
                name: "temperature", 
                type: "capability.temperatureMeasurement", 
                title: "Select GreenHouse Temperature Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )         
        }

        section("<b>Green House Vent Fan Switch Device</b>") {
            input (
                name: "fan", 
                type: "capability.switch", 
                title: "Select Green House Vent Fan Switch Device", 
                required: true, 
                multiple: false
            )
        }

        section("<b>Green House Heater Switch Device</b>") {
            input (
                name: "heater", 
                type: "capability.switch", 
                title: "Select Green House Heater Switch Device", 
                required: false, 
                multiple: false
            )
        }

        section("<b>Green House Heat Pad Switch Device</b>") {
            input (
                name: "heaterPad", 
                type: "capability.switch", 
                title: "Select Green House Heat Pad Switch Device", 
                required: false, 
                multiple: false
            )
        }

        section("<b>Green House Circulator Fan Switch Device</b>") {
            input (
                name: "circulator", 
                type: "capability.switch", 
                title: "Select Green House Circulator Fan Switch Device", 
                required: false, 
                multiple: false
            )
        }

        section("<b>Log To Google Device</b>") {
            input (
                name: "googleLogs", 
                type: "capability.actuator",
                title: "Select Log to Google Device", 
                required: false, 
                multiple: false,
                submitOnChange: true
            )        
        }

        if (googleLogs) {
            section("") {
                input (
                    name: "googleLogging", 
                    type: "bool", 
                    title: "Enable Google logging", 
                    required: false, 
                    defaultValue: false
                )
            }
        }

        section("") {
            input (
                name: "errorCorrect", 
                type: "bool", 
                title: "Enable Error correcting at every temp change to check if devices are in the correct state (if they missed a command)", 
                required: true, 
                defaultValue: true
            )
        }

        section("") {
            input (
                name: "debugMode", 
                type: "bool", 
                title: "Enable logging", 
                required: true, 
                defaultValue: true
            )
        }
    }
}

def installed() {

    logDebug("installed() called")
    state.outsideTemp = 75.0
    state.temperature = 75.0
    state.operatingState = "idle"
    setGreenhouseDevice()
    updated()
}

def updated() {
    logDebug("updated() called")
    if (settings?.debugMode) runIn(3600, logDebugOff)   // one hour

    unsubscribe()
    initialize()
}

def initialize() {

    // device subscriptions to monitor device states
    if (outsideHumidity) subscribe(outsideHumidity, "humidity", setOutsideHumidity)
    if (greenHouseHumidity) subscribe(greenHouseHumidity, "humidity", setHumidity)
    if (outsideTemperature) subscribe(outsideTemperature, "temperature", setOutsideTemp)  
    if (circulator) subscribe(circulator, "switch", circulatorSwitchController) 
    if (heaterPad) subscribe(heaterPad, "switch", heatPadsSwitchController) 

    subscribe(temperature, "temperature", setTemperature) 
    subscribe(fan, "switch", fanSwitchController) 
    subscribe(fan, "speed", fanSpeedChangeController)

}

def setGreenhouseDevice() {
    logDebug("settingGreenhouseDevice")
    if (!greenhouseDriver) {
        def ID = createGreenhouseDevice()
        app.updateSetting("greenhouseDriver",[value:getChildDevice(ID),type:"capability.actuator"])
    }
}

def createGreenhouseDevice() {
    logDebug("createGreenhouseDevice() called")
    def deviceNetworkId = "CD_${app.id}_${new Date().time}"
    
    try {
        def greenhouseDevice = addChildDevice(
            "Hubitat", 
            "Greenhouse Driver", 
            deviceNetworkId,
            null,
            [
                name: "Greenhouse",
                label: "Greenhouse",
                isComponent: false
            ]
        )
        
        logDebug("Created greenhouse device in 'Hubitat' using driver 'Greenhouse Driver' (DNI: ${deviceNetworkId})")

        state.greenhouseDevice = deviceNetworkId
        return deviceNetworkId

    } catch (Exception e) {
        log.error "Failed to create greenHouse device: ${e}"
    }
}

// ** Log to Google - Requires the log to google device**
def logToGoogle() {
    logDebug("logToGoogle Called")
    if (googleLogging) {

        def onHeat = 0
        def onCool = 0
        def onPad = 0

        def temperature = greenhouseDriver.currentValue("temperature")
        def greenHouseHumidity = greenhouseDriver.currentValue("humidity")
        def heatPadState = greenhouseDriver.currentValue("heatPadState")      
        def outsideTemp = greenhouseDriver.currentValue("outsideTemp")
        def power = heater.currentValue("energy")
        def heatPadSetpoint = greenhouseDriver.currentValue("heatPadSetpoint")
        def coolingSetpoint = greenhouseDriver.currentValue("coolingSetpoint")
        def heatingSetpoint = greenhouseDriver.currentValue("heatingSetpoint")
        def operatingState = greenhouseDriver.currentValue("operatingState")
        if (operatingState == "heating") {onHeat = heatingSetpoint}
        if (operatingState == "venting") {onCool = coolingSetpoint}
        if (heatPadState == "on") {onPad = heatPadSetpoint}

        def logParams = "Temperature="+temperature+"&Humidity="+greenHouseHumidity+"&On Heat="+onHeat+"&On Cool="+onCool+"&Heat Point="+heatingSetpoint+"&Cool Point="+
        coolingSetpoint+"&On Pad="+onPad+"&Pad Point="+heatPadSetpoint+"&Outside Temp="+outsideTemp+"&Power="+power
        googleLogs.sendLog("Greenhouse", logParams) 
        logDebug("Log Sent")
    }
}

// ************* Update states in Driver when heating and cooling devices change state ***************

// update pads state in greenhouse controller when switch changes
def heatPadsSwitchController(evt) {
    logDebug("Heat pads switch changed to ${evt.value}")
    def padState = greenhouseDriver.currentValue("heatPadState")
    if (evt.value == "on" && padState == "off") {
        greenhouseDriver.setHeatPadState("on")
    } else if (evt.value == "off" && padState == "on") {       
        greenhouseDriver.setHeatPadState("off")
    }
}

// update vent fan state in greenhouse controller when switch device changes
def fanSwitchController(evt) {
    logDebug("Vent Fan switch changed to ${evt.value}")
    def fanState = greenhouseDriver.currentValue("fanState")
    if (evt.value == "on" && fanState == "off") {
        logDebug("Updating vent fan state in driver to on")
        greenhouseDriver.setFanState("on") 
    } 
    if (evt.value == "off" && fanState == "on") {greenhouseDriver.setFanState("off")}
}

// update vent fan state in greenhouse controller when switch device changes
def fanSpeedChangeController(evt) {
    def newSpeed = evt.value
    logDebug("Vent Fan speed device changed to ${newSpeed}")
    def fanSpeed = greenhouseDriver.currentValue("fanSpeed")
    if (newSpeed != fanSpeed) {
        logDebug("Updating vent fan speed attribute in driver to ${newSpeed} to match state")
        greenhouseDriver.setFanSpeed(newSpeed) 
    } 
}

// update circulator state in greenhouse controller when switch changes
def circulatorSwitchController(evt) {
    logDebug("Circulator switch changed to ${evt.value}")
    def circState = greenhouseDriver.currentValue("circulatorState")
    if (evt.value == "on" && circState == "off") {
        greenhouseDriver.setCirculatorState("on")
    } else if (evt.value == "off" && circState == "on") {greenhouseDriver.setCirculatorState("off")}
}

// *********** Update device Values in Driver when app sensor devices update **************

// Humidity sensor event - update driver
def setHumidity(evt) {

    logDebug("Humidity Sensor Event = ${evt.value}")
    def lvl = evt.value   //.toInteger()

    greenhouseDriver.setHumidity(lvl) 
    //runIn(1,logToGoogle)
}

// Outside temp event - update driver
def setOutsideTemp(evt) {
    
    logDebug("Outside Temp Event = ${evt.value}")
    def temp = evt.value.toBigDecimal()

    if (temp != state?.outsideTemp) {
        greenhouseDriver.setOutsideTemp(temp)  
    } 
    state.outsideTemp = evt.value.toBigDecimal()
}

// Temperature sensor event - update driver and log
def setTemperature(evt) {
    
    logDebug("GreenHouse Temp Event = ${evt.value}")
    def temp = evt.value.toBigDecimal()

    if (temp != state?.temperature) {
        greenhouseDriver.setTemperature(temp) 
        runIn(1,logToGoogle)
    }
    state.temperature = temp
  
    if (errorCorrect) {
        def opState =greenhouseDriver.currentValue("operatingState") 
        if (opState == "idle") runIn(1,checkIdleError)
        if (opState == "heating") runIn(1,checkHeatError)
        if (opState == "venting") runIn(1,checkVentError)
        runIn(2, checkCirculatorError)
    }
}

// ******** Error checkers run when temp changes in greenhouse ************
def checkIdleError() {
    def ventSwitch = fan.currentValue("switch")
    def heaterSwitch = heater.currentValue("switch")

    if (heaterSwitch == "on" ) {
        logDebug("Fixing heater to be off")
        heater.off()
    }
    if (ventSwitch == "on" ) {
        fan.off()
    }
}
def checkCirculatorError() {
    def circulatorSwitch = circulator.currentValue("switch")
    def circState = greenhouseDriver.currentValue("circulatorState")
    if (circState == "on" && circulatorSwitch == "off") {
        circulator.on()
    }
    if (circState == "off" && circulatorSwitch == "on") {
        circulator.off()
    }    
}
def checkHeatError() {
    def heaterSwitch = heater.currentValue("switch")
    logDebug("Checking for heating error, heater switch is ${heater}")
    if (heaterSwitch == "off" ) {
        logDebug("Fixing heater to be on")
        heater.on()
    }

}
def checkVentError() {
    def ventSwitch = fan.currentValue("switch")
    logDebug("Checking for venting error, vent fan switch is ${ventSwitch}")
    if (ventSwitch == "off") {
        fan.on()
    }

}
// Outside humity sensor event - update driver
def setOutsideHumidity(evt) {

    logDebug("Outside Humidity Event = ${evt.value}")
    def lvl = evt.value.toInteger()

    greenhouseDriver.setOutsideHumidity(lvl)  
}


// ************ Change the state of the heat/cool devices when sent commands from the driver ***************

// control pads switch from greenhouse pads state
def heatPadsStateController(value) {
    logDebug("Heater Pad state changed to ${value}")
    if (value == "on") {
        heaterPad.on()
        logDebug("Turning heat pads on")
    } else {heaterPad.off()}
}


// change circulator switch when greenhouse circulator state changes
def circulatorStateController(cState) {
    logDebug("Circulator state changed to ${cState}")
    def circSwitch = circulator.currentValue("switch")
    logDebug("State changed to ${cState} and switch is ${circSwitch}") 
    if (cState == "on" && circSwitch == "off" ) {      
        logDebug("Turning circulator fan off")
        circulator.on()
        runInMillis(100,circulatorOn)
    } else if (cState == "off" && circSwitch == "on") {  
        logDebug("Turning circulator fan off")
        circulator.off()
        runInMillis(100,circulatorOff)
    }
}

def circulatorOn() {circulator.on()}
def circulatorOff() {circulator.off()}

// Update the fan speed on device when fans speed changes in the driver
def fanSpeedController(speed) {
    logDebug("Fan speed changed to ${speed}")
    if (fan.currentValue("switch") != "off") {fan.setSpeed(speed)}
}

// ****** Change the state of heater and vent fan devices when operating state changes from driver *******
def operatingStateController(opState) {
    def last = state?.operatingState
    def operatingState = opState  //evt.value

    logDebug("GreenHouse Operating State Event = ${operatingState}, was ${last}")

    if (operatingState == "venting") {
        fanOn()  
        runIn(1,setFanSpeed)
    }
    if (operatingState == "heating") {
        heatOn()
    }
    if (operatingState == "idle") {
        heatOff()
        fanOff()
        runIn(1,setFanSpeed)
    }
    runIn(1,logToGoogle)
    state.operatingState = opState
}

// set driver fanSpeed Attribute
def setFanSpeed() {
    greenhouseDriver.setFanSpeed(fan.currentValue("speed"))
}

// return fan speed to the device
def getFanSpeed() {
    return(fan.currentValue("speed"))
}

// on/off device commands for operating state changes
def fanOff() {
    logDebug("Turning vent fan off")
    fan.off()
}
def fanOn() {
    logDebug("fanOn() called")
    logDebug("Turning vent fan on")
    fan.on()
}
def heatOff() {
    logDebug("Turning heater off")
    heater.off()
}
def heatOn() {
    logDebug("Turning heater on")
    heater.on()
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("debugMode",[value:"false",type:"bool"])
}