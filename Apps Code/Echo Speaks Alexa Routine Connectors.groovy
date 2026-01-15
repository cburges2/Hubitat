/**
 *  ****************  Echo Speaks Alexa Routine Connectors  ****************
 *
 *  Usage:
 *  This was designed to control two virtual Switches from Hubitat using the executeRoutineId command in Echo Speaks App to control Alexa Devices
 *  For more than two switches, use multiple instance of the app, and name them different in the app settings. 
 *
**/

definition (
    name: "Echo Speaks Alexa Routine Connectors",
    namespace: "Hubitat",
    author: "ChrisB",
    description: "Controller for Alexa connected dimmer devices from Hubitat using Echo Speaks executeRoutineId feature",
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
            label title: "", required: false
        }  
        
        section("<b>Echo Device for Commands</b>") {

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

            section("<b>Routine ID for Switch 1 On</b>") {
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

            section("<b>Routine ID for Switch 1 Off</b>") {
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

        section("<b>Add Another Switch</b>") {
            state.more1 = false
            input (
              name: "selectController2", 
              type: "bool", 
              title: "Turn on to add more devices", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController2) {
                state.more1 = true
            } 
   

            if (state.more1) {   
                section("<b>Virtual Switch Device 2</b>") {
                    input (
                    name: "virtualSwitch2", 
                    type: "capability.switch", 
                    title: "Select Virtual Switch 2 Device", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true             
                    )

                    if (virtualSwitch2) {
                        section("<b>Routine ID for Switch 2 On</b>") {
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

                        section("<b>Routine ID for Switch 2 Off</b>") {
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
    unsubscribe()
    initialize()
}

def initialize() {

    subscribe(virtualSwitch1, "switch", switchHandler1) 
   
    if (state.more1) {
        subscribe(virtualSwitch2, "switch", switchHandler2)
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
    if (evt.value == "on") routineID = onRoutine2
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