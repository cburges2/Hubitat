/**
 *  ****************  Dual Window Curtains Parent App  ****************
 *
 *  Usage:
 *  Controller for one virtual window shade device to control two curtains on one window
**/

definition (
    name: "Dual Window Curtains Parent App",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for one virtual window shade device to control two curtains on one window",
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

        section("<b>Left Curtain Device</b>") {
            input (
              name: "leftShade", 
              type: "capability.windowShade", 
              title: "Select Left Windowshade Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )     
        }   

        section("<b>Right Curtain Device</b>") {
            input (
              name: "rightShade", 
              type: "capability.windowShade", 
              title: "Select Right Windowshade Device", 
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
    updated()
}

def updated() {

    unsubscribe()

    if (!curtainDevice) {setCurtainDevice()}

    initialize()
}

def initialize() {
   
   subscribe(leftShade, leftShadeHandler)
   subscribe(rightShade, rightShadeHandler)
}    

def leftShadeHandler(evt) {
    logDebug("leftShadeHandler called with ${evt.name} and ${evt.value}")

    if (evt.name == "position" || evt.name == "level") {
         updateDriver(evt.name, evt.value)
         if (evt.name == "position") updateDriver("positionLeft", evt.value)
    }
    else { // if(rightShade.currentValue(evt.name) == evt.value) {
        pauseExecution(50)
        updateDriver(evt.name, evt.value) // update device when attribute value of both shades mataches
    } 
}

def rightShadeHandler(evt) {
    logDebug("rightShadeHandler called with ${evt.name} and ${evt.value}")

    if (evt.name == "position" || evt.name == "level") {
         updateDriver(evt.name, evt.value) 
         if (evt.name == "position") updateDriver("positionRight", evt.value)       
    }
    else {//if (leftShade.currentValue(evt.name) == evt.value) {
        pauseExecution(50)
        updateDriver(evt.name, evt.value)
    }
}

def updateDriver(name, value) {
    logDebug("updateDriver called with ${name} and ${value}")

    if (curtainDevice.currentValue(name) != value) {
        curtainDevice.eventSend(name, value)
    }   
}

def setCurtainDevice() {
    logDebug("settingCurtainDevice")
    if (!curtainDevice) {
        def ID = createCurtainDevice()
        app.updateSetting("curtainDevice",[value:getChildDevice(ID),type:"capability.actuator"])
    }
}

def createCurtainDevice() {
    logDebug("createCurtainDevice() called")
    def deviceNetworkId = "CD_${app.id}_${new Date().time}"
    
    try {
        def curtainDevice = addChildDevice("Hubitat", "Dual Window Curtains Device", deviceNetworkId, null, [name: "Dual Window Curtain Device",label: "Curtains", isComponent: false])        
        logDebug("Created curtain device in 'Hubitat' using driver 'Dual Window Curtains Device' (DNI: ${deviceNetworkId})")
        state.curtainDevice = deviceNetworkId
        return deviceNetworkId
    } catch (Exception e) {
        log.error "Failed to create Curtain Device: ${e}"
    }
}

def off() {
    rightShade.off()
    leftShade.off()
}

def on() {
    rightShade.on()
    leftShade.on()
}

def open() {
    rightShade.open()
    leftShade.open()
}

def close() {
    rightShade.close()
    leftShade.close()
}

def stepOpen(steps) {
    rightShade.stepOpen(steps)
    leftShade.stepOpen(steps)
}

def stepClose(steps) {
    rightShade.stepClose(steps)
    leftShade.stepClose(steps)
}

def setPosition(position) {
    logDebug("setPositon(${position}) was called)")
    rightShade.setPosition(position)
    leftShade.setPosition(position)
}

def startPositionChange(direction) {
    rightShade.startPositionChange(direction)
    leftShade.startPositionChange(direction)
}

def stopPositionChange() {
    rightShade.stopPositionChange()
    leftShade.stopPositionChange()
}

def setLevel(value, rate = null) {
    rightShade.setLevel(value)
    leftShade.setLevel(value)
}

def setSpeed(speed) {
    rightShade.setSpeed(speed)
    leftShade.setSpeed(speed)
}


def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}