/**
 *  ****************  Broadlink Button Connectors  ****************   
 *
 *  Usage:
 *  This was designed to sync up to five virtual buttones to a Broadlink Virtual Device that has stored code commands.
 *  Each device is assigned its own on and off command to match the stored code name. 
 *  Install another instance for more than 5 button devices. 
 *
**/

definition (
    name: "Broadlink Button Connectors",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for virtual buttons to trigger broadlink RF code names in Broadlink driver",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {

        section("<b>Broadlink Device</b>") {

            input (
              name: "broadlinkDevice", 
              type: "capability.actuator", 
              title: "Select BroadLink Device", 
              required: true, 
              multiple: false           
            )
        }    

        section("<b>Virtual Button Device</b>") {

            input (
              name: "virtualButton", 
              type: "capability.pushableButton", 
              title: "Select Virtual Pushable Button Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )

            if (virtualButton) {
                input (
                    name: "trackVirtualButton", 
                    type: "bool", 
                    title: "Track Virtual Button Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }

        section("<b>BroadLink Button 1 Command</b>") {
            input (
              name: "push1", 
              type: "String", 
              title: "Enter Push 1 Command", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
        }        
        state.button1 = push1

        section("<b>Add Another Button</b>") {
            input (
              name: "selectPush2", 
              type: "bool", 
              title: "Turn on to add another button", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectPush2) {
                state.addPush2 = true
            } else {
                state.addPush2 =  false           
            }
        }

        if (state.addPush2) {
            section("<b>BroadLink Button 2 Command</b>") {
                input (
                name: "push2", 
                type: "String", 
                title: "Enter Push 2 Command", 
                required: true, 
                multiple: false,
                submitOnChange: true               
                )
            }        
            state.button2 = push2
        }

        section("<b>Add Another Button</b>") {
            input (
              name: "selectPush3", 
              type: "bool", 
              title: "Turn on to add another button", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectPush3) {
                state.addPush3 = true
            } else {
                state.addPush3 =  false           
            }
        }

        if (state.addPush3) {
            section("<b>BroadLink Button 3 Command</b>") {
                input (
                name: "push3", 
                type: "String", 
                title: "Enter Push 3 Command", 
                required: true, 
                multiple: false,
                submitOnChange: true               
                )
            }        
            state.button3 = push3
        }

        section("<b>Add Another Button</b>") {
            input (
              name: "selectPush4", 
              type: "bool", 
              title: "Turn on to add another button", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectPush4) {
                state.addPush4 = true
            } else {
                state.addPush4 =  false           
            }
        }

        if (state.addPush4) {
            section("<b>BroadLink Button 4 Command</b>") {
                input (
                name: "push4", 
                type: "String", 
                title: "Enter Push 4 Command", 
                required: true, 
                multiple: false,
                submitOnChange: true               
                )
            }        
            state.button4 = push4
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
    initialize()
}

def initialize() {

    subscribe(virtualButton, "pushed", buttonHandler) 
}    

def buttonHandler(evt) {
    state.button = evt.value
    logDebug("Pushed Button Event = $state.button")
    def pushed = evt.value.toInteger()  

    if (pushed == 1) {
        logDebug("Sending Code = $state.button1")
        broadlinkDevice.SendStoredCode(state.button1)
    } else if ((pushed == 2) && (state.addPush2)) {
        logDebug("Sending Code = $state.button2")
        broadlinkDevice.SendStoredCode(state.button2)
    } else if ((pushed == 3) && (state.addPush3)) {
        logDebug("Sending Code = $state.button3")
        broadlinkDevice.SendStoredCode(state.button3)
    } else if ((pushed == 4) && (state.addPush4)) {
        logDebug("Sending Code = $state.button4")
        broadlinkDevice.SendStoredCode(state.button4)
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}