/**
 *  ****************  Virtual Thermostat Link Broadlink  ****************
 *
 *  Usage:
 *  This was designed to link a virtual thermostat cooling on/off to broadlink on/off commands  
 *
 *  10/13/23 - Initial Code
**/

definition (
    name: "Virtual Thermostat Link Broadlink",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for a Virtual Thermostat to sync with a Physical Thermostat and a Temp Sensor",
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

    if (thermoState == "cooling") {
        logDebug("State Change is $thermoState")
        state.acState = "on"

        broadlink.SendStoredCode(state.broadlinkOn)
        runIn(5,on)

    }
    if (thermoState == "idle" || thermoState =="fan") {
        logDebug("State Change is $thermoState")
        state.acState = "off"

        broadlink.SendStoredCode(state.broadlinkOff)
        runIn(5,off)
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