/**
 *  ****************  Broadlink Fireplace Connector  ****************
 *
 *  Usage:
 *  This was designed to sync up to five virtual switches to Broadlink Virtual Device code names
 *
**/

definition (
    name: "Broadlink Fireplace Connector",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for Virtual Switches to control an IR fireplace via Broadlink device",
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

        section("<b>Fireplace Flames</b>") {

            input (
              name: "flamesSwitch", 
              type: "capability.switch", 
              title: "Select Flames Virtual Switch", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Fireplace Heat</b>") {

            input (
            name: "heatSwitch", 
            type: "capability.switch", 
            title: "Select Fireplace Virtual Switch", 
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

    subscribe(flamesSwitch, "switch", flamesHandler) 
    subscribe(heatSwitch, "switch", heatHandler)

}    

def flamesHandler(evt) {

    state.switch = evt.value
    logDebug("Switch Event = $state.swtich")   

    if (evt.value == "on") {
        broadlinkHandler("Flames On")
    } else {
        broadlinkHandler("Flames Off")
    }
}

def heatHandler(evt) {

    state.switch2 = evt.value
    logDebug("Switch2 Event = $state.swtich2")   

    if (evt.value == "on") {
        broadlinkHandler("Fireplace Heat")
    } else {
        broadlinkHandler("Fireplace Heat Off")
        runIn(5,confirmHeatOff)
    }
}

def broadlinkHandler(command) {
    broadlinkDevice.SendStoredCode(command)
}

def confirmHeatOff() {
    broadlinkDevice.SendStoredCode("Fireplace Heat Off")
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}