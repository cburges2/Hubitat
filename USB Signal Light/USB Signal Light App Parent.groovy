/**
 *  **************** USB Signal Light App Parent  ****************
 *
 *  Usage:
 *  This was designed to 
 *  
 *
**/

definition (
    name: "USB Signal Light App Parent",
    namespace: "Hubitat",
    author: "ChrisB",
    description: "Controller for ",
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
            label title: "Optionally assign a custom name for this app", required: false
        } 

        section("<b>Main USB Multi-Switch Parent</b>") {

            input (
              name: "parentSwitch", 
              type: "capability.switch", 
              title: "Select USB Switch Parent Device", 
              required: true, 
              multiple: false           
            )
        }    

        section("<b>Switch Child Devices to Control </b>") {

            input (
              name: "blue", 
              type: "capability.switch", 
              title: "Select Blue Light Switch Device", 
              required: true, 
              multiple: false          
            )

            input (
              name: "red", 
              type: "capability.switch", 
              title: "Select Red Light Switch Device", 
              required: true, 
              multiple: false           
            )

            input (
              name: "green", 
              type: "capability.switch", 
              title: "Select Green Light Switch Device", 
              required: true, 
              multiple: false          
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
    updated()
}

def updated() {
    if (logEnable) runIn(1800,logsOff)

    if (!lightController) setLightControllerDevice()
    initialize()
}

def initialize() {

}    

// **************** Create Main Switch Child Device ******************
def setLightControllerDevice() {
    if (settings?.debugMode) logDebug("settingLightControllerDevice")
    if (!lightControllerDevice) {
        def ID = createLightControllerDevice()
        app.updateSetting("lightController",[value:getChildDevice(ID),type:"capability.actuator"])
    }
}
def createLightControllerDevice() {
    if (settings?.debugMode) logDebug("createLightControllerDevice() called")
    def deviceNetworkId = "CD_${app.id}_${new Date().time}"  
    try {
        def lightSwitchDevice = addChildDevice("Hubitat", "USB Signal Light Child Device", deviceNetworkId, null,[name: "USB Signal Light",label: "USB Signal Light", isComponent: false])       
        if (settings?.debugMode) logDebug("Created Signal Light device in 'Hubitat' using driver 'USB Signal Light Child Device' (DNI: ${deviceNetworkId})")
        state.lightControllerDevice = deviceNetworkId
        return deviceNetworkId
    } catch (Exception e) {
        log.error "Failed to create USB Signal Light Child Device: ${e}"
    }
}

// call from child device
def setColor(color, status) {
    def device = settings?."${color}"
    logDebug("device is ${device}")
    logDebug("command is ${status}")

    if (status == "on") device.on()
    if (status == "off") device.off()
}

def allOn() {
    parentSwitch.on()
}

def allOff() {
    parentSwitch.off()
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}