/**
 *  ****************  Irrigation Controller Sync  ****************
 *
 *  Usage:
 *  This was designed to update a virtual irrigation controller virtual device from an external soil moisture sensor
 *    
 *  v.1.0 4/19/23  Initial code
 *  v.1.5 4/26/23  Added water and fertilize pumps activation option

**/

definition (
    name: "Irrigation Controller Sync",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync soil moisture sensor's moisture to a virtual irrigation controller device",
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

        section("<b>Moisture Sensor Device</b>") {
            input (
                name: "moistureSensor", 
                type: "capability.relativeHumidityMeasurement", 
                title: "Select Soil Moisture Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (moistureSensor) {
                input (
                    name: "trackMoisture", 
                    type: "bool", 
                    title: "Track physical moisture changes", 
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

        section("<b>Use Fertilize Pump Switch Device</b>") {
            input (
              name: "selectFertilize", 
              type: "bool", 
              title: "Activate Fertilize Pump Device when Fertilizing", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectFertilize) {
                state.useFertilizePump = true
                section("<b>Water Pump Switch Device</b>") {
                    input (
                        name: "fertilizePump", 
                        type: "capability.switch", 
                        title: "Select Fertilize Pump Switch Device", 
                        required: true, 
                        multiple: false
                    )
                }
            } else {
                state.useFertilizePump = false            
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
    state.sensorMoisture = 50
    state.water = "off"
    state.fertilize = "off"
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(moistureSensor, "humidity", moistureSync)
    subscribe(irrigationController, "contact", waterController)
    subscribe(irrigationController, "lock", fertilizeController)

}

def moistureSync(evt) {

    state.sensorMoisture = evt.value
    logDebug("Moisture Sensor Event = $state.sensorMoisture")
    def percent = evt.value.toInteger()

    irrigationController.setMoisture(percent)
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

def fertilizeController(evt) {
    def lock = evt.value

    if (lock == "locked") {
        state.fertilize = "on"
        if (state.useFertilizePump) fertilizePump.on()
    } else {
        state.fertilize = "off"
        if (state.useFertilizePump) fertilizePump.off()
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}