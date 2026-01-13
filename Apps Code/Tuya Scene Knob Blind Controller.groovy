/**
 *  **************** Tuya Scene Knob Blind Controller  ****************
 *
 *  Usage:
 *  This app links a Tuya Scene Knob to a blind device 
 *  The Scene Knob should be using @kkossev's Tuya Zigbee Scene Switch TS004F driver, in Scene Mode. Position Change Steps setting determines how much granularity 
 *       the knob has for changing the blind position. 
 *
**/

definition (
    name: "Tuya Scene Knob Blind Controller",
    namespace: "Hubitat",
    author: "cburgess",
    description: "Controller for a Tuya Scene Knob to control a Window Shade Device",
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

        section("<b>Tuya Scene Knob</b>") {

            input (
              name: "sceneKnob", 
              type: "capability.actuator", 
              title: "Select Tuya Scene Knob Device", 
              required: true, 
              multiple: false           
            )
        }    

        section("<b>Window Shade Device</b>") {

            input (
              name: "blind", 
              type: "capability.windowShade", 
              title: "Select Window Shade Device", 
              required: true, 
              multiple: false           
            )
        } 

        section("") {
            input (
                name: "positionChangeDuration", 
                type: "number", 
                title: "Milliseconds for position change per knob click", 
                required: true, 
                defaultValue: 400
            )
        }

        section("") {
            input (
                name: "closedPosition", 
                type: "number", 
                title: "Postion when shade is closed", 
                required: true, 
                defaultValue: 20
            )
        }

        section("") {
            input (
                name: "partialPosition", 
                type: "number", 
                title: "Postion when shade is half open", 
                required: true, 
                defaultValue: 60
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
    if (debugMode) runIn(1800,logsOff)

    initialize()
}

def initialize() {

    subscribe(sceneKnob, "pushed", knobPushedHandler)
    subscribe(sceneKnob, "doubleTapped", knobDoubleTappedHandler)
    subscribe(sceneKnob, "held", knobHeldHandler)
}    

// on/off and increment level with push
def knobPushedHandler(evt) {
    logDebug("knobPushedHandler(${evt.value}) called")

    def pressed = (evt.value).toInteger()

    if (pressed == 1) {
        if (blind.currentValue("windowShade") == "closed") blind.open()
        else if (blind.currentValue("windowShade") == "open") blind.close()
        else if (blind.currentValue("windowShade") == "partially open" ) {
            if (blind.currentValue("position").toInteger() < 60) {blind.open()} else {blind.close()}
        }
    }
    if (pressed == 3) { // step open
        blind.startPositionChange("open")
        runInMillis(settings?.positionChangeDuration.toInteger(), stopPositionChange)
    }
    if (pressed == 2) { // step closed
        blind.startPositionChange("close")
        runInMillis(settings?.positionChangeDuration.toInteger(), stopPositionChange)
    }
}

def stopPositionChange() {
    blind.stopPositionChange()
}

// stop position change with double tap
def knobDoubleTappedHandler(evt) {
    logDebug("knobDoubleTappedHandler(${evt.value}) called")
    blind.stopPositionChange()
}

// Half Open when held
def knobHeldHandler(evt) {
    logDebug("knobHeldHandler(${evt.value}) called")
    blind.setPosition(settings?.partialPosition.toInteger())
}


def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("debugMode",[value:"false",type:"bool"])
}