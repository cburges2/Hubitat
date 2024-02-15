/**
 *  ****************  Illuminance Data Sync  ****************
 *
 *  Usage:
 *  This was designed to update a virtual illuminance attribute driver attributes based on illuminance
 *  Updates sunset, lowLight, dayLight bools as illuminance values change
 *    
 * v.2.0 - 10/23 - get states directly from defice instead of saving as state variable
**/

definition (
    name: "Illuminance Data Sync",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Set Illuminance Values in Illuminance Data Device",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Illuminance Data Device</b>") {

            input (
              name: "illuminanceData", 
              type: "capability.actuator",  
              title: "Select Illuminance Data Device", 
              required: true, 
              multiple: false,     
              submitOnChange: true
            )
            
            if (illuminanceData) {
                input (
                    name: "trackIlluminanceData", 
                    type: "bool", 
                    title: "Track physical data changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }  
        }
        

        section("<b>Outside Illuminance Sensor Device</b>") {
            input (
                name: "illuminanceSensor", 
                type: "capability.illuminanceMeasurement",
                title: "Select Outside Illuminance Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (illuminanceSensor) {
                input (
                    name: "trackIlluminance", 
                    type: "bool", 
                    title: "Track outside illuminance changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }

        section("<b>Inside Illuminance Sensor Device</b>") {
            input (
                name: "indoorSensor", 
                type: "capability.illuminanceMeasurement",
                title: "Select Inside Illuminance Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (illuminanceSensor) {
                input (
                    name: "trackIndoor", 
                    type: "bool", 
                    title: "Track indoor illuminance changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }        

        section("<b>Virtual Auto Light Target Dimmer Device</b>") {
            input (
                name: "lightTarget", 
                type: "capability.switchLevel",
                title: "Select Virtual Light Target Dimmer Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (illuminanceSensor) {
                input (
                    name: "trackTarget", 
                    type: "bool", 
                    title: "Track Light Target changes", 
                    required: true, 
                    defaultValue: "true"
                )
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

    if (settings?.debugMode) runIn(3600, logDebugOff)   // one hour
    initialize()
}

def initialize() {

    subscribe(illuminanceSensor, "illuminance", illuminanceController)
    subscribe(insideSensor, "illuminance", insideSensorController)
    subscribe(lightTarget, "level", lightTargetController)   
}

def illuminanceController(evt) {
    logDebug("Illuminance Sensor Event = ${evt.value}")

    def lux = evt.value.toInteger()
    illuminanceData.setSensorIlluminance(lux)
}

// sync indoor illuminance to dilluminance data
def insideSensorController(evt) {
    state.insideSensor = evt.value.toInteger()
    logDebug("Inside Sensor Event = $state.insideSensor")

    illuminanceData.setIndoorIlluminance(evt.value)
}

// sync light target changes to illuminance data
def lightTargetController(evt) {
    state.lightTarget = evt.value.toInteger()
    logDebug("Light Target Event = $state.lightTarget")

    illuminanceData.setLightTarget(evt.value)
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