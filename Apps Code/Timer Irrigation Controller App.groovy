/**
 *  ****************  Timer Irrigation Controller App  ****************
 *
 *  Usage:
 *  This was designed to control a pump for a virtual timer irrigation controller device
 *    
 *  v.1.0 5/2/23  Initial code
 *  v.2.0 5/26/23 Added Run Seconds Variable Connector sync to attribute option

**/

definition (
    name: "Timer Irrigation Controller App",
    namespace: "Hubitat",
    author: "cburgess",
    description: "Activate water pump from a virtual timer irrigation controller device",
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
        
        section("<b>Virtual Irrigation Controller</b>") {

            input (
              name: "irrigationController", 
              type: "capability.actuator", 
              title: "Select Virtual Irrigation Controller Device", 
              required: true, 
              multiple: false,
              submitOnChange: true  
            ) 
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

        section("<b>Setting Variable Device</b>") {

            input (
              name: "settingsInput", 
              type: "capability.actuator", 
              title: "Select Settings Varialbe Device", 
              required: true, 
              multiple: false,
              submitOnChange: true  
            ) 
        }        

        section("<b>Settings Buttons Device</b>") {

            input (
              name: "settingsButtons", 
              type: "capability.pushableButton", 
              title: "Select Settings Button Device", 
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
    state.water = "off"
    initialize()
}

def updated() {

    if (settings?.debugMode) runIn(3600, logDebugOff)   // one hour
    unsubscribe(runSecondsController)

    initialize()
}

def initialize() {

    subscribe(irrigationController, "contact", waterController)
    subscribe(settingsButtons, "pushed", settingsController)    

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

def settingsController(evt) {  
    def pushed = evt.value.toInteger()
    def value = settingsInput.currentValue("variable").toInteger()

    // Run Seconds
    if (pushed == 1) {irrigationController.setRunSeconds(value)}
    // Run Interval Hours
    if (pushed == 2) {irrigationController.setRunIntervalHours(value)}
    // start timer
    if (pushed == 3) {irrigationController.startTimer()}
    // stop timer
    if (pushed == 4) {irrigationController.stopTimer()}

}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}

def logDebugOff() {
    logDebug("Turning off debugMode")
    app.updateSetting("debugMode",[value:"false",type:"bool"])
}