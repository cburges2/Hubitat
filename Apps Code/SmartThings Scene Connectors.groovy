/**
 *  ****************  SmartThing Scene Device Connectors  ****************
 *
 *  Usage:
 *  This was designed to sync up to five virtual switches to SmartThings scene knobs from Hubitat Replica SmartThings Hub connect
 *
**/

definition (
    name: "SmartThings Scene Connectors",
    namespace: "Hubitat",
    author: "Burgess",
    description: "Controller for Hubitat Scene Switches connected to Replica by scene knob devices",
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

        section("<b>Virtual Switch Device</b>") {

            input (
              name: "virtualSwitch", 
              type: "capability.switch", 
              title: "Select Virtual Switch Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )

            if (virtualSwitch) {
                input (
                    name: "trackVirtualSwitch", 
                    type: "bool", 
                    title: "Track Virtual Switch Changes", 
                    required: true, 
                    defaultValue: "true"
                )
            } 
        }

        section("<b>SmartThings On Scene Device</b>") {
            input (
                name: "onScene", 
                type: "capability.actuator", 
                title: "Select On Scene Device", 
                required: true, 
                multiple: false
            )
        }

        section("<b>SmartThings Off Scene Device</b>") {
            input (
                name: "offScene", 
                type: "capability.actuator", 
                title: "Select Off Scene Device", 
                required: true, 
                multiple: false
            )
        }

        section("<b>Add More Devices</b>") {
            input (
              name: "selectController", 
              type: "bool", 
              title: "Turn on to add more devices", 
              required: true, 
              multiple: false,
              submitOnChange: true               
            )
            if (settings.selectController) {
                state.more1 = true
            } else {
                state.more1 =  false           
            }
        }

        if (state.more1) {

            section("<b>Virtual Switch Device 2</b>") {

                input (
                name: "virtualSwitch2", 
                type: "capability.switch", 
                title: "Select Virtual Switch Device", 
                required: true, 
                multiple: false,
                submitOnChange: true             
                )

                if (virtualSwitch2) {
                    input (
                        name: "trackVirtualSwitch2", 
                        type: "bool", 
                        title: "Track Virtual Switch2 Changes", 
                        required: true, 
                        defaultValue: "true"
                    )
                } 
            }

            section("<b>SmartThings On Scene Device 2</b>") {
                input (
                    name: "onScene2", 
                    type: "capability.actuator", 
                    title: "Select On Scene Device", 
                    required: true, 
                    multiple: false
                )
            }

            section("<b>SmartThings Off Scene Device 2</b>") {
                input (
                    name: "offScene2", 
                    type: "capability.actuator", 
                    title: "Select Off Scene Device", 
                    required: true, 
                    multiple: false
                )
            }

            section("<b>Add More Devices</b>") {
                input (
                name: "selectController2", 
                type: "bool", 
                title: "Turn on to add more devices", 
                required: true, 
                multiple: false,
                submitOnChange: true               
                )
                if (settings.selectController2) {
                    state.more2 = true
                } else {
                    state.more2 =  false           
                }
            }

            if (state.more2) {
                section("<b>Virtual Switch Device 3</b>") {

                    input (
                    name: "virtualSwitch3", 
                    type: "capability.switch", 
                    title: "Select Virtual Switch 3 Device", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true             
                    )

                    if (virtualSwitch2) {
                        input (
                            name: "trackVirtualSwitch3", 
                            type: "bool", 
                            title: "Track Virtual Switch3 Changes", 
                            required: true, 
                            defaultValue: "true"
                        )
                    } 
                }

                section("<b>SmartThings On Scene Device 3</b>") {
                    input (
                        name: "onScene3", 
                        type: "capability.actuator", 
                        title: "Select On Scene Device", 
                        required: true, 
                        multiple: false
                    )
                }

                section("<b>SmartThings Off Scene Device 3</b>") {
                    input (
                        name: "offScene3", 
                        type: "capability.actuator", 
                        title: "Select Off Scene Device", 
                        required: true, 
                        multiple: false
                    )
                }

                section("<b>Add More Devices</b>") {
                    input (
                    name: "selectController3", 
                    type: "bool", 
                    title: "Turn on to add more devices", 
                    required: true, 
                    multiple: false,
                    submitOnChange: true               
                    )
                    if (settings.selectController3) {
                        state.more3 = true
                    } else {
                        state.more3 =  false           
                    }
                }     

                if (state.more3) {  
                    section("<b>Virtual Switch Device 4</b>") {

                        input (
                        name: "virtualSwitch4", 
                        type: "capability.switch", 
                        title: "Select Virtual Switch 4 Device", 
                        required: true, 
                        multiple: false,
                        submitOnChange: true             
                        )

                        if (virtualSwitch4) {
                            input (
                                name: "trackVirtualSwitch4", 
                                type: "bool", 
                                title: "Track Virtual Switch4 Changes", 
                                required: true, 
                                defaultValue: "true"
                            )
                        } 
                    }

                    section("<b>SmartThings On Scene Device 4</b>") {
                        input (
                            name: "onScene4", 
                            type: "capability.actuator", 
                            title: "Select On Scene Device", 
                            required: true, 
                            multiple: false
                        )
                    }

                    section("<b>SmartThings Off Scene Device 4</b>") {
                        input (
                            name: "offScene4", 
                            type: "capability.actuator", 
                            title: "Select Off Scene Device", 
                            required: true, 
                            multiple: false
                        )
                    }

                    section("<b>Add More Devices</b>") {
                        input (
                        name: "selectController4", 
                        type: "bool", 
                        title: "Turn on to add more devices", 
                        required: true, 
                        multiple: false,
                        submitOnChange: true               
                        )
                        if (settings.selectController3) {
                            state.more4 = true
                        } else {
                            state.more4 =  false           
                        }
                    } 

                    if (state.more4) {  
                        section("<b>Virtual Switch Device 5</b>") {

                            input (
                            name: "virtualSwitch5", 
                            type: "capability.switch", 
                            title: "Select Virtual Switch 4 Device", 
                            required: true, 
                            multiple: false,
                            submitOnChange: true             
                            )

                            if (virtualSwitch4) {
                                input (
                                    name: "trackVirtualSwitch5", 
                                    type: "bool", 
                                    title: "Track Virtual Switch4 Changes", 
                                    required: true, 
                                    defaultValue: "true"
                                )
                            } 
                        }

                        section("<b>SmartThings On Scene Device 5</b>") {
                            input (
                                name: "onScene5", 
                                type: "capability.actuator", 
                                title: "Select On Scene Device", 
                                required: true, 
                                multiple: false
                            )
                        }

                        section("<b>SmartThings Off Scene Device 5</b>") {
                            input (
                                name: "offScene5", 
                                type: "capability.actuator", 
                                title: "Select Off Scene Device", 
                                required: true, 
                                multiple: false
                            )
                        }

                    } // end more 4
                } // end more 3        
            } // end more 2
        } // end more 1

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

    subscribe(virtualSwitch, "switch", switchHandler) 
    if (state.more1) {
        subscribe(virtualSwitch2, "switch2", switchHandler2)
    }
    if (state.more2) {
        subscribe(virtualSwitch3, "switch3", switchHandler3)
    }
    if (state.more3) {
        subscribe(virtualSwitch4, "switch4", switchHandler4)
    }
    if (state.more4) {
        subscribe(virtualSwitch5, "switch5", switchHandler5)
    }
}    

def switchHandler(evt) {

    state.switch = evt.value
    logDebug("Switch Event = $state.swtich")   

    if (evt.value == "on") {
        onScene.refresh()
    } else {
        offScene.refresh()
    }
}

def switchHandler2(evt) {

    state.switch2 = evt.value
    logDebug("Switch2 Event = $state.swtich2")   

    if (evt.value == "on") {
        onScene2.refresh()
    } else {
        offScene2.refresh()
    }
}

def switchHandler3(evt) {

    state.switch3 = evt.value
    logDebug("Switch3 Event = $state.swtich3")   

    if (evt.value == "on") {
        onScene3.refresh()
    } else {
        offScene3.refresh()
    }
}

def switchHandler4(evt) {

    state.switch4 = evt.value
    logDebug("Switch4 Event = $state.swtich4")   

    if (evt.value == "on") {
        onScene4.refresh()
    } else {
        offScene4.refresh()
    }
}

def switchHandler5(evt) {

    state.switch5 = evt.value
    logDebug("Switch5 Event = $state.swtich5")   

    if (evt.value == "on") {
        onScene5.refresh()
    } else {
        offScene5.refresh()
    }
}

def logDebug(txt){
    try {
        if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
        log.error("bad debug message")
    }
}