/**
 *  ****************  Broadlink AC Switch Buttons Connector  ****************
 *
 *  Usage:
 *  This was designed to sync a bedroom AC Fan switch with a buttons device
 *
**/

definition (
    name: "Broadlink AC Switch Buttons Connector",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for virtual switch to sync with a button device to trigger broadlink RF code names in Broadlink driver",
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

        section("<b>Virtual AC Fan Switch Device</b>") {

            input (
              name: "acFanSwitch", 
              type: "capability.switch", 
              title: "Select Virtual Switch Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>AC Buttons Device</b>") {

            input (
              name: "acButtons", 
              type: "capability.pushableButton", 
              title: "Select AC Buttons Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }        

        section("<b>Bedroom Virtual Thermostat/b>") {

            input (
              name: "thermostat", 
              type: "capability.thermostat", 
              title: "Select Bedroom Virtual Thermostat Device", 
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
}

def initialize() {

    subscribe(acFanSwitch, "switch", switchHandler) 
    subscribe(acButtons, "pushed", buttonHandler)      
    
    //subscribe(thermostat, "acStatus", statusHandler)
}    

def statusHandler(evt) {
    if (evt.value == "fan") {state.current = "acOff"}
    if (evt.value == "cool") {state.current = "acCool"}
    if (evt.value == "off") {state.current = "acOff"}
}

def switchHandler(evt) {
    logDebug("AC Fan Switch received ${evt.value}")

    def acStatus = thermostat.currentValue("presence")
    def switched = evt.value

    logDebug("acStatus is ${acStatus}")

    if (switched == "on" && acStatus != "fan") {        
        logDebug("Turned on Fan Switch")
        sendBroadlink("AC Fan",1)
    }
    if (switched == "off" && acStatus != "off") { 
        logDebug("Turned Off Fan Switch")
        sendBroadlink("AC Off",1)
    }   
}

// 1=AC Fan 3=ac off 2=cool
def buttonHandler(evt) {
    logDebug("Button Handler pressed is ${evt.value}")

    def acStatus = thermostat.currentValue("presence")
    logDebug("acStatus is ${acStatus}")

    if (evt.value == "1" && acStatus != "fan") {
        sendBroadlink("AC Fan",1)
        acFanSwitch.on()   
    }
    if (evt.value == "2" && acStatus != "cool") {
        sendBroadlink("AC Cool",2)
        logDebug("Sent AC Cool to Broadlink",1)
    }
    if (evt.value == "3" && acStatus != "off") {
        acFanSwitch.off()
        sendBroadlink("AC Off",1)
    }
}

def sendBroadlink(command,reps) {
    broadlinkDevice.SendStoredCode(command,reps)
    def times = "time"
    if (reps > 1) {times = "times"}
    logDebug("Sent ${command} to broadlink ${reps} ${times}")
    state.acStatus = command
    runIn(18,checkStatus)
}

def checkStatus() {
    def acStatus = thermostat.currentValue("presence")

    if (state?.acStatus == "AC Fan" && acStatus != "fan") {
        logDebug("Resending AC Fan")
        sendBroadlink("AC Fan", 1)      
    }
    if (state?.acStatus == "AC Off" && acStatus != "off") {
        logDebug("Resending AC Off")
        sendBroadlink("AC Off", 1)    
    }
    if (state?.acStatus == "AC Cool" && acStatus != "cool") {
        logDebug("Resending AC Cool")
        sendBroadlink("AC Cool", 1)
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}