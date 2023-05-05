/**
 *  ****************  Timer Irrigation Controller App  ****************
 *
 *  Usage:
 *  This was designed to control a pump for a virtual timer irrigation controller device
 *    
 *  v.1.0 5/2/23  Initial code
 *  

**/

definition (
    name: "Timer Irrigation Controller App",
    namespace: "Hubitat",
    author: "cburgess",
    description: "Active water pump from a virtual timer irrigation controller device",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Virtual Irrigation Controller</b>") {

            input (
              name: "irrigationController", 
              type: "capability.actuator", 
              title: "Select Virtual Irrigation Controller Device", 
              required: true, 
              multiple: false,
              submitOnChange: true  
            )
            if (mirrigationController) {
                input (
                    name: "trackController", 
                    type: "bool", 
                    title: "Track Controller changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }      
        }

        section("<b>Use Water Pump Switch Device</b>") {
            input (
              name: "selectController", 
              type: "bool", 
              title: "Activate Water Pump Device when Watering", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController) {
                state.useWaterPump = true
                section("<b>Water Pump Switch Device</b>") {
                    input (
                        name: "waterPump", 
                        type: "capability.switch", 
                        title: "Select Water Pump Switch Device", 
                        required: true, 
                        multiple: false
                    )
                }
            } else {
                state.useWaterPump = false            
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
    state.water = "off"
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(irrigationController, "contact", waterController)

}

def waterController(evt) {  
    def contact = evt.value

    if (contact == "open") {
        state.water = "on"
        if (state.useWaterPump) waterPump.on()
    } else {
        state.water = "off"
        if (state.useWaterPump) waterPump.off()
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}