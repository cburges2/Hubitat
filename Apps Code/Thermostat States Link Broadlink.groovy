/**
 *  **************** Thermostat States Link Broadlink  ****************
 *
 *  Usage:
 *  This was designed to link a thermostat heating/cooling to send a broadlink stored command
 *  a BROADLINK IR device must be installed to use this app.  
 *
 *  Author: Chris Burgess
 *  10/13/23 - Initial Code
 *  
**/

definition (
    name: "Thermostat States Link Broadlink",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for a Thermostat to link with a Broadlink device to send commands with heat/cool idle/fan",
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

        section("<b>Broadlink Device</b>") {
            input (
                name: "broadlink", 
                type: "capability.actuator", 
                title: "Select Broadlink Device", 
                required: true, 
                multiple: false
            )
        }
        section("<b>BroadLink On Command</b>") {
            input (
            name: "blOn", 
            type: "String", 
            title: "Enter On Command", 
            required: true, 
            multiple: false,
            submitOnChange: true               
            )
        }        
        state.broadlinkOn = blOn

        section("<b>BroadLink Off Command</b>") {
            input (
            name: "blOff", 
            type: "String", 
            title: "Enter Off Command", 
            required: true, 
            multiple: false,
            submitOnChange: true               
            )
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

    if ((thermoState == "cooling"  && state?.cooling) || (thermoState == "heating" && !state?.cooling)) {
        logDebug("State Change is ${thermoState}")
        state.acState = thermoState
        broadlink.SendStoredCode(state.broadlinkOn)
    }
    if (thermoState == "idle" || thermoState =="fan") {
        logDebug("State Change is {$thermoState}")
        state.acState = thermoState
        broadlink.SendStoredCode(state.broadlinkOff)
    }
}

def off() {
    broadlink.SendStoredCode(state.broadlinkOff)
}

def on() {
    broadlink.SendStoredCode(state.broadlinkOn)
}

def logDebug(txt){
    try {
        if (settings?.debugMode) { log.debug("${virtualThermostat.getLabel()} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}