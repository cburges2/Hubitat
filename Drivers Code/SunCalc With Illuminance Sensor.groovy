/**
 * SunCalc Driver for Hubitat
 *
 * A port of the SunCalc.js library https://github.com/mourner/suncalc
 *
 * Altitude is the angle up from the horizon. Zero degrees altitude means exactly on your local horizon, and 90 degrees is "straight up". Hence, "directly underfoot" is -90 degrees altitude. 
 * Azimuth is the angle along the horizon, with zero degrees corresponding to North, and increasing in a clockwise fashion. Thus, 90 degrees is East, 180 degrees is South, and 270 degrees is West. 
 * Using these two angles, one can describe the apparent position of an object (such as the Sun at a given time).
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
 *
 *  Modified by C Burgess, January, 2026
 *  V1.1.0 - Added solar irradiance and illuminance calculations with incident angle preference
 *  V1.2.0 - Added preference for a calibration Factor to calibrate illuminance value to a sensor
 *  V1.3.0 - Added days only scheduling to only auto Update between sunrise and sunset
 *  V1.4.0 - Added Period of the day attribute and get method
 *  V1.5.0 - Added sensorIlluminance to be set from external source, and calulate sensorPeriod when set. 
             Added preference to refresh calcs when sensorIlluminance is updated.  
 */

import java.util.Calendar

