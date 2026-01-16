/**
 * SunCalc Driver for Hubitat
 * A port of the SunCalc.js library https://github.com/mourner/suncalc
 * 
 * Altitude is the angle up from the horizon. Zero degrees altitude means exactly on your local horizon, and 90 degrees is "straight up". Hence, "directly underfoot" is -90 degrees altitude. 
 * Azimuth is the angle along the horizon, with zero degrees corresponding to North, and increasing in a clockwise fashion. Thus, 90 degrees is East, 180 degrees is South, and 270 degrees is West. 
 * Using these two angles, one can describe the apparent position of an object (such as the Sun at a given time).
 *
 * Solar Irradiance Calculation Added: Calculates solar power per square meter based on sun position and incidence angle
 * Lux Calculation Added: Converts solar irradiance to illuminance (lux)
 * 
 * Copyright (c) 2019 Justin Walker
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  V1.0.0 - Initial version
 *  V1.1.0 - Deepseek Added solar irradiance calculation and incidence angle preference
 *  V1.2.0 - Deepseek Added Lux calculation and attribute
 *  V1.2.1 - Fixed solar spectrum preference application in all conversion methods
 *  V1.2.2 - Added debug logging preference and illuminance offset preference
 *  V1.2.3 - Added scaling offset with calculated noon lux as maximum range
 */

