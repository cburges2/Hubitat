/**
 *  ****************  Thermostat States Link Echo Speaks RoutineIDs  ****************
 *  
 *  Usage:
 *  This was designed to link a thermostat's cooling/idle states to Echo Speaks device's executeRoutineID command 
 *  ECHO SPEAKS app must be installed and have a speak device to use this app.  Get the routineIDs from Echo Speaks settings - routine testing. 
 *
 *  Author: Chris Burgess
 *  10/13/23 - Initial Code
 *  
**/

definition (
    name: "Thermostat States Link Echo Speaks RoutineIDs",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for a Thermostat to link cooling/idle commands to Echo Speaks routineIDs",
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

        section("<b>Virtual Thermostat Device</b>") {

            input (
              name: "virtualThermostat", 
              type: "capability.thermostat", 
              title: "Select Virtual Thermostat Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )

            if (virtualThermostat) {
                input (
                    name: "trackVirtual", 
                    type: "bool", 
                    title: "Track Virtual Thermo Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }

        section("<b>Echo Speaks Device</b>") {
            input (
                name: "echoDevice", 
                type: "capability.speechRecognition", 
                title: "Select Echo Device", 
                required: true, 
                multiple: false
            )
        }
        section("<b>Routine ID for AC On</b>") {
            input (
            name: "onRoutine", 
            type: "String", 
            title: "On Routine",
            defaultValue: "",
            required: true, 
            multiple: false,
            submitOnChange: true               
            )
            state.onRoutine = onRoutine
        }         

        section("<b>Routine ID for AC Off</b>") {
            input (
            name: "offRoutine", 
            type: "String", 
            title: "Off Routine", 
            defaultValue: "",
            required: true, 
            multiple: false,
            submitOnChange: true               
            )
            state.offRoutine = offRoutine
        }    

        section("<b>Use for Cooling instead of Heating?</b>") {
        input (
            name: "useCool", 
            type: "bool", 
            title: "Turn on to use cooling state instead of heating", 
            required: true, 
            multiple: false,
            submitOnChange: true               
            )
            if (settings.useCool) {
                state.cooling = true
            } else {
                state.cooling =  false           
            }
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
    settings.cooling = false
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(virtualThermostat, "thermostatOperatingState", stateHandler)

}

def stateHandler(evt) {
    def thermoState = evt.value.toString()
    logDebug("State Change Event = ${thermoState}")
    def routineID = ""

    if ((thermoState == "cooling"  && state?.cooling) || (thermoState == "heating" && !state?.cooling)) {
        logDebug("State Change is ${thermoState}")
        state.acState = thermoState
        routineID = state?.onRoutine
    }
    if (thermoState == "idle" || thermoState =="fan") {
        logDebug("State Change is {$thermoState}")
        state.acState = thermoState
        routineID = state?.offRoutine
    }

    echoDevice.executeRoutineId(routineID)
}

def logDebug(txt){
    try {
        if (settings?.debugMode) { log.debug("${virtualThermostat.getLabel()} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}