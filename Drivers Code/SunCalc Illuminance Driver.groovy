/**
 * SunCalc Driver for Hubitat
 *
 * A port of the SunCalc.js library https://github.com/mourner/suncalc
 *
 * Altitude is the angle up from the horizon. Zero degrees altitude means exactly on your local horizon, and 90 degrees is "straight up". Hence, "directly underfoot" is -90 degrees altitude. 
 * Azimuth is the angle along the horizon, with zero degrees corresponding to North, and increasing in a clockwise fashion. Thus, 90 degrees is East, 180 degrees is South, and 270 degrees is West. 
 * Using these two angles, one can describe the apparent position of an object (such as the Sun at a given time).
 *
 * Copyright (c) 2019 Justin Walker - SunCalc original code
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  V1.0.0 - Initial version
 *
 *  Modified by C Burgess, (c) January/February, 2026 to calcuate Expected illuminance and sun conditions.
 *      * AI was used to generate some calculation methods and some code used in other methods. This Code is otherwise human written and structured.
 *
 * SunCalc Modified:

 *  V1.1.0 - Added Illuminance Sensor capability. Added solar irradiance and illuminance calculations with incident angle preference for the calculation. 
 *  V1.2.1 - Added a twilight calculation for when solor illuminance goes to zero during twilight, but a lux sensor still sees light
 *  V1.3.0 - Added sensorIlluminance attribute and set command, to set an actual lux value from an external sensor, and a preference to refresh calcs when it is updated. 
 *  V1.4.0 - Added preference for a lux calibration Factor, to calibrate illuminance value to the physical lux sensor
 *  V1.5.0 - Added getting the illumance difference between actual and expected lux, storing difference values in a running list, and using that list for an average and a variance calc
             Average DFN is used to determine Sunny, or cloudy.  Variance of the list values are useed to determine partly or mostly conditions.
             Hazy Sun when low sunlight, and Diminished Light for a possibly blocked sensor (snow on sensor or sensor in shade). 
 *  V1.5.1 - Added a preference for the number of differences to store in the list.  More points takes longer to change the condition, but it should be more accurate to the total sky 
 *  V1.6.0 - Rewowrked updateConditions method.  Calculated lux values are now also stoared in a list of the length set in prefreneces for difference list, and that averaged is 
             used for nominal lux, instead of just the current Lux value.  Preferences added for Haxy, Cloudy, and Diminished thresholds, based on the % below target.  Default values set. 
             Reworked twilight illuminance calc, it takes over from the standard irradiance based lux calc after about 5 degrees altitude, when the two equations intersect, and twilight goes
             lower than the standard irradiance illuminance calc. 
 *  V1.7.0 - Adding scaling based on a factor of current altitude vs alltitude at noon, with new method to calculate noon altitude. New Calculations for both light conditions and variance
             modifiers, to keep the values vs the targets in proper scale to each other through the day, as both get larger with altitude otherwise.    
 *  V1.8.0 - Scrapped Variance to determine Party/Mostly modifiers.  Now using a list of conditions based on the number of conditions to store prefrence. Saves Cloudy or Sunny states to
             the conditions list, then calculates percent cloudy of the list to determine the mostly/partly modifier. 
             Removed Hazy Sun as a condition, by weather definition this is Partly Sunny, but I'm using Mostly Sunny.  Changed threshold preference to be Mostly Sunny instead of Hazy Sun. 
             Mostly Sunny is set based on illuminance, but it can still be set as a modifier for lux above Mostly Sunny threshold if the cloud percent calls for it.          
 *  V1.9.0 - Added Contact Sensor Capability and the contact attribute.  Added preference for setting contact to closed when sensorPercent crosses the threshold set for closed.
             This is to be used as a trigger condition for closing curtains and blinds as a signal for how much sun is coming in the window (to be chacked against an altitude and azimuth range in the automation)
             New exponential formula for the scaling factor needed in updateConditions. Reversed factor to squash low altitudes instead of boosting high altitudes, and use a single formula. 
 *  V1.9.1 - Added Contact Close Features.  Preference to keep the contact closed timer, so contact will stay closed for that amount of time before reopening.  
             To save opening and closing too often during Partly conditions. 
             Added an optional Contact Close Restrction Prefrence, based on current condition. The closing of the contacat can be based on the current condition level, to avoid too much open and closing. 
 *  V1.9.2 - More Updates to scaling factor. Noon altitude is used for the scaling factor around noon, but a preference for Max Altitudfe was added to keep lower altitude scale factor constant. I also added
             a noon Altitude Multiplier preference, to control when the scaling factor kicks in over the noon curve (higher muliplier means noon scaling starts at higher altitudes). 

  * SunCalc Illuminance Driver - Initial Release

  * Version 1.0.0-beta - 02-28-26 - Functioning beta version with Calculated Illuminance, Sensor Illuminance, Condition Calcs, and Contact Sensor features. 
 */

