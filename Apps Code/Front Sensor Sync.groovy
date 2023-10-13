/**
 *  ****************  Front Sensor Data Sync  ****************
 *
 *  Usage:
 *  This was designed to sync a virtual attribute device with motion sensor events
 *  
 *  Version 9/26/23  - Initial release
 *  Version 10/11/23 - check Timeout before setting Inactive
**/

definition (
    name: "Front Sensor Sync",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Sync Motion Sensors to Front Sensor Data attribute device",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Virtual Attribute Controller</b>") {

            input (
              name: "attributeController", 
              type: "capability.actuator", 
              title: "Select Virtual Attribute Device", 
              required: true, 
              multiple: false               
            )
            if (attributeController) {
                input (
                    name: "trackAttributes", 
                    type: "bool", 
                    title: "Track attribute changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }  
        }

        section("<b>Living Room Motion Sensor Device</b>") {
            input (
                name: "livingRoomSensor", 
                type: "capability.motionSensor", 
                title: "Select Living Room Motion Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (livingRoomSensor) {
                input (
                    name: "trackLivRmMotion", 
                    type: "bool", 
                    title: "Track physical motion changes", 
                    required: true, 
                    defaultValue: "true"
                )
            }             
        }
        
        section("<b>Kitchen Motion Sensor Device</b>") {
            input (
                name: "kitchenSensor", 
                type: "capability.motionSensor", 
                title: "Select Kitchen Motion Sensor Device", 
                required: true, 
                multiple: false,
                submitOnChange: true
            )

            if (kitchenSensor) {
                input (
                    name: "trackKitMotion", 
                    type: "bool", 
                    title: "Track physical motion changes", 
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
    state.kitchenMotionStatus = "Inactive"
    state.livingRoomMotionStatus = "Inactive"
    initialize()
}

def updated() {
    initialize()
}

def initialize() {

    subscribe(livingRoomSensor, "motion", livingRoomSync)
    subscribe(kitchenSensor, "motion", kitchenSync)
    subscribe(attributeController, "livingRoomMotionStatus", setLivingRoomState)
    subscribe(attributeController, "kitchenMotionStatus", setKitchenState)
}

def setLivingRoomState(evt) {
   if (evt.value == "Timeout") {
       state.livingRoomMotionStatus == "Timeout"
   }
}

def setKitchenState(evt) {
   if (evt.value == "Timeout") {
       state.kitchenMotionStatus = "Timeout"
   }
}

def livingRoomSync(evt) {
   
    logDebug("Living Room Motion Event = $state.livingRoomMotion")
    def motion = evt.value
    
    if (motion == "active") {
        attributeController.setLivingRoomMotionStatus("Active")  
        state.livingRoomMotionStatus = "Active"
    }
    if (motion == "inactive" && state?.livingRoomMotion != "Timeout") {
        attributeController.setLivingRoomMotionStatus("Inactive")  
        state.livingRoomMotionStatus = "Inactive"
    }
    setFrontStatus()
    
}

def kitchenSync(evt) {

    logDebug("Kithen Motion Event = $state.kitchenMotion")
    def motion = evt.value
    
    if (motion == "active") { 
        attributeController.setKitchenMotionStatus("Active") 
        attributeController.setKitchenActivityStatus("Active") 
        state.kitchenMotionStatus = "Active"
    }
    if (motion == "inactive" && state?.kitchenMotion != "Timeout") {
        attributeController.setKitchenMotionStatus("Inactive") 
        attributeController.setKitchenActivityStatus("Inactive")  
        state.kitchenMotionStatus = "Inactive"
    }
    
    setFrontStatus()
}

def setFrontStatus() {
    def front = state?.frontMotionStatus
    def livingRoom = state?.livingRoomMotionStatus
    def kitchen = state?.kitchenMotionStatus
    
    if ((kitchen == "Active" || livingRoom == "Active") && front != "Active") attributeController.setFrontMotionStatus("Active")
    if ((kitchen == "Inactive" && livingRoom == "Inactive") && front != "Inactive") attributeController.setFrontMotionStatus("Inactive")
    if ((kitchen == "Timeout" && livingRoom == "Timeout") && front != "Timeout") attributeController.setFrontMotionStatus("Timeout")
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}