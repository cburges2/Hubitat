/**
 *  ****************  Update SunCalc Illuminance  ****************
 *
 *  Usage:
 *  This was designed to send a lux sensor value to the SunCalc Illuminance driver
 *  
 *  
**/

definition (
    name: "Update SunCalc Illuminance",
    namespace: "Hubitat",
    author: "CBurgess",
    description: "",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {

    dynamicPage(name: "mainPage") {

        section("<b>Outdoor Lux Sensor</b>") {
            input (
              name: "luxSensor", 
              type: "capability.illuminanceMeasurement", 
              title: "Select Outdoor Lux Sensor Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }

        section("<b>SunCalc Illuminance Device</b>") {
            input (
              name: "sunCalc", 
              type: "capability.illuminanceMeasurement", 
              title: "Select SunCalc Illuminance Device", 
              required: true, 
              multiple: false,
              submitOnChange: true             
            )
        }  
    }
}

def installed() {
    initialize()
}

def updated() {

    unsubscribe()
    initialize()

}
def initialize() {
    subscribe(luxSensor, "illuminance", updateSunCalc)

}

def updateSunCalc(evt) {
    def lux = evt.value
    sunCalc.setSensorIlluminance(lux)
}