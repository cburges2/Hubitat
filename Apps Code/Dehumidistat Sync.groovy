/**
 *  ****************  Deumidistat Sync  ****************
 *
 *  Usage:
 *  This was designed to update a virtual dehumidistat's humidity from an external humidiy sensor
 *  Also syncs setpoint from 2nd external humidity sensor
 *    
 *  1/15/24: Added Fan Control, and set fan Speed based on humid difference. 
**/

definition (
    name: "Dehumidistat Sync",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync humidity sensor's humidity to a virtual dehumidistat device",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Virtual Dehumidistat Controller</b>") {

            input (
              name: "dehumidistatController", 
              type: "capability.actuator", 
              title: "Select Virtual Dehumidistat Device", 
              required: true, 
              multiple: false               
            )
        }

        section("<b>Humidity Sensor Device</b>") {
            input (
                name: "humidity1", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Room Humidity Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )           
        }
        
        section("<b>Humidity Sensor Setpoint Device</b>") {
            input (
                name: "humidity2", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Humidity Sensor Device for Setpoint", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )           
        }
        
        section("<b>Bathroom Fan</b>") {
            input (
                name: "bathroomFan", 
                type: "capability.fanControl", 
                title: "Select Bathroom Fan Controller Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )           
        }

        section("<b>Flush Sensor</b>") {
            input (
                name: "flushSensor", 
                type: "capability.contactSensor", 
                title: "Select Flush Sensor Contact Device", 
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
    unsubscribe()
    initialize()
}

def initialize() {

    subscribe(flushSensor, "contact", contactController)
    subscribe(dehumidistatController, "motion", motionController)
    subscribe(dehumidistatController, "operatingState", stateController)
    subscribe(bathroomFan, "switch", switchController)
    subscribe(bathroomFan, "doubleTapped", tapController)

    subscribe(humidity1, "humidity", dehumidistatHumiditySensor1)
    subscribe(humidity2, "humidity", dehumidistatHumiditySensor2)

    state.sensorHumidity = humidity1.humidity
    state.setpointHumidity = humidity2.humidity
}

def contactController(evt) {

    if (evt.value == "open") {
        logDebug("flush Sensor Activated")
        if (bathroomFan.currentValue("switch") == "off") {
            def speed = dehumidistatController.currentValue("speed")
            bathroomFan.setSpeed(speed)
        }
        dehumidistatController.setOffInMin(10)
        bathroomFan.on()
    }
}

def motionController(evt) {

    if (evt.value == "inactive") {
        logDebug("Motion Sensor Inactive")
        if (dehumidistatController.currentValue("operatingState") == "idle") {
            bathroomFan.off()
        } 
    }
}

def stateController(evt) {

    if (evt.value == "idle") {
        logDebug("Turning off Fan - Idle")
        bathroomFan.off()
    }
}

def switchController(evt) {

    if (evt.value == "on") {
        logDebug("Fan Switch Pressed On")
        dehumidistatController.setOffInMin(15)
    }
}

def tapController(evt) {

    if (evt.value == "1") {
        logDebug("Fan Switch Doubletap 1")
        dehumidistatController.setSpeed("high")
        dehumidistatController.setOffInMin(30)
        bathroomFan.on()
    }

}

def dehumidistatHumiditySensor1(evt) {

    state.sensorHumidity = evt.value
    logDebug("Humidity Sensor Event = $state.sensorHumidity")
    def lvl = evt.value.toInteger()

    dehumidistatController.setHumidity(lvl)  

    setSpeed()
}



def dehumidistatHumiditySensor2(evt) {

    state.setpointHumidity = evt.value
    logDebug("Humidity Setpoint Event = $state.setpointHumidity")
    def lvl = evt.value.toInteger()
    
    def setlvl = lvl + 3

    dehumidistatController.setDehumidifyingSetpoint(setlvl) 

    setSpeed()
}

def setSpeed() {

    def fanSwitch = bathroomFan.currentValue("switch")
    def fanSpeed = bathroomFan.currentValue("speed")
    def bathHumid = humidity1.currentValue("humidity").toInteger()
    def roomHumid = humidity2.currentValue("humidity").toInteger()
    def speed = dehumidistatController.currentValue("speed")
    def humidDiff = bathHumid - roomHumid

    if ((humidDiff >= 5) && (speed != "medium")) {
        dehumidistatController.setSpeed("medium")
        if (fanSwitch == "on" && fanSpeed != "medium") {
            bathroomFan.setSpeed("medium")
            logDebug("Fan speed set to medium")
        }
    } else {
        if ((humidDiff > 0 && humidDiff < 5) && speed != "low") {
            dehumidistatController.setSpeed("low")
            if (fanSwitch == "on" && fanSpeed != "low") {
                bathroomFan.setSpeed("low")
                logDebug("Fan speed set to low")
            }
        }
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}