metadata {
    definition (name: "SunCalc Illuminance Driver", namespace: "Hubitat", author: "C Burgess") {
        capability "Actuator"
        capability "Sensor"
        capability "IlluminanceMeasurement"
        capability "ContactSensor"

        attribute "altitude", "number"
        attribute "azimuth", "number"
        attribute "solarIrradiance", "number"
        attribute "illuminance", "number"
        attribute "rawLux", "number"
        attribute "lastCalculated", "string"
        attribute "sensorIlluminance", "number"
        attribute "calFactor", "number"
        attribute "condition", "string"
        attribute "skyCondition", "string"
        attribute "oscillation", "string"
        attribute "sensorPercent", "string"
        attribute "contact", "enum"

        command "refresh"
        command "setSensorIlluminance",[[name:"Sensor Illuminance",type: "NUMBER", description: "Set External Sensor Illuminance Value"]]       
        command "clearListValues"
        command "setContact", [[name:"For Automation Testing",type:"ENUM", description:"Manually Set open or closed", constraints:["open","closed"]]]
        command "resetOpenDelay"
    }
    preferences() {
        section("Automatic Calculations"){
            input "autoUpdate", "bool", title: "Auto Update?", required: true, defaultValue: false
            input "updateInterval", "enum", title: "Update Interval:", required: true, defaultValue: "5 Minutes", options: ["1 Minute", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
            input "sensorUpdate", "bool", title: "Update when Sensor Illuminance Updates?", required: true, defaultValue: true       
            input "logEnable", "bool", title: "Enable debug logging",defaultValue: false
        }
        section("Solar Radiation And Illuminance Settings") {
            input "incidenceAngle", "number", title: "Surface Incidence Angle (degrees)", description: "0=horizontal, 90=vertical. Set this to match the angle of your lux sensor, so the calculations will match.  Most Weather Stations look straight up at 0 degrees.", required: true, defaultValue: 0, range: "0..90"
        }
        section("Sensor Illuminance Settings") {
            input "calibrationFactor", "number", title: "Lux Calibration Factor", description: "Multiplier to illuminance based on raw lux (e.g., 1.1 = +10%, 1.0 = no correction). Negative values are allowd (e.g., -1.1 = -10%) ). This should be used to calibrate noon illuminance value when sunny.", required: true, defaultValue: 1.0, range: "-5.01..5.01"
            input "autoCalibrate", "bool", title: "Auto Calibrate.", description: "Turn this on somewhere around Noon on a totally clear sunny day. It will calculate and set the Lux Calibration Factor using the current Lux value of sensor and expected Lux (experimental)", required: true, defaultValue: false
            input "scalingChangeRate", "number", title: "Power for the exponential scaling factor", description: "Rate to scale adjusted lux % as altitude gets bigger or smaller. A lower value will push the adjusted lux % down lower at low altitudes and high noon", required: true, defaultValue: 8.0, range: "1..25"
            input "maxAltitude", "number", title: "Max Altitude to use for low Altitude scaling factor", description: "The Max altitude value that is used in the low altitude scaling equaltion. Higher max altitude will result in the low altitude scaling factor kicking in at higher altitudes. ", required: true, defaultValue: 35.0, range: "10..95"
            input "noonMultiplier", "number", title: "Noon Altitude multiplier for scaling factor", description: "Adjusts the Noon altitude multiplier that is used in the scaling equaltion at noon. Higher Noon Multiplier will result in noon scaling factor kicking in at higher altitudes. ", required: true, defaultValue: 1.9, range: "1..5"
        }
        section("Sky Condition Settings") {
            input "minAltitude", "number", title: "The Altitude to stop calculating Conditions", description: "Below 6.5 degree altitude, condition calcs are not very accurate. Below this altitude, conditions change to period names", required: true, defaultValue: 6.5, range: "2..8"
            input "numValues", "number", title: "Number of lux values to use for Current Condition Calcs", description: "The fewer data values used, the faster current condtion will update to overtake the average as changing lux values get added", required: true, defaultValue: 3.0, range: "3..15"
            input "numConditions", "number", title: "Number of Sun/Cloud values to use for Condition Modifier Calcs", description: "How many Sunny or Cloudy conditions to store to determine Partly or Mostly condition modifiers", required: true, defaultValue: 10.0, range: "5..15"
            input "numSkyConditions", "number", title: "Number of Current Condition values to determine the Sky Condition ", description: "How many Current Conditions to store for the most common Sky Condition", required: true, defaultValue: 15.0, range: "15..60"
            input "partly", "number", title: "Percent of Expected Lux for Partly Sunny", description: "Below this %, the condition changes from Sunny to Partly Sunny by current Lux. Sun is partially obscured by light clouds.", required: true, defaultValue: 50.0, range: "30..60"
            input "mostly", "number", title: "Percent of Expected Lux for Mostly Cloudy", description: "Below this %, the condition changes from Partly Sunny to Mostly Cloudy by currrent Lux. Some dim sunlight with light shadows", required: true, defaultValue: 46.0, range: "30..60"         
            input "cloudy", "number", title: "Percent of Expected Lux for Cloudy", description: "Below this %, the condition changes from Mostly Cloudy to Cloudy. Increase if Cloudy conditons report Mostly Cloudy", required: true, defaultValue: 42.0, range: "20..50"
            input "diminished", "number", title: "Percent of Expected Lux for Diminished Light", description: "Below this %, condition changes from Cloudy to Diminished Light. Increase if blocked sensor is reporting Cloudy", required: true, defaultValue: 10.0, range: "1..30"      
        }
        section("Contact Settings") {
            input "percentClosed", "number", title: "Percent of Expected Lux to Close Contact", description: "Contact will be set to closed or open based on crossing this percent threshold, to trigger shades and curtains", required:true, defaultValue: 60.0, range: "40..100"
            input "minutesClosed", "enum", title: "Number of minutes Contact will stay Closed", description: "How many minutes to ignore if lux percent drops below the close percent setting (contact won't re-open for clouds during this time)", defaultValue: "5", options:[1:"1",5:"5",6:"6",7:"7",8:"8",9:"9",10:"10",11:"11",12:"12",13:"13",14:"14",15:"15",20:"20",30:"30",45:"45"]
            input "conditionClosed", "enum", title: "Require a Current Condtion Level to Close Contact ", description: "Current Condition Level Required to Close contact. Everything at or above the chosen value will allow contact to close", defaultValue: 0, options:[0:"No Restriction",1:"Sunny",2:"Mostly Sunny",3:"Partly Sunny ",4:"Partly Cloudy"]
        }
    }
}

def installed() {
    log.info "SunCalc Illuminance Driver Installed"
    initStates()
    runIn(2,updated)
}

def initStates() {
    state.lastCondition = "Not Set"
    state?.delayOpen = false
    
    clearListValues()

    sendEvent(name: "sensorIlluminance", value: 0.0)
    sendEvent(name: "illuminance", value: 0.0)
    sendEvent(name: "rawLux", value: 0.0)
    sendEvent(name: "sensorPercent", value: "0%")
    sendEvent(name: "oscillation", value: "Rising")
    sendEvent(name: "solarIrradiance", value: 0.0)
    sendEvent(name: "altitude", value: 0.0)
    sendEvent(name: "azimuth", value: 0.0)
    sendEvent(name: "contact", value: "open")
    sendEvent(name: "condition", value: "Not Set")
    sendEvent(name: "skyCondition", value: "Not Set")
}

/* Clear and/or initialize all the stored list values */
def clearListValues() {
    state.valueList = []
    state.luxList = []
    state.skyConditionList = ["Sunny","Partly Sunny","Mostly Sunny","Partly Cloudy","Mostly Cloudy","Cloudy"]
    state.conditionList = ["Sunny","Cloudy"]
    if (logEnable) logDebug("list are cleared and initialized")
    sendEvent(name: "contact", value: "open")
}

def updated() {
    unschedule()
    log.info "updated()"
    if (logEnable) runIn(7200,logsOff)

    if(autoUpdate) {
        def updateIntervalCmd = (settings?.updateInterval ?: "1 Minutes").replace(" ", "")
        "runEvery${updateIntervalCmd}"(refresh)
    } else {unschedule("refresh")}

    sendEvent(name: "calFactor", value: settings?.calibrationFactor)

    //if (!settings?.sensorUpdate) initialize()   // calc on update
    if (settings?.sensorUpdate) {device.updateSetting("autoUpdate",[value:"false",type:"bool"])}  // turn off auto if sensor update
}

def resetOpenDelay() {
    state?.delayOpen = false 
}

def initialize() {
    refresh()
}

def parse(String description) {
}

/* refersh calculations without sensor input */
def refresh() {
    calculate(-1)  
}

/* calculate */
def calculate(lux) {
    
    // Calculate new solar values && coords.altitude >= 6.0
    def coords = getPosition()
    setOscillation(coords.altitude)
    def irradiance = calculateSolarIrradiance(coords.altitude, coords.azimuth)
    def illuminance = calculateIlluminance(irradiance, coords.altitude)
    
    sendEvent(name: "altitude", value: coords.altitude)
    sendEvent(name: "azimuth", value: coords.azimuth)
    sendEvent(name: "solarIrradiance", value: irradiance)
    sendEvent(name: "illuminance", value: illuminance)
    sendEvent(name: "lastCalculated", value: new Date().format("yyyy-MM-dd h:mm", location.timeZone))  
     
    if (lux != -1) {updateConditions(coords.altitude)} //setConditions(illuminance, lux, coords.altitude)}    //calculate conditions if called from setSensorIlluminance()
}

/*
/// Altitude and Azimuth Calculations
*/

/* date/time constants and conversions */
def dayMs() { return 1000 * 60 * 60 * 24 }
def J1970() { return 2440588 }
def J2000() { return 2451545 }
def rad() { return  Math.PI / 180 }
def e() { return  rad() * 23.4397 } // obliquity of the Earth

def toJulian() {
    def date = new Date()
    date = date.getTime() / dayMs() - 0.5 + J1970()
    return date
}
def fromJulian(j)  { return new Date((j + 0.5 - J1970()) * dayMs()) }
def toDays(){ return toJulian() - J2000() }

// general calculations for position

def rightAscension(l, b) { return Math.atan2(Math.sin(l) * Math.cos(e()) - Math.tan(b) * Math.sin(e()), Math.cos(l)) }
def declination(l, b)    { return Math.asin(Math.sin(b) * Math.cos(e()) + Math.cos(b) * Math.sin(e()) * Math.sin(l)) }

def azimuth(H, phi, dec)  { return Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(dec) * Math.cos(phi)) }
def altitude(H, phi, dec) { return Math.asin(Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(H)) }

