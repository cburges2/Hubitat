/**
 *  ****************  Irrigation Controller Sync  ****************
 *
 *  Usage:
 *  This was designed to update a virtual irrigation controller virtual device from an external soil moisture sensor
 *  It also syncs settings attributes with hub variable devices to be used in dashboards. 
 *    
 *  v.1.0 4/19/23  Initial code
 *  v.1.5 4/26/23  Added water and fertilize pumps activation option
 *  v.2.0 5/24/23  Add Sync for hub variables to attributes and vice-versa

**/

definition (
    name: "Irrigation Controller Sync",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync soil moisture sensor's moisture to a virtual irrigation controller device, and sync attributes to hub variables",
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

        // optional use start moisture variable
        section("<b>Use Start Moisture Variable Device</b>") {
            input (
              name: "selectStartMoisture", 
              type: "bool", 
              title: "Use Start Moisture Variable?", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectStartMoisture) {
                state.useStartMoisture = true
                section("<b>Start Moisture Variable Device</b>") {
                    input (
                    name: "setStart", 
                    type: "capability.actuator", 
                    title: "Select Start Moisture Variable Device", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true  
                    )
                    if (setStart) {
                        input (
                            name: "trackStartSetting", 
                            type: "bool", 
                            title: "Track Start Moisture Setting Device", 
                            required: true, 
                            defaultValue: "true"
                        )
                    }  
                }
            }  else {
                state.useStartMoisture = false            
            }     
        }    

        // optional use min at target
        section("<b>Use Minutes at Target Variable Device</b>") {
            input (
              name: "selectMinTarget", 
              type: "bool", 
              title: "Use Minutes at Target Variable?", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectMinTarget) {
                state.useMinTarget = true
                section("<b>Minutes at Target Variable Device</b>") {
                    input (
                        name: "minTarget", 
                        type: "capability.actuator", 
                        title: "Select Min at Target Variable Device", 
                        required: true, 
                        multiple: false,
                        submitOnChange: true 
                    )
                    if (minTarget) {
                    input (
                        name: "trackMinTarget", 
                        type: "bool", 
                        title: "Track Min Target Variable Device", 
                        required: true, 
                        defaultValue: "true"
                    )
                  } 
                }
            } else {
                state.useMinTarget = false            
            }
        }        

        // optional use max Minutes device
        section("<b>Use Max Minutes Variable Device</b>") {
            input (
              name: "selectMaxMin", 
              type: "bool", 
              title: "Use Max Minutes Variable?", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectMaxMin) {
                state.useMaxMin = true
                section("<b>Max Minutes Variable Device</b>") {
                    input (
                        name: "maxMin", 
                        type: "capability.actuator", 
                        title: "Select Max Minutes Variable Device", 
                        required: true, 
                        multiple: false,
                        submitOnChange: true 
                    )
                    if (maxMin) {
                    input (
                        name: "trackMaxMin", 
                        type: "bool", 
                        title: "Track Max Minutes Variable Device", 
                        required: true, 
                        defaultValue: "true"
                    )
                  } 
                }
            } else {
                state.useMaxMin = false            
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

    if (state.useStartMoisture) subscribe(setStart, "variable", startController)
    if (state.useMinTarget) subscribe(minTarget, "variable", minController)
    if (state.useMaxMin) subscribe(maxMin, "variable", maxController)

    if (state.useMinTarget) subscribe(irrigationController, "minutesAtTarget", minDeviceController)
    if (state.useStartMoisture) subscribe(irrigationController, "startMoisture", startDeviceController)    
    if (state.useMaxMin) subscribe(irrigationController, "maxMinutes", maxDeviceController)
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
    } else if (lock == "unlocked") {
        state.fertilize = "off"
        if (state.useFertilizePump) fertilizePump.off()
    }
}

def startController(evt) {  
    state.startSetting = evt.value
    def ms = evt.value.toInteger()

    irrigationController.setStartMoisture(ms)
}

def minController(evt) {  
    state.minSetting = evt.value
    def ms = evt.value.toInteger()

    irrigationController.setMinutesAtTarget(ms)
}

def maxController(evt) {  
    state.maxSetting = evt.value
    def ms = evt.value.toInteger()

    irrigationController.setMaxMinutes(ms)
}

def startDeviceController(evt) {  
    state.startSetting = evt.value
    def ms = evt.value.toInteger()

    setStart.setVariable(ms)
}

def minDeviceController(evt) {  
    state.minSetting = evt.value
    def ms = evt.value.toInteger()

    minTarget.setVariable(ms)
}

def maxDeviceController(evt) {  
    state.maxSetting = evt.value
    def ms = evt.value.toInteger()

    maxMins.setVariable(ms)
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}