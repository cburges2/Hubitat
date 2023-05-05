//https://github.com/sab0276/Hubitat/blob/main/virtualSwitchUDTH-Lite.groovy
//Can be used to integrate other systems/devices into Hubitat via 3rd party platforms like IFTTT, Alexa, Webhooks, etc
//Alexa Routines need to use Contact Sensors or Motion Sensors for their Triggers
//so if you need Alexa integration, make sure you enable the Contact or Motion Sensor functions in the preferences
//Note adding some capabilities like Lock or Door Control may limit where it can be used due to security
//Idea from Mike Maxwell's SmartThings uDTH: https://community.smartthings.com/t/release-universal-virtual-device-type-and-translator/47836
//If you need more than just SWITCH, CONTACT, MOTION, and/or PRESENCE, use my Virtual Switch uDTH Super device driver for that device instead.    

//Force State Update preference will send an event everytime you manually push a form button or app tries to do something with the device.  Ie.  If the device is already On, and an app tries to turn it On, it will send a On/Open/Motion/Present event. 

metadata {
    definition (name: "Virtual Alexa Actuator Switch", namespace: "sab0276", author: "Scott Barton, Chris Burgess") {
        capability "Sensor"
        capability "Actuator"
        capability "Configuration"
        capability "Contact Sensor"	//"open", "closed"
        capability "Motion Sensor"	//"active", "inactive" 
        capability "Switch"		    //"on", "off" - switches Motion Sensor
        command "open"
        command "close"
        command "active"
        command "inactive"
 	}   
    
    preferences {
        input name: "contact", type: "bool", title: "Contact Sensor", defaultValue: false
        input name: "motion", type: "bool", title: "Motion Sensor", defaultValue: false
        
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "forceUpdate", type: "bool", title: "Force State Update", description: "Send event everytime, regardless of current status. ie Send/Do On even if already On.",  defaultValue: false
    } 
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: forceUpdate)
    sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
    logTxt "turned Off"
}

def on() {
    sendEvent(name: "switch", value: "on", isStateChange: forceUpdate)
    sendEvent(name: "motion", value: "active", isStateChange: forceUpdate)
    logTxt "turned On"
    if (autoOff.toInteger()>0){
        runIn(autoOff.toInteger(), off)
    }
}

def close() {
    sendEvent(name: "contact", value: "closed", isStateChange: forceUpdate)
    logTxt "closed"
}

def open() {
    sendEvent(name: "contact", value: "open", isStateChange: forceUpdate)
    logTxt "closed"
}

def inactive() {
    sendEvent(name: "motion", value: "inactive", isStateChange: forceUpdate)
    logTxt "inactive"
}

def active() {
    sendEvent(name: "motion", value: "active", isStateChange: forceUpdate)
    logTxt "active"
}

def installed() {
}

void logTxt(String msg) {
	if (logEnable) log.info "${device.displayName} ${msg}"
}

//Use only if you are on 2.2.8.141 or later.  device.deleteCurrentState() is new to that version and will not work on older versions.  
def configure(){  
    if (device.currentValue("contact") != null) device.deleteCurrentState("contact")
    if (device.currentValue("motion") != null) device.deleteCurrentState("motion")
    logTxt "configured. State values reset."
}