def siderealTime(d, lw) { return rad() * (280.16 + 360.9856235 * d) - lw }

// general sun calculations
def solarMeanAnomaly(d) { return rad() * (357.5291 + 0.98560028 * d) }

def eclipticLongitude(M) {
	def C = rad() * (1.9148 * Math.sin(M) + 0.02 * Math.sin(2 * M) + 0.0003 * Math.sin(3 * M)) // equation of center
	def P = rad() * 102.9372 // perihelion of the Earth
    return M + C + P + Math.PI
}

def sunCoords(d) {
    def M = solarMeanAnomaly(d)
    def L = eclipticLongitude(M)
	return [dec: declination(L, 0), ra: rightAscension(L, 0)]
}

/* calculates sun position for a given date and latitude/longitude */
def getPosition() {
	def lng = location.longitude
   	def lat = location.latitude

    def lw  = rad() * -lng
    def phi = rad() * lat
    def d   = toDays()
    def c  = sunCoords(d)
    def H  = siderealTime(d, lw) - c.ra

    def az = azimuth(H, phi, c.dec)
    az = (az * 180 / Math.PI) + 180

    def al = altitude(H, phi, c.dec)
    al = al * 180 / Math.PI

    return [
        azimuth: az,
        altitude: al,
    ]
}

/* ---- NEW CALCULATION METHODS ADDED ----- */

