metadata {
    definition (name: "Virtual Switch Button", namespace: "Hubitat", author: "chrisbvt") {

        capability "Switch"	
        capability "PushableButton"	

        attribute "switch", "enum"
        attribute "pushed", "number"
    
 	}   
    
    preferences {       

    } 
}

def updated() {
    unschedule("off")
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: forceUpdate)
    sendEvent(name: "pushed", value: 2, stateChange: true)
}

def on() {
    sendEvent(name: "switch", value: "on", isStateChange: forceUpdate)
    sendEvent(name: "pushed", value: 1,stateChange: true)
}

def push(number) {
    if (number == 1) on()
    if (number == 2) off()
}