metadata {
    definition (name: "SunCalc Lux With Auto-Scaling Offset", namespace: "augoisms", author: "Justin Walker") {
        capability "Actuator"
        capability "Sensor"

        command "refresh"
        command "recalculateNoonLux"

        attribute "altitude", "number"
        attribute "azimuth", "number"
        attribute "solarIrradiance", "number"
        attribute "illuminance", "number"
        attribute "lastCalculated", "string"
        attribute "noonIlluminance", "number"
        attribute "scaleFactor", "number"
    }
    preferences() {
        section("Automatic Calculations"){
            input "autoUpdate", "bool", title: "Auto Update?", required: true, defaultValue: true
            input "updateInterval", "enum", title: "Update Interval:", required: true, defaultValue: "5 Minutes", options: ["1 Minute", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
        }
        section("Solar Irradiance Settings") {
            input "incidenceAngle", "number", title: "Panel Incidence Angle (degrees)", description: "0=horizontal, 90=vertical facing equator", required: true, defaultValue: 0, range: "0..90"
            input "atmosphericPressure", "number", title: "Atmospheric Pressure (hPa)", description: "Default: 1013.25 (sea level)", required: false, defaultValue: 1013.25
            input "turbidity", "number", title: "Atmospheric Turbidity", description: "Linke factor: 2.0=clear to 5.0=hazy", required: false, defaultValue: 3.0, range: "2.0..5.0"
            input name: "irradianceInfo", type: "text", title: "Note:", description: "Solar irradiance is calculated in watts per square meter (W/m²)"
        }
        section("Lux Conversion Settings") {
            input "solarSpectrum", "enum", title: "Solar Spectrum Type:", description: "Affects W/m² to lux conversion", required: true, defaultValue: "Direct Sunlight", options: ["Direct Sunlight", "Overcast Sky", "Clear Sky", "CIE Standard Illuminant D65"]
            input "conversionMethod", "enum", title: "Conversion Method:", description: "Method for W/m² to lux conversion", required: true, defaultValue: "Average Efficacy", options: ["Average Efficacy", "Solar Constant Based", "Photopic Luminous Efficacy"]
            input "upperRangeDifference", "number", title: "Upper Range Difference (lux)", description: "Offset at noon (scales down proportionally throughout day)", required: false, defaultValue: 0
            
            input name: "luxInfo", type: "text", title: "Note:", description: "Upper range difference scales proportionally based on noon illuminance (0 lux = 0 offset)."
        }
        section("Debug Settings") {
            input "enableDebug", "bool", title: "Enable Debug Logging", description: "Log detailed calculation information", defaultValue: false
            input name: "debugInfo", type: "text", title: "Note:", description: "Debug logs will appear in Hubitat logs"
        }
    }
}

def installed() {
    logDebug "Driver installed"
    initialize()
}

def updated() {
    logDebug "updated()"
    logDebug "Debug logging enabled: ${settings?.enableDebug}"
    logDebug "Upper range difference: ${settings?.upperRangeDifference} lux"

    unschedule()
    initialize()
}

def initialize() {
    logDebug "initialize()"
    
    // Calculate noon lux for today
    calculateAndStoreNoonLux()
    
    refresh()

    if(autoUpdate) {
        def updateIntervalCmd = (settings?.updateInterval ?: "5 Minutes").replace(" ", "")
        "runEvery${updateIntervalCmd}"(refresh)
        logDebug "Scheduled auto-refresh every ${updateIntervalCmd}"
        
        // Schedule noon lux recalculation for midnight to get fresh value for new day
        schedule("0 0 * * ?", recalculateNoonLux)
        logDebug "Scheduled noon lux recalculation at midnight"
    }
}

def recalculateNoonLux() {
    logDebug "Recalculating noon lux for new day"
    calculateAndStoreNoonLux()
    refresh()
}

def calculateAndStoreNoonLux() {
    def noonLux = calculateNoonIlluminance()
    state.noonIlluminance = noonLux
    state.noonCalculatedDate = new Date().format("yyyy-MM-dd")
    
    sendEvent(name: "noonIlluminance", value: Math.round(noonLux * 10) / 10.0, unit: "lux")
    
    logDebug "Calculated noon illuminance for ${state.noonCalculatedDate}: ${noonLux} lux"
    return noonLux
}

def calculateNoonIlluminance() {
    // Calculate solar noon for current day and location
    def noonPosition = calculateSolarNoonPosition()
    def noonIrradiance = calculateSolarIrradiance(noonPosition.altitude, noonPosition.azimuth)
    def noonLux = calculateLux(noonIrradiance, noonPosition.altitude)
    
    logDebug "Noon calculation: altitude=${noonPosition.altitude}°, azimuth=${noonPosition.azimuth}°, irradiance=${noonIrradiance} W/m², lux=${noonLux}"
    
    return Math.max(0, noonLux)
}

def calculateSolarNoonPosition() {
    // Get current date at noon (12:00) in local time zone
    def now = new Date()
    def calendar = Calendar.getInstance(location.timeZone)
    calendar.time = now
    
    // Set to noon today
    calendar.set(Calendar.HOUR_OF_DAY, 12)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    
    def noonDate = calendar.time
    
    // Calculate sun position at solar noon (simplified - uses current day's declination)
    def lng = location.longitude
    def lat = location.latitude
    def lw = rad() * -lng
    def phi = rad() * lat
    
    // Calculate Julian date for noon
    def jd = noonDate.getTime() / dayMs() - 0.5 + J1970()
    def d = jd - J2000()
    
    def c = sunCoords(d)
    
    // At solar noon, hour angle is 0
    def H = 0
    def az = azimuth(H, phi, c.dec)
    az = (az * 180 / Math.PI) + 180

    def al = altitude(H, phi, c.dec)
    al = al * 180 / Math.PI
    
    // Adjust for actual solar noon (which may not be exactly 12:00)
    // This is a simplified calculation - real solar noon depends on equation of time
    def solarNoonAdjustment = calculateSolarNoonAdjustment(d)
    logDebug "Solar noon adjustment: ${solarNoonAdjustment} minutes"
    
    return [azimuth: az, altitude: al]
}

def calculateSolarNoonAdjustment(d) {
    // Calculate equation of time to adjust for solar noon vs clock noon
    def M = solarMeanAnomaly(d)
    def C = rad() * (1.9148 * Math.sin(M) + 0.02 * Math.sin(2 * M) + 0.0003 * Math.sin(3 * M))
    def L = M + C + rad() * 102.9372 + Math.PI
    
    // Equation of time in minutes: 229.18 * (0.000075 + 0.001868*cos(M) - 0.032077*sin(M) - 0.014615*cos(2*M) - 0.040849*sin(2*M))
    def eqTime = 229.18 * (0.000075 + 0.001868 * Math.cos(M) - 0.032077 * Math.sin(M) - 
                          0.014615 * Math.cos(2 * M) - 0.040849 * Math.sin(2 * M))
    
    return eqTime
}

def parse(String description) {
    // Required method for Hubitat drivers
}

def refresh() {
    logDebug "refresh() called"
    
    def coords = getPosition()
    def irradiance = calculateSolarIrradiance(coords.altitude, coords.azimuth)
    def lux = calculateLux(irradiance, coords.altitude)
    
    // Apply scaling offset using calculated noon lux
    def result = applyScalingOffset(lux)
    def finalLux = result.finalLux
    def scaleFactor = result.scaleFactor
    
    logDebug "Calculated values - Altitude: ${coords.altitude}°, Azimuth: ${coords.azimuth}°, Irradiance: ${irradiance} W/m², Raw Lux: ${lux}, Final Lux: ${finalLux}, Scale: ${scaleFactor}"
    
    irradiance = Math.round(irradiance * 10) / 10.0

    sendEvent(name: "altitude", value: Math.round(coords.altitude * 100) / 100.0)
    sendEvent(name: "azimuth", value: Math.round(coords.azimuth * 100) / 100.0)
    //sendEvent(name: "solarIrradiance", value: Math.round(irradiance * 10) / 10.0, unit: "W/m²")
    sendEvent(name: "solarIrradiance", value: irradiance, unit: "W/m²")
    sendEvent(name: "illuminance", value: Math.round(finalLux * 10) / 10.0, unit: "lux")
    sendEvent(name: "scaleFactor", value: Math.round(scaleFactor * 1000) / 1000.0)
    sendEvent(name: "lastCalculated", value: new Date().format("yyyy-MM-dd HH:mm", location.timeZone))
    
    // Update noon illuminance if not set or if it's a new day
    def today = new Date().format("yyyy-MM-dd")
    if (!state.noonCalculatedDate || state.noonCalculatedDate != today) {
        calculateAndStoreNoonLux()
    }
}

def applyScalingOffset(luxValue) {
    def upperRangeDiff = settings?.upperRangeDifference ?: 0
    
    // Convert to numeric type if needed
    if (upperRangeDiff instanceof String && upperRangeDiff.isNumber()) {
        upperRangeDiff = upperRangeDiff.toDouble()
    } else if (upperRangeDiff instanceof Integer || upperRangeDiff instanceof BigDecimal) {
        upperRangeDiff = upperRangeDiff.toDouble()
    }
    
    // Handle edge cases
    if (luxValue <= 0) {
        logDebug "Lux value is 0 or negative, returning 0"
        return [finalLux: 0.0, scaleFactor: 0.0]
    }
    
    // Get noon lux from state or calculate if missing
    def noonLux = state.noonIlluminance
    def today = new Date().format("yyyy-MM-dd")
    
    if (!noonLux || !state.noonCalculatedDate || state.noonCalculatedDate != today) {
        noonLux = calculateAndStoreNoonLux()
    }
    
    if (noonLux <= 0) {
        logDebug "Invalid noonLux (${noonLux}), using simple offset"
        return [finalLux: Math.max(0, luxValue + upperRangeDiff), scaleFactor: 1.0]
    }
    
    // Calculate scaling factor based on current lux relative to noon lux
    // Cap at 1.0 to avoid over-scaling if current lux exceeds noon lux
    def scaleFactor = Math.min(luxValue / noonLux, 1.0)
    
    // Apply scaled offset (upperRangeDiff is the maximum adjustment at noon)
    def scaledOffset = upperRangeDiff * scaleFactor
    def offsetLux = luxValue + scaledOffset
    
    logDebug "Scaling offset: raw=${luxValue}, noonLux=${noonLux}, scale=${scaleFactor.round(4)}, scaledOffset=${scaledOffset.round(1)}, final=${offsetLux.round(1)}"
    
    // Ensure lux doesn't go below 0
    return [
        finalLux: Math.max(0, offsetLux),
        scaleFactor: scaleFactor
    ]
}

///
/// Lux Calculation Methods (unchanged from previous version)
///

def calculateLux(irradiance, altitude) {
    if (altitude <= 0 || irradiance <= 0) {
        return 0.0
    }
    
    def luxValue = 0.0
    def method = settings?.conversionMethod ?: "Average Efficacy"
    def spectrum = settings?.solarSpectrum ?: "Direct Sunlight"
    
    logDebug "calculateLux: method=${method}, spectrum=${spectrum}, irradiance=${irradiance} W/m²"
    
    switch(method) {
        case "Average Efficacy":
            luxValue = calculateLuxByAverageEfficacy(irradiance, spectrum)
            break
            
        case "Solar Constant Based":
            luxValue = calculateLuxBySolarConstant(irradiance, altitude, spectrum)
            break
            
        case "Photopic Luminous Efficacy":
            luxValue = calculateLuxByPhotopicEfficacy(irradiance, spectrum)
            break
            
        default:
            luxValue = calculateLuxByAverageEfficacy(irradiance, spectrum)
            break
    }
    
    logDebug "calculateLux result: ${luxValue} lux"
    return luxValue
}

def calculateLuxByAverageEfficacy(irradiance, spectrum) {
    def efficacy = getLuminousEfficacy(spectrum)
    return irradiance * efficacy
}

def calculateLuxBySolarConstant(irradiance, altitude, spectrum) {
    def solarConstant = 1367.0
    def pressure = (settings?.atmosphericPressure ?: 1013.25).toBigDecimal()
    def airMass = calculateAirMass(altitude, pressure)
    def turbidity = settings?.turbidity ?: 3.0
    def transmittance = Math.exp(-turbidity * airMass * 0.2)
    
    def dayOfYear = new Date().format("D").toInteger()
    def eccentricity = 1 + 0.033 * Math.cos(2 * Math.PI * (dayOfYear - 3) / 365.25)
    def topOfAtmosphere = solarConstant * eccentricity * Math.sin(altitude * Math.PI / 180)
    
    def baseEfficacy = getLuminousEfficacy(spectrum)
    def atmosphericFactor = transmittance * 0.9 + 0.1
    
    return irradiance * baseEfficacy * atmosphericFactor
}

def calculateLuxByPhotopicEfficacy(irradiance, spectrum) {
    def efficacyMap = [
        "Direct Sunlight": 93.0,
        "Clear Sky": 110.0,
        "Overcast Sky": 120.0,
        "CIE Standard Illuminant D65": 100.0
    ]
    
    def efficacy = efficacyMap[spectrum] ?: 100.0
    return irradiance * efficacy
}

def getLuminousEfficacy(spectrum) {
    def efficacyMap = [
        "Direct Sunlight": 100.0,
        "Clear Sky": 115.0,
        "Overcast Sky": 125.0,
        "CIE Standard Illuminant D65": 100.0
    ]
    
    return efficacyMap[spectrum] ?: 100.0
}

///
/// Solar Irradiance Calculations (unchanged from previous version)
///

def calculateSolarIrradiance(altitude, azimuth) {
    if (altitude <= 0) {
        return 0.0
    }
    
    def incidenceAngleDeg = settings?.incidenceAngle ?: 0
    def pressure = (settings?.atmosphericPressure ?: 1013.25).toBigDecimal()
    def turbidity = settings?.turbidity ?: 3.0
    
    logDebug "calculateSolarIrradiance: altitude=${altitude}°, azimuth=${azimuth}°, incidence=${incidenceAngleDeg}°"
    
    def directIrradiance = calculateDirectRadiation(altitude, pressure, turbidity)
    def diffuseIrradiance = calculateDiffuseRadiation(altitude, turbidity)
    def totalIrradiance = calculateSurfaceIrradiance(directIrradiance, diffuseIrradiance, altitude, azimuth, incidenceAngleDeg)
    
    return Math.round(totalIrradiance * 10) / 10.0
}

def calculateDirectRadiation(altitude, pressure, turbidity) {
    def altitudeRad = altitude * Math.PI / 180
    def solarConstant = 1367.0
    def airMass = calculateAirMass(altitude, pressure)
    
    def dayOfYear = new Date().format("D").toInteger()
    def eccentricity = 1 + 0.033 * Math.cos(2 * Math.PI * (dayOfYear - 3) / 365.25)
    def extraterrestrial = solarConstant * eccentricity
    def transmittance = Math.exp(-turbidity * airMass * 0.2)
    def directIrradiance = extraterrestrial * transmittance * Math.sin(altitudeRad)
    
    return Math.max(0, directIrradiance)
}

def calculateAirMass(altitude, pressure) {
    def altitudeRad = altitude * Math.PI / 180
    def airMass = 1.0 / (Math.sin(altitudeRad) + 0.50572 * Math.pow((6.07995 + altitude), -1.6364))
    def pressureRatio = (pressure.toBigDecimal() / 1013.25.toBigDecimal())
    
    return airMass * pressureRatio
}

def calculateDiffuseRadiation(altitude, turbidity) {
    def altitudeRad = altitude * Math.PI / 180
    def diffuseFraction = 0.1 + (turbidity - 2.0) * 0.05 + (1 - Math.sin(altitudeRad)) * 0.3
    
    def dayOfYear = new Date().format("D").toInteger()
    def eccentricity = 1 + 0.033 * Math.cos(2 * Math.PI * (dayOfYear - 3) / 365.25)
    def baseIrradiance = 1367.0 * eccentricity * Math.sin(altitudeRad)
    def diffuseIrradiance = baseIrradiance * diffuseFraction
    
    return Math.max(0, diffuseIrradiance)
}

def calculateSurfaceIrradiance(directIrradiance, diffuseIrradiance, altitude, azimuth, incidenceAngleDeg) {
    def altitudeRad = altitude * Math.PI / 180
    def incidenceAngleRad = incidenceAngleDeg * Math.PI / 180
    
    def cosineFactor = Math.cos(incidenceAngleRad - altitudeRad)
    cosineFactor = Math.max(0, cosineFactor)
    
    def directOnSurface = directIrradiance * cosineFactor
    def diffuseOnSurface = diffuseIrradiance * 0.5 * (1 + Math.cos(incidenceAngleRad))
    
    return directOnSurface + diffuseOnSurface
}

///
/// Sun Position Calculations (unchanged from previous version)
///

def dayMs() { return 1000 * 60 * 60 * 24 }
def J1970() { return 2440588 }
def J2000() { return 2451545 }
def rad() { return  Math.PI / 180 }
def e() { return  rad() * 23.4397 }

def toJulian() { 
    def date = new Date()
    date = date.getTime() / dayMs() - 0.5 + J1970()
    return date   
}

def fromJulian(j)  { return new Date((j + 0.5 - J1970()) * dayMs()) }
def toDays(){ return toJulian() - J2000() }

def rightAscension(l, b) { return Math.atan2(Math.sin(l) * Math.cos(e()) - Math.tan(b) * Math.sin(e()), Math.cos(l)) }
def declination(l, b)    { return Math.asin(Math.sin(b) * Math.cos(e()) + Math.cos(b) * Math.sin(e()) * Math.sin(l)) }

def azimuth(H, phi, dec)  { return Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(dec) * Math.cos(phi)) }
def altitude(H, phi, dec) { return Math.asin(Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(H)) }

def siderealTime(d, lw) { return rad() * (280.16 + 360.9856235 * d) - lw }

def solarMeanAnomaly(d) { return rad() * (357.5291 + 0.98560028 * d) }

def eclipticLongitude(M) {
    def C = rad() * (1.9148 * Math.sin(M) + 0.02 * Math.sin(2 * M) + 0.0003 * Math.sin(3 * M))
    def P = rad() * 102.9372
    return M + C + P + Math.PI
}

def sunCoords(d) {
    def M = solarMeanAnomaly(d)
    def L = eclipticLongitude(M)
    return [dec: declination(L, 0), ra: rightAscension(L, 0)]
}

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

    return [azimuth: az, altitude: al]
}

///
/// Utility Methods
///

def logDebug(msg) {
    if (settings?.enableDebug) {
        log.debug "SunCalc: ${msg}"
    }
}