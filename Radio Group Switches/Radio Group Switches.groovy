/*
 	Radio Group Switches Device

    Copyright 2025 -> Chris B

    This Virtual Device will create child component virtual switches that will work as a radio group.  When this parent switch is on, it will turn off
    all other child devices when one child is turned on, working like a radio group.  When parent swtich is off, switches will behave individually.  When the
    preference for Enabling use as a button controller is enabled, the switch that is turned on will also turn back off, acting like a button. The off delay is set in the 
    component switches individually, with a default of 500ms.  

    The attribute "active" will always update to the last switch that was turned on. When used as a button controller, this is the attribute that can be used in
    other automations to trigger events based on which switch was "pushed". As a button controller, the flash of the switch turning on gives a visual confirmation
    that the dashboard "button" was pushed, unlike regular dashboard button tiles. 

    Child devices are managed through this parent device. Create the child devices with the addSwitchDevice(label) command.  Remove with removeSwitchDevice(label).
    To keep labels in sync with the state device map, only change child device labels (names) using the renameSwitchDevice(old,new) command. 

    10/26/25 - v. 1.0 - Initial Release
    10/28/25 - v. 1.1 - added TurnOnSwitchDevice() command to turn on a switch in the group (from an app or other automation)
                      - added useAsAppChild prefrence, to call parent method radioSwitchActive(label) when a switch changes to active, 
                        if the useAsAppChild preference is set to true. 
*/

metadata {
    definition (name: "Radio Group Switches", namespace: "Hubitat", author: "Chris B") {
        capability "Actuator"
        capability "Switch"

        attribute "switch", "ENUM"
        attribute "active", "ENUM"
        attribute "numberOfSwitches", "NUMBER"
        
        command "addSwitchDevice", [[name: "label", type:"STRING", description:"Enter a Label to Create a New Radio Switch Device", defaultValue: ""]]
        command "removeSwitchDevice", [[name: "label", type:"STRING", description:"Enter a Label to Remove an Existing Radio Switch Device", defaultValue: ""]]
        command "renameSwitchDevice", [[name: "old Label", type:"STRING", description:"Enter a Label of the Device to Change Names", defaultValue: ""],[name: "new Label", type:"STRING", description:"Enter a new name to replace label in device", defaultValue: ""]]
        command "turnOnSwitchDevice", [[name: "label", type:"STRING", description:"Enter a Label to turn on a Switch Device", defaultValue: ""]]
    }

    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false
        input name: "buttonController", type:"bool", title: "Enable use as a button controller", defaultValue: false
        input name: "useAsAppChild", type:"bool", title: "Enable to use as a child device for a custom app", description: "This will call the parent method radioSwitchActive(label) when a switch becomes active" ,defaultValue: false
    }
}

def installed() {
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "active", value: "none")
    sendEvent(name: "numberOfSwitches", value: 0)
    initialize()

    updated()
}

def updated() {

    if (logEnable) runIn(1800,logsOff)

}

def initialize() {
    state.deviceMap = [:]
}

def addSwitchDevice(label) {   
    logDebug("addSwitchDevice(${label}) called")
  
    def ID = createSwitchChild(label)  // create child device

    // update maps and attribute
    if (ID != "error") {
        state.deviceMap[label] = ID
        sendEvent(name: "numberOfSwitches", value: state?.deviceMap.size())
    }
}
def createSwitchChild(name) {
    logDebug("createSwitchChild(${name}) called")
    def deviceNetworkId = "RS_${device.id}_${new Date().time}"
    
    try {
        def switchDevice = addChildDevice("hubitat", "Generic Component Virtual Switch", deviceNetworkId, [name: device.getLabel(), label: "${name}", isComponent: false])      
        logDebug("Created child device in 'hubitat' using driver 'Generic Component Virtual Switch' (DNI: ${deviceNetworkId})")
        return deviceNetworkId
    } catch (Exception e) {
        log.error "Failed to create ${name} device: ${e}"
        return "error"
    }
}

def getChildSwitchDevice(label) {

    def ID = state?.deviceMap["${label}"]
    logDebug("The ID of ${label} is ${ID}")

    return getChildDevice(ID)
}

def renameSwitchDevice(label, newLabel) {

    def ID = state?.deviceMap["${label}"]
    def thisDevice = getChildSwitchDevice(label)

    thisDevice.setLabel(newLabel)   
    state.deviceMap.remove(label)
    state.deviceMap[newLabel] = ID
}

def turnOnSwitchDevice(label) {
    def ID = state?.deviceMap["${label}"]
    def thisDevice = getChildSwitchDevice(label)
    thisDevice.on()
}

def removeSwitchDevice(label) {

    def ID = state?.deviceMap["${label}"]
    logDebug("child ID to remove is ${ID} as ${label}")

    try {
        deleteChildDevice(ID)
        state.deviceMap.remove(label)
        def mapSize = state?.deviceMap.size()
        sendEvent(name: "numberOfSwitches", value: mapSize, descriptionText: getDescriptionText("numberOfSwitches set to ${mapSeize}"))
    } catch (Exception e) {
        logDebug("Failed to delete child device ${label}")
    }
}

// component switch Turned On
def componentOn(childDevice) {
    logDebug("componentOn(${childDevice}) called")

    if (device.currentValue("switch") == "on") {turnOffOtherSwitches(childDevice)}
    else {
        logDebug("parent switch is off")
    }
}    

// component switch Turned Off - unused
def componentOff(childDevice) {
    logDebug("componentOff(${childDevice}) called")
}

// component refreshed - unused
def componentRefresh(childDevice) {
    logDebug("componentRefresh(${childDevice}) called")
}

def turnOffOtherSwitches(childDevice) {
    logDebug("turnOffOtherSwitches(${childDevice}) called") 

    def label = childDevice.getLabel()

    def iDs = state?.deviceMap.collect{entry -> entry.value}
    iDs.each{ID -> 
        def otherDevice = getChildDevice(ID)
        if (!label.equals(otherDevice.getLabel())) {
            if (otherDevice.currentValue("switch") == "on") {otherDevice.off()}
        }
    }    
    sendEvent(name: "active", value: label, descriptionText: getDescriptionText("active set to ${label}"))
    if (settings?.useAsAppChild) {parent?.radioSwitchActive(label)}

    if (settings?.buttonController) {
        logDebug("Button Controller Enabled")      
        childDevice.delayOff()        
    }
}

def parse(String description) { noCommands("parse") }

def on() {
    sendEvent(name: "switch", value: "on", descriptionText: getDescriptionText("switch set to on"))
}

def off() {
    sendEvent(name: "switch", value: "off", descriptionText: getDescriptionText("switch set to off"))   
}

def getDescriptionText(msg) {
    def descriptionText = "${device.displayName} ${msg}"
    if (settings?.txtEnable) log.info "${descriptionText}"
    return descriptionText
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}

