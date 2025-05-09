metadata {
    definition(name: "Virtual Humidity Device", namespace: "yourNamespace", author: "Your Name") {
        capability "RelativeHumidityMeasurement"
        capability "Refresh"
        
        attribute "humidity", "number"
        
        command "setHumidity", [[name: "humidity", type: "NUMBER", description: "Humidity percentage (0-100)"]]
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def installed() {
    log.info "Virtual Humidity Device installed"
    sendEvent(name: "humidity", value: 50.0, unit: "%")
}

def updated() {
    log.info "Virtual Humidity Device updated"
    if (logEnable) runIn(1800, logsOff)
}

def logsOff() {
    log.warn "Debug logging disabled"
    device.updateSetting("logEnable", [value:"false", type:"bool"])
}

def refresh() {
    if (logEnable) log.debug "Refresh requested"
    def currentHumidity = device.currentValue("humidity") ?: 50.0
    sendEvent(name: "humidity", value: currentHumidity, unit: "%")
}

def setHumidity(humidity) {
    if (logEnable) log.debug "Setting humidity to ${humidity}%"
    
    // Validate input
    humidity = humidity.toFloat()
    if (humidity < 0) humidity = 0.0
    if (humidity > 100) humidity = 100.0
    
    // Round to 1 decimal place
    humidity = Math.round(humidity * 10) / 10.0
    
    sendEvent(name: "humidity", value: humidity, unit: "%")
}