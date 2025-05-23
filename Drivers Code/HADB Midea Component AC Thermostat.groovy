/*

Copyright 2024

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------

Change history:

0.1  - ritchierich  - initial version
2.4  - Yves Mercier - Modified healthCheck handling
2.7  - Yves Mercier - Add support for presets
2.8  - mluck        - corrected typo
2.12 - Yves Mercier - Add presets by name
2.14 - Yves Mercier - Add support for humidity setting
2.17 - cburgess     - Modifed for Midea MAW12V1QWT U-Shaped AC. Changed the name to HADB Midea Component AC Thermostat, added correct mode and fan commands, and added 
                      thermostatOperatingState attribute for dashboard tile, with a choice for the fan_only mode to be "fan only" or "idle", for operatingState.
                      which is set from the thermostatMode value.   
*/

metadata
{
    definition(name: 'HADB Midea Component AC Thermostat', namespace: 'community', author: 'community')
    {
        capability 'Actuator'
        capability 'Sensor'
        capability 'TemperatureMeasurement'
        capability 'Thermostat'
        capability 'RelativeHumidityMeasurement'
        capability 'Refresh'
        capability "Health Check"
    }
    preferences
    {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "fanOnlyState", type: "enum", title: "thermostatOperatingState for fan_only", description: "operating state value to use", defaultValue: "fan only", options:["fan only","idle"]
    }

    command "setPreset", [[name: "preset", type: "STRING", description: "Preset"]]
    command "setHumidity", [[name: "humiditySetpoint", type: "NUMBER", description: "Humidity setpoint"]]
    command "fanOnly"
    command "dry"
    command "fanSilent"
    command "fanLow"
    command "fanMedium"
    command "fanHigh"
    command "fanMax"
    command "setThermostatOperatingState", [[name:"thermostatOperatingState",type:"ENUM", description:"Thermostat operatingState to set", constraints:["fan only","idle","cooling","dry","auto","off"]]]
    command "setThermostatMode", [[name:"thermostatMode",type:"ENUM", description:"Thermostat mode to set", constraints:["fan_only","dry","cool","auto","off"]]]
    command "setThermostatFanMode", [[name:"thermostatFanMode",type:"ENUM", description:"Fan Mode to set", constraints:["silent","low","medium","high","auto","max"]]]

    attribute "healthStatus", "enum", ["offline", "online"]
    attribute "supportedThermostatFanModes", "JSON_OBJECT"
    attribute "supportedThermostatModes", "JSON_OBJECT"
    attribute "supportedPresets", "string"
    attribute "currentPreset", "string"
    attribute "maxHumidity", "number"
    attribute "minHumidity", "number"
    attribute "humiditySetpoint", "number"
    attribute "thermostatOperatingState", "string"
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    refresh()
}

void updated() {
    log.info "Updated..."
    if (logEnable) runIn(1800,logsOff)
    sendEvent(name: "supportedThermostatModes", value: '["fan_only","dry","cool","auto","off"]')
}

void uninstalled() {
    log.info "${device} driver uninstalled"
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description) {
    description.each {
        if (it.name in ["thermostatMode", "temperature", "thermostatFanMode", "thermostatSetpoint", "coolingSetpoint", "heatingSetpoint", "supportedThermostatModes", "supportedThermostatFanModes", "supportedPresets", "currentPreset", "healthStatus", "maxHumidity", "minHumidity", "humidity", "humiditySetpoint"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
        if (it.name == "thermostatMode") {
            setThermostatOperatingState(it.value)
        }        
    }
}

void off() {
    parent?.componentOff(this.device)
    sendEvent(name: "thermostatOperatingState", value: "off")
}

void refresh() {
    parent?.componentRefresh(this.device)
}

void setCoolingSetpoint(BigDecimal temperature) {
    parent?.componentSetCoolingSetpoint(this.device, temperature)
}

void setHeatingSetpoint(BigDecimal temperature) {
    parent?.componentSetHeatingSetpoint(this.device, temperature)
}

void setThermostatMode(String thermostatMode) {
    parent?.componentSetThermostatMode(this.device, thermostatMode)
    setThermostatOperatingState(thermostatMode)
}

void setThermostatOperatingState(String thermostatMode) {
    if (logEnable) log.debug "setThermostatOperatingState called with ${thermostatMode}"

    if (thermostatMode != null) {
        if (thermostatMode == "cool") {sendEvent(name: "thermostatOperatingState", value: "cooling")}
        else if (thermostatMode == "fan_only") {sendEvent(name: "thermostatOperatingState", value: fanOnlyState)}  // set in prefrences
        else if (thermostatMode == "dry") {sendEvent(name: "thermostatOperatingState", value: "drying")}
        else if (thermostatMode == "auto") {sendEvent(name: "thermostatOperatingState", value: "auto")}
        else if (thermostatMode == "off") {sendEvent(name: "thermostatOperatingState", value: "off")}  
    }
}

void setThermostatFanMode(String fanMode) {
    parent?.componentSetThermostatFanMode(this.device, fanMode)
}

def setHumidity(humiditySetpoint) {
    if ((humiditySetpoint > this.device.currentValue("maxHumidity")) || (humiditySetpoint < this.device.currentValue("minHumidity"))) log.warn "humidity setpoint out of range"
    else parent?.componentSetHumidity(this.device, humiditySetpoint)
}

void auto() {
    parent?.componentAuto(this.device)
    sendEvent(name: "thermostatOperatingState", value: "auto")
}

void cool() {
    parent?.componentCool(this.device)
    sendEvent(name: "thermostatOperatingState", value: "cooling")
}

void emergencyHeat() {
    parent?.componentEmergencyHeat(this.device)
}

void fanOnly() {
    parent?.componentSetThermostatMode(this.device, "fan_only")
    sendEvent(name: "thermostatOperatingState", value: fanOnlyState)
}

void fanSilent() {
    parent?.componentSetThermostatFanMode(this.device, "silent")
}

void fanLow() {
    parent?.componentSetThermostatFanMode(this.device, "low")
}

void fanMedium() {
    parent?.componentSetThermostatFanMode(this.device, "medium")
}

void fanHigh() {
    parent?.componentSetThermostatFanMode(this.device, "high")
}

void fanMax() {
    parent?.componentSetThermostatFanMode(this.device, "max")
}

void dry() {
    parent?.componentSetThermostatMode(this.device, "dry")
    sendEvent(name: "thermostatOperatingState", value: "drying")
}

void heat() {
    parent?.componentHeat(this.device)
}

void fanAuto() {
    parent?.componentFanAuto(this.device)
}

void fanCirculate() {
    parent?.componentFanCirculate(this.device)
}

void fanOn() {
    parent?.componentFanOn(this.device)
}

def setPreset(preset){
    if (this.device.currentValue("supportedPresets") == "none") log.warn "no supported presets defined"
    else parent?.componentSetPreset(this.device, preset)
}

def logsOff(){
    log.warn("debug logging disabled...")
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def ping() {
    refresh()
}
