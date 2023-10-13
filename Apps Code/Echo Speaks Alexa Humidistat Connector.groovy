/**
 *  ****************  Echo Speaks Alexa Routine Connectors  ****************
 *
 *  Usage:
 *  This was designed to control a humidifier  from Hubitat using the executeRoutineId command in Echo Speaks App to trigger Alexa routines by ID. 
 *
**/

definition (
    name: "Echo Speaks Alexa Humidistat Connector",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for Alexa connected devices from Hubitat using Echo Speaks executeRoutineId feature",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {

        section("<b>Echo Device to issue Commands</b>") {

            input (
              name: "echoDevice", 
              type: "capability.speechSynthesis", 
              title: "Select Echo Device to use to send commands", 
              required: true, 
              multiple: false            
            )
            state.more1 = false
            state.more2 = false
        }

        section("<b>Virtual Switch Device 1</b>") {

            input (
              name: "virtualSwitch1", 
              type: "capability.switch", 
              title: "Select Virtual Switch 1 Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )

            if (virtualSwitch1) {
                input (
                    name: "trackVirtualSwitch1", 
                    type: "bool", 
                    title: "Track Virtual Switch 1 Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 

            section("<b>Routine ID for Humidifier On</b>") {
                input (
                name: "onRoutine1", 
                type: "String", 
                title: "On Routine 1",
                defaultValue: "",
                required: true, 
                multiple: false,
                submitOnChange: true               
                )
                state.onRoutine1 = onRoutine1
            }         

            section("<b>Routine ID for Humidifier Off</b>") {
                input (
                name: "offRoutine1", 
                type: "String", 
                title: "Off Routine 1", 
                defaultValue: "",
                required: true, 
                multiple: false,
                submitOnChange: true               
                )
                state.offRoutine1 = offRoutine1
            }    
        }

        section("<b>Add Sterilize Lock Device?</b>") {
            state.more1 = false
            input (
              name: "selectController2", 
              type: "bool", 
              title: "Turn on to add Sterilize Device", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController2) {
                state.more1 = true
            } 
   

            if (state.more1) {   
                section("<b>Virtual Motion Device</b>") {
                    input (
                    name: "virtualSwitch2", 
                    type: "capability.lock", 
                    title: "Select Virtual Motion Device", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true             
                    )

                    if (virtualSwitch2) {
                        input (
                            name: "trackVirtualSwitch2", 
                            type: "bool", 
                            title: "Track Virtual Switch 2 Changes", 
                            required: true, 
                            defaultValue: "false"
                        )

                        section("<b>Routine ID for Sanatize Active</b>") {
                            input (
                            name: "onRoutine2", 
                            type: "String", 
                            title: "On Routine 2",
                            defaultValue: "",
                            required: true, 
                            multiple: false,
                            submitOnChange: true               
                            )
                            state.onRoutine2 = onRoutine2
                        }         

                        section("<b>Routine ID for Sanatize Inactive</b>") {
                            input (
                            name: "offRoutine2", 
                            type: "String", 
                            title: "Off Routine 2", 
                            defaultValue: "",
                            required: true, 
                            multiple: false,
                            submitOnChange: true               
                            )
                            state.offRoutine2 = offRoutine2
                        }    

                    } 
                }

            }  
        }   

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

 
def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(virtualSwitch1, "switch", switchHandler1) 
   
    if (state.more1) {
        subscribe(virtualSwitch2, "motion", switchHandler2)
    }

}    

def switchHandler1(evt) {

    state.switch1 = evt.value
    logDebug("Switch Event = $state.switch1")   

    def routineID = ""
    if (evt.value == "on") routineID = onRoutine1
    else routineID = offRoutine1
    logDebug("routineID = $routineID")

    echoDevice.executeRoutineId(routineID)
}

def switchHandler2(evt) {

    state.switch2 = evt.value
    logDebug("Switch Event = $state.switch2")   

    def routineID = ""
    if (evt.value == "locked") routineID = onRoutine2
    else routineID = offRoutine2
    logDebug("routineID = $routineID")

    echoDevice.executeRoutineId(routineID)
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}