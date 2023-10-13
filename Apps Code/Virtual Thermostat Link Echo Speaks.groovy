/**
 *  ****************  Virtual Thermostat Link Echo Speaks  ****************
 *
 *  Usage:
 *  This was designed to link a virtual thermostat cooling on/off to Echo Speaks run RoutineID 
 *  
 *   10/13/23 - Initial Code
**/

definition (
    name: "Virtual Thermostat Link Echo Speaks",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for a Virtual Thermostat to link cooling commands to Echo Speaks routineIDs",
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

    subscribe(virtualThermostat, "thermostatOperatingState", virtualStateHandler)

}

def virtualStateHandler(evt) {
    def thermoState = evt.value.toString()
    logDebug("State Change Event = $thermoState")
    def routineID = ""

    if (thermoState == "cooling") {
        logDebug("State Change is $thermoState")
        state.acState = "on"
        routineID = state?.onRoutine
    }
    if (thermoState == "idle" || thermoState =="fan") {
        logDebug("State Change is $thermoState")
        state.acState = "off"
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