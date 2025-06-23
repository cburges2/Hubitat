/**
 *  ****************  Echo Speaks Command Dimmer Switch  ****************
 *
 *  Usage:
 *  This was designed to control up to five virtual Dimmers from Hubitat using the speak text command in Echo Speaks App to control Alexa dimmers
 *  c. 2023 cburgess
**/

definition (
    name: "Echo Speaks Command Dimmer Switch",
    namespace: "Hubitat",
    author: "CBurgess",
    description: "Controller for Alexa connected dimmer devices from Hubitat virtual dimmer using Echo Speaks Command as text feature",
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
        }

        section("<b>Virtual Dimmer Device</b>") {

            input (
              name: "virtualSwitch", 
              type: "capability.switchLevel", 
              title: "Select Virtual Dimmer Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("") {
            input (
                name: "alexaDimmer", 
                type: "text", 
                title: "Alexa Dimmer Device Name", 
                required: true, 
                defaultValue: false
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

    subscribe(virtualSwitch, "switch", switchHandler) 
    subscribe(virtualSwitch, "level", levelHandler) 
}    

def switchHandler(evt) {

    logDebug("Switch Event = ${evt.value}")   

    def cmd = "Turn " + evt.value + " " + settings?.alexaDimmer
    logDebug("cmd = $cmd")
    echoDevice.voiceCmdAsText(cmd)

}

def levelHandler(evt) {
    logDebug("Level Event = ${evt.value}")

    def cmd = "Set " + settings?.alexaDimmer + " to " + evt.value + " percent"
    logDebug("cmd = $cmd")
    echoDevice.voiceCmdAsText(cmd)
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}