/* Apply Calibration factor to the calculated lux value */
def applyCalibration(luxValue) {
    def factor = settings?.calibrationFactor ?: 1.0
    def offset = settings?.calibrationOffset ?: 1.0
    def altitude = device.currentValue("altitude") ?: 1.0
    
    // Ensure factor is a numeric type
    if (factor instanceof String && factor.isNumber()) {
        factor = factor.toDouble()
    } else if (factor instanceof Integer || factor instanceof BigDecimal) {
        factor = factor.toDouble()
    }

    def calibratedValue = luxValue * factor
    if (factor < 0) {calibratedValue = luxValue - (Math.abs(calibratedValue) - luxValue)} // allow negative calibration

    // Ensure lux gets to 0 but not less
    if (altitude < -16.0 || calibratedValue < 0) {calibratedValue = 0}

    return calibratedValue
}

 /* Sets Oscillation (Rising or Setting) and call auto calbrate*/
def setOscillation(altitude) {
    def oscillation = "Rising"
    def current = device.currentValue("altitude").toFloat()
    if (altitude < device.currentValue("altitude").toFloat()) {
        oscillation = "Setting"
    }
    sendEvent(name: "oscillation", value: oscillation)
    state.oscillation = oscillation    

    if (settings?.autoCalibrate) {autoCalibrateSensor()}
}

/* auto calibrate the sensor based on current lux and sensor values */
def autoCalibrateSensor() {
    def lux = device.currentValue("illuminance").toFloat()
    def sensor = device.currentValue("sensorIlluminance").toFloat()

    def factor = -(lux / sensor)
    device.updateSetting("calibrationFactor",[value: factor, type: "number"])
    device.updateSetting("autoCalibrate",[value: "false", type:"bool"])
}

/**
 * @param altitude Sun's altitude in degrees.
 * @param azimuth Sun's azimuth in degrees.
 * @return Solar irradiance in W/m².
 */
def calculateSolarIrradiance(altitude, azimuth) {
    return standardIrradianceCalculation(altitude, azimuth)
}

/* Simulate beginning and end of day atmospheric light when there is no irradiance */
def twilightIlluminanceCalculaton(altitude) {
    def lux = 310.0 * Math.exp(0.377 * altitude)
    return lux
}

