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
 *  V1.1.0 - Added solar irradiance and illuminance calculations with incident angle preference
 *  V1.2.0 - Added preference for a calibration Factor to calibrate illuminance value to a sensor
 *  V1.3.0 - Added days only scheduling to only auto Update between sunrise and sunset
 */

import java.util.Calendar

metadata {
    definition (name: "SunCalc Driver with Illuminance", namespace: "augoisms", author: "Justin Walker") {
        capability "Actuator"
        capability "Sensor"

        command "refresh"

        attribute "altitude", "number"
        attribute "azimuth", "number"
        attribute "solarIrradiance", "number"
        attribute "illuminance", "number"
        attribute "rawLux", "number"
        attribute "sunRise", "date"
        attribute "sunSet", "date"
        attribute "lastCalculated", "string"
    }
    preferences() {
        section("Automatic Calculations"){
            input "autoUpdate", "bool", title: "Auto Update?", required: true, defaultValue: true
            input "dayUpdateOnly", "bool", title:"Auto Update only between Sunrise and Sunset?", required: true, defaultValue: true
            input "updateInterval", "enum", title: "Update Interval:", required: true, defaultValue: "5 Minutes", options: ["1 Minute", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
        }
        section("Solar Radiation Settings") {
            input "incidenceAngle", "number", title: "Surface Incidence Angle (degrees)", description: "0=horizontal, 90=vertical", required: true, defaultValue: 0, range: "0..90"
            input "calibrationFactor", "number", title: "Lux Calibration Factor", description: "Multiplier to illuminance based on raw lux (e.g., 1.1 = +10%, 1.0 = no correction)", required: true, defaultValue: 1.0
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
    }   

    if(autoUpdate) {
        def updateIntervalCmd = (settings?.updateInterval ?: "1 Minutes").replace(" ", "")
        "runEvery${updateIntervalCmd}"(refresh)
    } else (unschedule("refresh"))
}

def parse(String description) {
}

def setDayOnlyRefresh() {
    def riseAndSet = getSunriseAndSunset()
    def sunRise = riseAndSet.sunrise
    def sunSet = riseAndSet.sunset     
    def calendar = Calendar.getInstance()

    // schedule enable autoUpdate at sunrise
    calendar.setTime(sunRise)
    Integer hour24Rise = calendar.get(Calendar.HOUR_OF_DAY)
    Integer minuteRise = calendar.get(Calendar.MINUTE)
    def riseSchedule = "0 ${minuteRise} ${hour24Rise} * * ?"
    schedule(riseSchedule, enableAutoUpdate) 
    
    // schedule disable autoUpdate at sunset
    calendar.setTime(sunSet)
    Integer hour24Set = calendar.get(Calendar.HOUR_OF_DAY)
    Integer minuteSet = calendar.get(Calendar.MINUTE)
    def setSchedule = "0 ${minuteSet} ${hour24Set} * * ?"
    schedule(setSchedule, disableAutoUpdate)

    sendEvent(name: "sunRise", value: sunRise)
    sendEvent(name: "sunSet", value: sunSet)
}

def setSunriseSunset() {

    def riseAndSet = getSunriseAndSunset()
    def sunRise = riseAndSet.sunrise
    def sunSet = riseAndSet.sunset     

    if (which == "sunRise") {
       sendEvent(name: "sunRise", value: sunRise)
    }
    if (which == "sunRise") {
        sendEvent(name: "sunSet", value: sunSet)
    }    
}

def enableAutoUpdate() {
    device.updateSetting("autoUpdate",[value:"true", type:"bool"])
    updated()
}

def disableAutoUpdate() {
    device.updateSetting("autoUpdate",[value:"false", type:"bool"])
    updated()
}

def refresh()
{
    def coords = getPosition()
    // Calculate new solar values
    def irradiance = calculateSolarIrradiance(coords.altitude, coords.azimuth)
    def lux = Math.round(calculateIlluminance(irradiance))
    def calLux = Math.round(applyCalibrationFactor(lux))

    sendEvent(name: "altitude", value: coords.altitude)
    sendEvent(name: "azimuth", value: coords.azimuth)
    sendEvent(name: "solarIrradiance", value: irradiance)
    sendEvent(name: "illuminance", value: calLux)
    sendEvent(name: "rawLux", value: lux)
    sendEvent(name: "lastCalculated", value: new Date().format("yyyy-MM-dd h:mm", location.timeZone))
}

def applyCalibrationFactor(luxValue) {
    def factor = settings?.calibrationFactor ?: 1.0
    
    // Ensure factor is a numeric type
    if (factor instanceof String && factor.isNumber()) {
        factor = factor.toDouble()
    } else if (factor instanceof Integer || factor instanceof BigDecimal) {
        factor = factor.toDouble()
    }
    
    // Apply calibration factor
    def calibratedValue = luxValue * factor
    
    // Ensure lux doesn't go below 0
    return Math.max(0, calibratedValue)
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
 * @param altitude Sun's altitude in degrees.
 * @param azimuth Sun's azimuth in degrees.
 * @return Solar irradiance in W/m².
 */
def calculateSolarIrradiance(altitude, azimuth) {
    if (altitude <= 0) {
        return 0.0 // Sun is below the horizon
    }

    def incidenceAngleDeg = settings?.incidenceAngle ?: 0
    def incidenceAngleRad = Math.toRadians(incidenceAngleDeg)
    def altitudeRad = Math.toRadians(altitude)
    def azimuthRad = Math.toRadians(azimuth)
    def latRad = Math.toRadians(location.latitude)

    // Calculate the angle of incidence (theta) using a simplified spherical law of cosines
    // cos(theta) = sin(alt) * cos(incidence) + cos(alt) * sin(incidence) * cos(azimuth - surface_azimuth)
    // Assuming surface azimuth faces equator (South in N hemisphere, North in S hemisphere) = 180°
    def surfaceAzimuthRad = Math.PI
    def cosTheta = Math.sin(altitudeRad) * Math.cos(incidenceAngleRad) +
                   Math.cos(altitudeRad) * Math.sin(incidenceAngleRad) * Math.cos(azimuthRad - surfaceAzimuthRad)

    cosTheta = Math.max(0, cosTheta) // Ensure non-negative

    // Simple clear sky model: Extraterrestrial irradiance * atmospheric transmittance
    def solarConstant = 1367.0 // W/m²
    def dayOfYear = new Date().format("D").toInteger()
    def eccentricityFactor = 1 + 0.033 * Math.cos(2 * Math.PI * (dayOfYear - 3) / 365.25)

    // Atmospheric transmittance (simplified, decreases with lower sun angle)
    def airMass = 1.0 / (Math.sin(altitudeRad) + 0.50572 * Math.pow(altitude + 6.07995, -1.6364))
    def transmittance = Math.exp(-0.2 * airMass) // Simplified coefficient

    def irradiance = solarConstant * eccentricityFactor * transmittance * cosTheta
    return Math.round(irradiance * 10) / 10.0 // Round to 1 decimal place
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