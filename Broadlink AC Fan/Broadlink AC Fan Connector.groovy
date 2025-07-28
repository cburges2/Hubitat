/**
 *  ****************  Broadlink AC Fan Connector  ****************
 *
 *  Usage:
 *  This was designed to sync a Broadlink AC Fan Device's attribute changes and send the corresponding Broadlink commands
 *
 *  Version 1.0 - 07/25/25 - works as an ac switch dimmer
 *  Version 1.1 - 07/26/25 - added optional temp sensor device to update temperature attribute in driver for ac switch thermostat tile display
 *  Version 1.2 - 07/27/25 - add optional virtual thermostat selection to have a virtual thermostat run the device from thermostatOperatingState
**/

definition (
    name: "Broadlink AC Fan Connector",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for an AC Fan Switch device to trigger broadlink RF code names in the Broadlink driver with thermostat control option",
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

        section("App Name") {
            label title: "", required: false
        }     

        section("<b>Broadlink Device</b>") {

            input (
              name: "broadlinkDevice", 
              type: "capability.actuator", 
              title: "Select BroadLink Device", 
              required: true, 
              multiple: false           
            )
        }    

        section("<b>Thermostat Device</b>") {

            input (
              name: "virtualThermostat", 
              type: "capability.thermostat", 
              title: "Select a Physical or Virtual Thermostat Device to run the AC (optional)", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>Temperature Sensor Device</b>") {

            input (
              name: "tempSensor", 
              type: "capability.temperatureMeasurement",
              title: "Select Temp Sensor Device (optional)", 
              required: false, 
              multiple: false,
              submitOnChange: true             
            )
        }

        if (virtualThermostat) {
            section("") {
                input (
                    name: "idleCmd",
                    type: "enum",
                    title: "Choose Command to send when virtual thermostat operating state is idle:",
                    options: [fan:"fan", off:"off"],
                    multiple: false,
                    defaultValue: 1,
                    required: true
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

    setAcFanDevice()
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {

    if (tempSensor) {subscribe(tempSensor, "temperature", updateTemperature)}
    if (virtualThermostat) {subscribe(virtualThermostat, "thermostatOperatingState", thermoStateHandler)}
}   

def setAcFanDevice() {
    logDebug("settingAcFanDevice")
    if (!acFanSwitch) {
        def ID = createAcFanDevice()
        app.updateSetting("AcFanSwitch",[value:getChildDevice(ID),type:"capability.actuator"])
    }
}

def createAcFanDevice() {
    logDebug("createAcFanDevice() called")
    def deviceNetworkId = "CD_${app.id}_${new Date().time}"
    
    try {
        def AcFanDevice = addChildDevice("Hubitat", "Broadlink AC Fan Driver", deviceNetworkId, null, [name: "Broadlink AC Fan Driver", label: "${app.label} Controller", isComponent: false])
        
        logDebug("Created Ac Fan device in 'Hubitat' using driver 'Broadlink AC Fan Driver' (DNI: ${deviceNetworkId})")
        state.AcFanDevice = deviceNetworkId
        return deviceNetworkId

    } catch (Exception e) {
        log.error "Failed to create Ac Fan device: ${e}"
    }
}

def sendBroadlinkCommand(command) {
    logDebug("Sending broadlinkCommand ${command}")

    broadlinkDevice.SendStoredCode(command,1)
}

def updateTemperature(evt) {
    def temp = (evt.value).toString()
    AcFanSwitch.setTemperature(temp)

    if (virtualThermostat) {
        virtualThermostat.setTemperature(evt.value)
    }
}

// If turn off on idle is set in prefs, there will be a three minute delay before turning off
def thermoStateHandler(evt) {
    def thermoState = evt.value

    if (thermoState == "idle") {
        if (idleCmd == "fan") {AcFanSwitch.fan()}
        if (idleCmd == "off") {AcFanSwitch.fan(); runIn(1800,turnOff)}
    }
    if (thermoState == "cooling") {AcFanSwitch.cool()}
    if (thermoState == "off") {AcFanSwitch.off()}
}

def turnOff() {
    AcFanSwitch.off()
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}