/**
  * irradiance physics calculation.
  * Calculates solar irradiance (W/m²) on a surface with a given tilt (Incident Angle Pefrence).
 */
 def standardIrradianceCalculation(altitude, azimuth) {
    def incidenceAngleDeg = settings?.incidenceAngle ?: 0       // set in prefrences

    def incidenceAngleRad = Math.toRadians(incidenceAngleDeg)
    def altitudeRad = Math.toRadians(altitude)
    def azimuthRad = Math.toRadians(azimuth)

    // Calculate the angle of incidence (cosTheta)
    def surfaceAzimuthRad = Math.PI // Surface faces equator
    def cosTheta = Math.sin(altitudeRad) * Math.cos(incidenceAngleRad) +
                   Math.cos(altitudeRad) * Math.sin(incidenceAngleRad) * Math.cos(azimuthRad - surfaceAzimuthRad)
    cosTheta = Math.max(0, cosTheta)

    // Simple clear sky model
    def solarConstant = 1367.0 // W/m²
    def dayOfYear = new Date().format("D").toInteger()
    def eccentricityFactor = 1 + 0.033 * Math.cos(2 * Math.PI * (dayOfYear - 3) / 365.25)

    // Atmospheric transmittance
    // Use a minimum altitude of 0.1 degrees for air mass calculation to avoid extreme values
    def apparentAltitudeForAirMass = altitude
    def airMass = 1.0 / (Math.sin(Math.toRadians(apparentAltitudeForAirMass)) + 0.50572 * Math.pow(apparentAltitudeForAirMass + 6.07995, -1.6364))
    def transmittance = Math.exp(-0.2 * airMass)

    return solarConstant * eccentricityFactor * transmittance * cosTheta
 }

/**
 * Converts solar irradiance (W/m²) to illuminance (lux).
 * Uses an average luminous efficacy in lm/W (120 lm/W is direct sunlight).
 * Uses simulated Twilight Calcs for low and negative altitudes
 * Adds the calculated Lux to the Lux List for conditions
 */
def calculateIlluminance(irradiance, altitude) {
    if (logEnable) logDebug("calulateIlluminance ${irradiance} called")

    def averageLuminousEfficacy = 120.0  // Default 120.0 lm/W for direct sunlight   
    
    // at low atitudes, check for the intersection of twilight lux  with standard lux for a smooth crossover    
    def useLux = (irradiance * averageLuminousEfficacy)        // standard calc based on irradiance for most of the day
    sendEvent(name: "rawLux", value: Math.round(useLux))
    def twilightCalc = twilightIlluminanceCalculaton(altitude) // simulated for low/negative altitudes

    if (twilightCalc > applyCalibration(useLux) && altitude < 6.0) {  
        useLux = twilightCalc
        if (logEnable) logDebug("Twilight lux is being used.  Value: ${useLux}")
    } else {
        useLux = applyCalibration(useLux)
        if (logEnable) logDebug("Standard lux is being used.  Value: ${useLux}")
    }

    addCalulatedLuxToList(useLux)

    return Math.round(useLux)
}

/**
*
* Set Sensor Illuminance from an external sensor and then call method to calculate sky condition
*
*/
def setSensorIlluminance(lux) {
	sendEvent(name: "sensorIlluminance", value: lux.toInteger())
    addSensorLuxToList(lux)

    // set conditions to night on the last sensor lux value report, else 
    if (lux == 0) {
        if (settings?.sensorUpdate && !settings?.autoUpdate) {
            sendEvent(name: "condition", value: "Night")
            sendEvent(name: "skyCondition", value: "Night")
            clearListValues()
        }
    } else if (settings?.sensorUpdate) {
        calculate(lux)  // calculate(lux) will call setConditions() below after calcs are done
    }     
}

// use the updated sensor lux and caluclated illuminance to set and update conditions
/* def setConditions(illuminance, lux, altitude) {
    //def difference = illuminance - lux
    
    
} */

/*
* -- Set Current Condition  --
*/ 
def updateConditions(altitude) {
    if (logEnable) logDebug("updateConditions called")
 
    def lastCondition = device.currentValue("condition")  
    def oscillation = device.currentValue("oscillation")
    def condition = "Not Set"
    
    if (logEnable) logDebug("altitude ${altitude}") 
    def minAltitude = settings?.minAltitude.toFloat()

    // when conditions cannot be calculated below 6.5 altitude, condition is set as the time period name, based on altitude and oscillation
    if (altitude >= minAltitude) {
        condition = determineCloudConditions(altitude)    // calculate condition from lux sensor and calculated lux at higher altitudes
    } else if (altitude >= 1.0) { 
        if (oscillation == "Rising") {condition = "Morning"}
        else {condition = "Evening"}
        if (device.currentValue("sensorPercent") != "N/A") sendEvent(name: "sensorPercent", value: "N/A")
    } 
    else if (altitude < 1.0  && altitude >= 0.0) { 
        if (oscillation == "Rising") {condition = "Dawn"}
        else {condition = "Dusk"}
    }     
    else if (altitude < 0.0 && altitude >= -1.2) {
        if (oscillation == "Rising") {condition = "Sunrise"}
        else {condition = "Sunset"}
    }     
    else if (altitude < -1.2 && altitude >= -4.0) {condition = "Twilight"}
    else {condition = "Night"}

    // SEND CURRENT CONDITION
    if (state?.lastCondition != condition) sendEvent(name: "condition", value: condition) // don't send an event if condition stayed the same
    if (altitude <= minAltitude && device.currentValue("skyCondition") != condition) sendEvent(name: "skyCondition", value: condition) // skyCondition for low altitude periods
    if (logEnable) logDebug("Condition: ${condition}")
    state.lastCondition = condition
}