metadata {
    definition (name: "SunCalc With Illuminance Sensor", namespace: "augoisms", author: "Justin Walker") {
        capability "Actuator"
        capability "Sensor"
        capability "IlluminanceMeasurement"
        command "refresh"

        attribute "altitude", "number"
        attribute "azimuth", "number"
        attribute "solarIrradiance", "number"
        attribute "illuminance", "number"
        attribute "rawLux", "number"
        attribute "calcStart", "string"
        attribute "calcStop", "string"
        attribute "lastCalculated", "string"
        attribute "sensorIlluminance", "number"

        command "setDayOnlyRefresh"
        command "setSensorIlluminance",[[name:"Sensor Illuminance",type: "NUMBER", description: "Set External Sensor Illuminance Value"]]
    }
    preferences() {
        section("Automatic Calculations"){
            input "autoUpdate", "bool", title: "Auto Update?", required: true, defaultValue: true
            input "dayUpdateOnly", "bool", title:"Schedule turning Auto Update On 30 minutes before sunrise and Off 30 minutes after sunset", required: true, defaultValue: true
            input "updateInterval", "enum", title: "Update Interval:", required: true, defaultValue: "5 Minutes", options: ["1 Minute", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
            input "sensorUpdate", "bool", title: "Update when Sensor Illuminance Updates?", required: true, defaultValue: false       
        }
        section("Solar Radiation Settings") {
            input "incidenceAngle", "number", title: "Surface Incidence Angle (degrees)", description: "0=horizontal, 90=vertical", required: true, defaultValue: 0, range: "0..90"
            input "calibrationFactor", "number", title: "Lux Calibration Factor", description: "Multiplier to illuminance based on raw lux (e.g., 1.1 = +10%, 1.0 = no correction)", required: true, defaultValue: 1.0
            input "calibrationOffset", "number", title: "Lux Calibration Offset", description: "Offset to illuminance based on raw lux (e.g., +/-100 = +/-100Lux, 0 = no correction)", required: true, defaultValue: 0.0
            input "twilightLimit", "number", title: "Twilight Limit (degrees)", description: "Sun altitude at which illuminance becomes zero. Must be negative (e.g., -6 for civil twilight, -12 for nautical, -18 for astronomical).", required: true, defaultValue: -6, range: "-18..0"        
        }
    }
}

def updated() {
    log.debug "updated()"

    unschedule()
    refresh()

    if (dayUpdateOnly) {
        log.debug "dayUpdateOnly set"
        schedule("0 0 02 * * ?", setDayOnlyRefresh) // set sunrsise and sunset auto update scheduling at 2am every day
        setDayOnlyRefresh()
    } else {
        unschedule("setDayOnlyRefresh")
        unschedule("enableAutoUpdate")
        unschedule("disableAutoUpdate")
        sendEvent(name: "calcStart", value: "disabled")
        sendEvent(name: "calcStop", value: "disabled")        
    }   

    if(autoUpdate) {
        def updateIntervalCmd = (settings?.updateInterval ?: "1 Minutes").replace(" ", "")
        "runEvery${updateIntervalCmd}"(refresh)
    } else (unschedule("refresh"))
}

def parse(String description) {
}

def setDayOnlyRefresh() {
    if (!settings?.dayUpdateOnly) {device.updateSetting("dauUpdateOnly",[value:"true", type:"bool"])}
    def riseAndSet = getSunriseAndSunset(sunriseOffset: -30, sunsetOffset: +30)   
    def calendar = Calendar.getInstance()

    // schedule autoUpdate to turn on at sunrise
    calendar.setTime(riseAndSet.sunrise)
    def startSchedule = "0 ${calendar.get(Calendar.MINUTE)} ${calendar.get(Calendar.HOUR_OF_DAY)} * * ?"
    schedule(startSchedule, enableAutoUpdate) 
    
    // schedule autoUpdate to turn off at sunset
    calendar.setTime(riseAndSet.sunset)
    def stopSchedule = "0 ${calendar.get(Calendar.MINUTE)} ${calendar.get(Calendar.HOUR_OF_DAY)} * * ?"
    schedule(stopSchedule, disableAutoUpdate)

    sendEvent(name: "calcStart", value: riseAndSet.sunrise)
    sendEvent(name: "calcStop", value: riseAndSet.sunset)
}

def enableAutoUpdate() {
    device.updateSetting("autoUpdate",[value:"true", type:"bool"])
    updated()
}

def disableAutoUpdate() {
    device.updateSetting("autoUpdate",[value:"false", type:"bool"])
    updated()
}

def refresh() {
    def coords = getPosition()
    // Calculate new solar values
    def irradiance = calculateSolarIrradiance(coords.altitude, coords.azimuth)
    def lux = Math.round(calculateIlluminance(irradiance))
    def calLux = Math.round(applyCalibrations(lux))

    sendEvent(name: "altitude", value: coords.altitude)
    sendEvent(name: "azimuth", value: coords.azimuth)
    sendEvent(name: "solarIrradiance", value: irradiance)
    sendEvent(name: "illuminance", value: calLux)
    sendEvent(name: "rawLux", value: lux)
    sendEvent(name: "lastCalculated", value: new Date().format("yyyy-MM-dd h:mm", location.timeZone))

}

def applyCalibrations(luxValue) {
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
    // Apply Difference Offset
    calibratedValue = calibratedValue + offset    

    
    if (factor < 0) {calibratedValue = luxValue - (Math.abs(calibratedValue) - luxValue)} // negative calibration

    // Ensure lux gets to 0 but not less
    if (altitude < -4.2 || calibratedValue < 0) {calibratedValue = 0}

    return calibratedValue
}

///
/// Calculations
///

// date/time constants and conversions
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

// calculates sun position for a given date and latitude/longitude

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

// --- NEW CALCULATION METHODS ADDED BELOW ---

/**
 * Calculates solar irradiance (W/m²) on a surface with a given tilt.
 * Modified to account for civil twilight (sun altitude 0 to twilightLimit degrees).
 * @param altitude Sun's altitude in degrees.
 * @param azimuth Sun's azimuth in degrees.
 * @return Solar irradiance in W/m².
 */
def calculateSolarIrradiance(altitude, azimuth) {
    // Get the user's twilight limit (must be negative, default -6)
    def twilightLimit = settings?.twilightLimit ?: -6.0

    // 1. Sun is above the horizon: use the standard calculation.
    if (altitude > 0) {
        return standardIrradianceCalculation(altitude, azimuth)
    }

    // 2. Sun is in twilight (between 0 and twilightLimit): calculate reduced irradiance.
    if (twilightLimit < 0 && altitude > twilightLimit && altitude <= 0) {
        // Linear fade from 100% at 0° to 0% at twilightLimit.
        def twilightFraction = 1 - (Math.abs(altitude) / Math.abs(twilightLimit))
        def horizonIrradiance = standardIrradianceCalculation(0, azimuth)
        def twilightIrradiance = horizonIrradiance * twilightFraction
        return Math.round(twilightIrradiance * 10) / 10.0
    }

    // 3. Sun is below the twilight limit (night): return 0.
    return 0.0
}

/**
 * Contains the original irradiance physics calculation.
 * Separated for clarity and re-use.
 */
private def standardIrradianceCalculation(altitude, azimuth) {
    def incidenceAngleDeg = settings?.incidenceAngle ?: 0
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
    def apparentAltitudeForAirMass = altitude  //Math.max(altitude, 0.1)
    def airMass = 1.0 / (Math.sin(Math.toRadians(apparentAltitudeForAirMass)) + 0.50572 * Math.pow(apparentAltitudeForAirMass + 6.07995, -1.6364))
    def transmittance = Math.exp(-0.2 * airMass)

    def irradiance = solarConstant * eccentricityFactor * transmittance * cosTheta
    return irradiance
}

/**
 * Converts solar irradiance (W/m²) to illuminance (lux).
 * Uses an average luminous efficacy of 120 lm/W for direct sunlight.
 * @param irradiance Solar irradiance in W/m².
 * @return Approximate illuminance in lux.
 */
def calculateIlluminance(irradiance) {
    def averageLuminousEfficacy = 120.0 // lm/W for direct sunlight
    return irradiance * averageLuminousEfficacy
}


/**
* Set Sensor Illuminance from an external source
*/
def setSensorIlluminance(lux) {
	sendEvent(name: "sensorIlluminance", value: lux)

    if (settings?.sensorUpdate) refresh()
}