/*
* Determine the Cloud Condtions and calculate a lux percent from sensor input
*/
def determineCloudConditions(altitude) {

    // Get the averages from the lists 
    def luxAverage = getListAverage(state?.luxList)             // average of the calcuated lux nominal
    def sensorAverage = getListAverage(state?.valueList)        // raw average sensor lux
    if (logEnable) logDebug("Raw Sensor Average is ${sensorAverage}")

    // set raw Percent state variable before scaling the sensor average by altitude
    def rawPercent = Math.round((sensorAverage / luxAverage) * 100)
    if (logEnable) logDebug("Raw Sensor % is ${rawPercent}")
    state.rawPercent = rawPercent

    // set the altitude factor used to normalize the sensor lux at low and high altititdes
    def factor = getLuxAltitudeFactor(altitude)
    if (logEnable) logDebug("altitude factor is ${factor}")

    // sensorAdjusted is scaled sensor average with the altitude factor applied, to fix the sensor lux average to conform to the calculated lux curve. 
    def sensorAdjusted = sensorAverage * factor
    if (sensorAdjusted < 0) {sensorAdjusted = 0}
    if (logEnable) logDebug("Sensor Adjusted is ${sensorAdjusted}")

    // Calc the adjusted sensor percent to the calculated lux average, and save to attribute
    def sensorPercent = Math.round((sensorAdjusted / luxAverage) * 100)
    if (logEnable) {logDebug("Percent Of Target: ${sensorPercent}%")}
    sendEvent(name: "sensorPercent", value: "${sensorPercent}%")

    // set cloud condition based on the adjusted sensor average compared to the lux targets that are set based on the calculated lux average
    def sunCondition = ""
    def partly = settings?.partly.toFloat(); def mostly = settings?.mostly.toFloat() 
    def cloudy = settings?.cloudy.toFloat(); def diminished = settings?.diminished.toFloat()  

    if (sensorPercent > partly) {sunCondition = "Sunny";} 
    else if (sensorPercent < partly && sensorPercent >= mostly) {sunCondition = "Partly Sunny"}
    else if (sensorPercent < mostly && sensorPercent >= cloudy) {sunCondition = "Mostly Cloudy"}
    else if (sensorPercent < cloudy && sensorPercent > diminished) {sunCondition = "Cloudy"}
    else {sunCondition = "Diminished Light"}    // Heavy clouds, snowing, storming, foggy, or blocked sensor 
    if (logEnable) logDebug("Sun Condition is ${sunCondition}")   

    // add to condition lists and get modifier from cloudy percentage
    if (sunCondition == "Diminished Light" || sunCondition == "Mostly Cloudy") {addConditionToList("Cloudy")} 
    if (sunCondition == "Sunny" || sunCondition == "Partly Sunny") {addConditionToList("Sunny")}
    def cloudPercent = getCondtionPercentage()
    
    // get a general sun modifier based on the past condition list values, only when sunCondition from % has no modifier
    def condition = sunCondition
    if (logEnable) logDebug("Current Condition unmodified is ${condition}") 
    if (condition == "Sunny" || condtion == "Cloudy") {
        def modifier = (getModifier(cloudPercent, condition))
        if (modifier != "none") {condition = modifier + " " + sunCondition}
    }

    // ADD AND SEND SKY CONDITION - the most common string in the sky condition list
    addSkyConditionToList(condition)
    def skyCondition = getMostCommonSkyCondition()
    sendEvent(name: "skyCondition", value: skyCondition)
    if (logEnable) logDebug("Sky Condition is ${skyCondition}")       
    
    // Set Contact Sensor
    if (altitude > minAltitude) {setContactSensor(sensorPercent, condition)} else {setContact("open")}

    return condition
}

/* 
*  set contact sensor value using the threshold prefrence, also set delay and sky restrictions.  
*/
def setContactSensor(sensorPercent, condition) {  
    if (logEnable) logDebug("setContactSensor(${sensorPercent}, ${condition})")
    def percentClosed = settings?.percentClosed.toInteger()
    if (sensorPercent >= percentClosed) {
        if (settings?.conditionClosed.toInteger() <= getConditionValue(condition)) {
            setContact("closed")
            if (!state?.delayOpen) {
                if (logEnable) logDebug("Setting state.delayOpen to true and will stay closed for ${settings?.minutesClosed.toInteger()} minutes")
                state.delayOpen = true
                runIn((settings?.minutesClosed.toInteger() * 60) - 15, endOpenDelay)
            }
        }
    } else if (!state?.delayOpen) {setContact("open"); if (logEnable) {logDebug("Opening Contact")}}
}
def endOpenDelay() {state.delayOpen = false; if (logEnable) {logDebug("Ending delay open - now false")}}

/* set contact attribute */
def setContact(value) {
    if (value == "open" && device.currentValue("contact") == "closed") {sendEvent(name: "contact", value: "open")}
    if (value == "closed" && device.currentValue("contact") == "open") {sendEvent(name: "contact", value: "closed")}
}

/* Return an integer condition value for contact close restrction comparison */
def getConditionValue(sky) {
    if (sky == "Sunny") return 1
    else if (sky == "Mostly Sunny") return 2
    else if (sky == "Partly Sunny") return 3
    else if (sky == "Partly Cloudy") return 4
    else return 0
}

/**
 * Get the lux adjustment factor based on sun altitude
 * Lux decreases as altitude decreases, with an altitude decrease around high noon
*/
def getLuxAltitudeFactor(altitude)  {
    def noonAltitude = getSolarNoonPosition().altitude
    if (logEnable) logDebug("noonAltitude is ${noonAltitude}")

    // setetings
    def maxAltitude = settings?.maxAltitude.toFloat()
    def power = settings?.scalingChangeRate.toFloat()
    def noonMultiplier = settings?.noonMultiplier.toFloat()
    if (logEnable) logDebug("maxAltitude is ${maxAltitude}, Power setting is ${power}, Noon Multiplier setting is ${noonMultiplier}")

    // set a high pre factor to reduce % at noon altitudes, and low pre factor to reduce % at low altitudes. 
    def preFactor
    if (altitude >= (noonAltitude * 0.5)) {preFactor = Math.pow((altitude / (noonAltitude * noonMultiplier)), (power - 3.5)); if (logEnable) logDebug("high - preFactor is ${preFactor}")}
    else if (altitude >= (maxAltitude * 0.5) && altitude < (noonAltitude * 0.5)) {preFactor = Math.pow(1 - (altitude / maxAltitude), (power - 1.0)); if (logEnable) logDebug("mid - preFactor is ${preFactor}")}
    else if (altitude < (maxAltitude * 0.5) && altitude > 8.0) {preFactor = Math.pow(1 - (altitude / maxAltitude), (power)); if (logEnable) logDebug("low - preFactor is ${preFactor}")}    
    else if (altitude <= 8.0) {preFactor = Math.pow(1 - (altitude / (maxAltitude + 0.5)), (power)); if (logEnable) logDebug("lowest - preFactor is ${preFactor}")}

    def factor = 1 - preFactor; if (logEnable) logDebug("Factor is ${factor}")

    return factor
}

/* * Get Modifier of Partly, Mostly or none from cloudy percent and condition*/
def getModifier(percent, condition) {

    if (percent > 50.0 && percent <= 80.0) {
        if (logEnable) logDebug("percent in modifier in 50-80 range with condition ${condition}")
        if (condition == "Cloudy") {return "Mostly"}
        if (condition == "Sunny") {return "Partly"}
    }
    if (percent <= 50.0 && percent >= 20.0) {
        if (logEnable) logDebug("percent in modifier in 10-49 range with condition ${condition}")
        if (condition == "Cloudy") {return "Partly"}
        if (condition == "Sunny") {return "Mostly"}
    }  

    return "none"
}

/** 
 * Calculate sun position at solar noon for current day
 * @return Map with altitude and azimuth at solar noon
 */
def getSolarNoonPosition() {
    //logDebug "Calculating solar noon position"
    
    def lng = location.longitude
    def lat = location.latitude
    
    def lw = rad() * -lng
    def phi = rad() * lat
    def d = toDays()
    def c = sunCoords(d)
    
    // At solar noon, the hour angle H is 0
    def H = 0
    
    def az = azimuth(H, phi, c.dec)
    az = (az * 180 / Math.PI) + 180
    
    def al = altitude(H, phi, c.dec)
    al = al * 180 / Math.PI
    
    def result = [
        altitude: Math.round(al * 100) / 100.0,
        azimuth: Math.round(az * 100) / 100.0
    ]
    
    if (result.altitde != state?.noonAltitude) state.noonAltitude = result.altitude
    return result
}

/* 
* Get the percentge of clouds in the condition list 
*/
def getCondtionPercentage() {

    def conditions = state?.conditionList

    if (conditions.size() >= 2.0) {
        def targetConditions = ["Sunny", "Mostly Sunny", "Cloudy"]
        def analysis = targetConditions.collectEntries { condition ->
            def count = conditions.count { it == condition }
            def percentage = (count / conditions.size()) * 100
            [condition, percentage]
        }

        return analysis['Cloudy']
    }
    // Return null if we don't have enough conditions yet
    if (logEnable) logDebug("Insufficient data for condition calculation: ${conditions.size()} values")
    return null    
}

/* return the most common entry in the sky condition list */
def getMostCommonSkyCondition() {
    def conditions = state?.skyConditionList
    return conditions.countBy { it }.max { it.value }.key
}

/*********************** List Methods used to Calc Averages and Varience for Condition  ************************/

/* Queue the calculated lux value to a list of lux values */
def addCalulatedLuxToList(lux) {
    if (!state.luxList) {
        state.luxList = []
    }
    def floatLux = lux as Float
    state.luxList << floatLux

    // Keep only the last luxValues values (if we have more than numValues)
    def numValues = (settings?.numValues).toInteger()
    if (state.luxList.size() > numValues) {
        // Keep the list to peference size by removing front elements
        def over = state?.luxList.size() - numValues
        if (over == 1) {state.luxList.removeAt(0)}
        else {
            for (int ct=0; ct < over; ct++) {
                state.luxList.removeAt(0)
            }
        }
    }

    // Log current state for debugging
    if (logEnable) logDebug("Added lux value ${lux}. Current lux list (${state.luxList.size()} items): ${state.luxList}")
}

/* Queue the sun condition to a list of sun condition values */
def addConditionToList(condition) {
    if (!state.conditionList) {
        state.conditionList = []
    }
    state.conditionList << condition

    // Keep only the last luxValues values (if we have more than numValues)
    def numValues = settings?.numConditions.toInteger()
    if (state.conditionList.size() > numValues) {
        // Keep the list to peference size by removing front elements
        def over = state?.conditionList.size() - numValues
        if (over == 1) {state.conditionList.removeAt(0)}
        else {
            for (int ct=0; ct < over; ct++) {
                state.conditionList.removeAt(0)
            }
        }
    }

    // Log current state for debugging
    if (logEnable) logDebug("Added condition ${condition}. Current condition list (${state.conditionList.size()}) items")
}

/* Queue the sun condition to a list of sun condition values */
def addSkyConditionToList(condition) {
    if (!state.skyConditionList) {
        state.skyConditionList = []
    }
    state.skyConditionList << condition

    // Keep only the last luxValues values (if we have more than numValues)
    def numValues = settings?.numSkyConditions.toInteger()
    if (state.skyConditionList.size() > numValues) {
        // Keep the list to peference size by removing front elements
        def over = state?.skyConditionList.size() - numValues
        if (over == 1) {state.skyConditionList.removeAt(0)}
        else {
            for (int ct=0; ct < over; ct++) {
                state.skyConditionList.removeAt(0)
            }
        }
    }

    // Log current state for debugging
    if (logEnable) logDebug("Added condition ${condition} to skyConditionList. Current list has (${state.skyConditionList.size()}) items")
}

/* get average of a list */
def getListAverage(list) {
    if (list.size() >= 1) {
        def sum = 0.0
        list.each { val ->
            sum += val
        }
        def mean = sum / list.size()
        return mean
    } else return 0
}

/* Add a difference value to difference value list */
def addSensorLuxToList(value) {
    // Initialize the list if it doesn't exist
    if (!state.valueList || state?.valueList == null) {
        state.valueList = []
    }
    
    // Convert value to float for accurate calculations
    def floatValue = value as Float
    
    // Add the new value to the list
    state.valueList << floatValue
    
    // Keep only the last numValues values (if we have more than numValues)
    def numValues = (settings?.numValues).toInteger()
    if (state.valueList.size() > numValues) {
        // Keep the list to peference size by removing front elements
        def over = state.valueList.size() - numValues
            for (int ct=0; ct < over; ct++) {
                state.valueList.removeAt(0)
            }
    }
    
    // Log current state for debugging
    if (logEnable) logDebug("Added value ${floatValue}. Current list (${state.valueList.size()} items): ${state.valueList}")
}

/* NOT USED 
* Calculate variance of a list
*/
private def calculateVariance(list) {
    // Safety check - should only be called with 5 values
    if (list.size() < 2) {
        log.warn "Cannot calculate variance with less than 2 values"
        return 0.0
    }
    
    // Calculate mean (average)
    def sum = 0.0
    list.each { val ->
        sum += val
    }
    def mean = sum / list.size()
    
    // Calculate sum of squared differences from mean
    def squaredDiffs = 0.0
    list.each { val ->
        def diff = val - mean
        squaredDiffs += (diff * diff)
    }
    
    // Calculate population variance (divide by n)
    def variance = squaredDiffs / list.size()
    
    return variance.round(4)  // Round to 4 decimal places for readability
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}
