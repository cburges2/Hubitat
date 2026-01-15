/**
 * SunCalc Driver for Hubitat
 
 * A port of the SunCalc.js library https://github.com/mourner/suncalc
 
 * Altitude is the angle up from the horizon. Zero degrees altitude means exactly on your local horizon, and 90 degrees is "straight up". Hence, "directly underfoot" is -90 degrees altitude. 
 * Azimuth is the angle along the horizon, with zero degrees corresponding to North, and increasing in a clockwise fashion. Thus, 90 degrees is East, 180 degrees is South, and 270 degrees is West. 
 * Using these two angles, one can describe the apparent position of an object (such as the Sun at a given time).

 * Solar Irradiance Calculation Added: Calculates solar power per square meter based on sun position and incidence angle
 * Lux Calculation Added: Converts solar irradiance to illuminance (lux)

 * Copyright (c) 2019 Justin Walker

 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *  V1.0.0 - Initial version
 *  V1.1.0 - Deepseek Added solar irradiance calculation and incidence angle preference
 *  V1.2.0 - Deepseek Added Lux calculation and attribute
 */

metadata {
    definition (name: "SunCalc Lux Driver", namespace: "augoisms", author: "Justin Walker") {
        capability "Actuator"
        capability "Sensor"

        command "refresh"

        attribute "altitude", "number"
        attribute "azimuth", "number"
        attribute "solarIrradiance", "number"
        attribute "illuminance", "number"
        attribute "lastCalculated", "string"    
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
            input name: "luxInfo", type: "text", title: "Note:", description: "Lux measures visible light intensity to human eye"
        }
    }
}

def updated() {
    log.debug "updated()"

    unschedule()
    refresh()

    if(autoUpdate) {
        def updateIntervalCmd = (settings?.updateInterval ?: "1 Minutes").replace(" ", "")
        "runEvery${updateIntervalCmd}"(refresh)
    }        
}

def parse(String description) {
}

def refresh()
{
    def coords = getPosition()
    def irradiance = calculateSolarIrradiance(coords.altitude, coords.azimuth)
    def lux = calculateLux(irradiance, coords.altitude)

    sendEvent(name: "altitude", value: coords.altitude)
    sendEvent(name: "azimuth", value: coords.azimuth)
    sendEvent(name: "solarIrradiance", value: irradiance, unit: "W/m²")
    sendEvent(name: "illuminance", value: lux, unit: "lux")
    sendEvent(name: "lastCalculated", value: new Date().format("yyyy-MM-dd h:mm", location.timeZone))

    log.debug "SunCalc: Altitude: ${coords.altitude}°, Azimuth: ${coords.azimuth}°, Irradiance: ${irradiance} W/m², Illuminance: ${lux} lux"
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

///
/// Solar Irradiance Calculations
///

def calculateSolarIrradiance(altitude, azimuth) {
    // Return 0 if sun is below horizon
    if (altitude <= 0) {
        return 0.0
    }

    // Get user preferences with defaults
    def incidenceAngleDeg = settings?.incidenceAngle ?: 0
    // Ensure numeric type for pressure calculation
    def pressure = (settings?.atmosphericPressure ?: 1013.25).toBigDecimal()
    def turbidity = settings?.turbidity ?: 3.0

    // Calculate solar irradiance
    def irradiance = calculateDirectRadiation(altitude, pressure, turbidity)
    def diffuseIrradiance = calculateDiffuseRadiation(altitude, turbidity)

    // Calculate total irradiance on a surface with given incidence angle
    def totalIrradiance = calculateSurfaceIrradiance(irradiance, diffuseIrradiance, altitude, azimuth, incidenceAngleDeg)

    return Math.round(totalIrradiance * 10) / 10.0 // Round to 1 decimal place
}

def calculateDirectRadiation(altitude, pressure, turbidity) {
    // Convert altitude to radians
    def altitudeRad = altitude * Math.PI / 180

    // Solar constant (W/m²)
    def solarConstant = 1367.0

    // Air mass (relative optical path length)
    def airMass = calculateAirMass(altitude, pressure)

    // Extraterrestrial radiation
    def dayOfYear = new Date().format("D").toInteger()
    def eccentricity = 1 + 0.033 * Math.cos(2 * Math.PI * (dayOfYear - 3) / 365.25)
    def extraterrestrial = solarConstant * eccentricity

    // Atmospheric transmittance (simplified Hottel model)
    def transmittance = Math.exp(-turbidity * airMass * 0.2)

    // Direct normal irradiance
    def directIrradiance = extraterrestrial * transmittance * Math.sin(altitudeRad)

    return Math.max(0, directIrradiance)
}

def calculateAirMass(altitude, pressure) {
    // Calculate relative air mass using Kasten-Young formula
    def altitudeRad = altitude * Math.PI / 180
    def airMass = 1.0 / (Math.sin(altitudeRad) + 0.50572 * Math.pow((6.07995 + altitude), -1.6364))

    // Adjust for atmospheric pressure
    def pressureRatio = (pressure.toBigDecimal() / 1013.25.toBigDecimal())
    return airMass * pressureRatio
}

def calculateDiffuseRadiation(altitude, turbidity) {
    // Simple model for diffuse radiation (circumsolar + isotropic)
    def altitudeRad = altitude * Math.PI / 180

    // Diffuse radiation fraction increases with turbidity and lower altitude
    def diffuseFraction = 0.1 + (turbidity - 2.0) * 0.05 + (1 - Math.sin(altitudeRad)) * 0.3

    // Base diffuse radiation
    def dayOfYear = new Date().format("D").toInteger()
    def eccentricity = 1 + 0.033 * Math.cos(2 * Math.PI * (dayOfYear - 3) / 365.25)
    def baseIrradiance = 1367.0 * eccentricity * Math.sin(altitudeRad)

    def diffuseIrradiance = baseIrradiance * diffuseFraction

    return Math.max(0, diffuseIrradiance)
}

def calculateSurfaceIrradiance(directIrradiance, diffuseIrradiance, altitude, azimuth, incidenceAngleDeg) {
    // Convert to radians
    def altitudeRad = altitude * Math.PI / 180
    def azimuthRad = azimuth * Math.PI / 180
    def incidenceAngleRad = incidenceAngleDeg * Math.PI / 180

    // Get location latitude
    def lat = location.latitude
    def latRad = lat * Math.PI / 180

    // Calculate sun's declination and hour angle (simplified)
    def dayOfYear = new Date().format("D").toInteger()
    def declination = 23.45 * Math.sin(2 * Math.PI * (284 + dayOfYear) / 365) * Math.PI / 180

    // Calculate solar zenith angle
    def zenithAngle = Math.PI/2 - altitudeRad

    // Calculate angle of incidence on tilted surface
    // Using simplified formula for surface facing equator (azimuth = 180° South)
    def surfaceAzimuth = 180.0 // Assuming surface faces South (common for solar panels)
    def surfaceAzimuthRad = surfaceAzimuth * Math.PI / 180

    // Calculate incidence angle using trigonometric formula
    def cosIncidence = Math.sin(declination) * Math.sin(latRad) * Math.cos(incidenceAngleRad) -
                       Math.sin(declination) * Math.cos(latRad) * Math.sin(incidenceAngleRad) * Math.cos(surfaceAzimuthRad) +
                       Math.cos(declination) * Math.cos(latRad) * Math.cos(incidenceAngleRad) * Math.cos(zenithAngle) +
                       Math.cos(declination) * Math.sin(latRad) * Math.sin(incidenceAngleRad) * Math.cos(surfaceAzimuthRad) * Math.cos(zenithAngle) +
                       Math.cos(declination) * Math.sin(incidenceAngleRad) * Math.sin(surfaceAzimuthRad) * Math.sin(zenithAngle)

    cosIncidence = Math.max(0, Math.min(1, cosIncidence))

    // Direct radiation on tilted surface
    def directOnSurface = directIrradiance * cosIncidence / Math.sin(altitudeRad)

    // Diffuse radiation on tilted surface (simplified isotropic model)
    def diffuseOnSurface = diffuseIrradiance * (1 + Math.cos(incidenceAngleRad)) / 2

    // Ground reflected radiation (albedo = 0.2 typical)
    def albedo = 0.2
    def groundReflected = (directIrradiance + diffuseIrradiance) * albedo * (1 - Math.cos(incidenceAngleRad)) / 2

    // Total irradiance on surface
    def totalIrradiance = directOnSurface + diffuseOnSurface + groundReflected

    return Math.max(0, totalIrradiance)
}

///
/// Lux (Illuminance) Calculations
///

def calculateLux(irradiance, altitude) {
    // Return 0 if no solar irradiance
    if (irradiance <= 0 || altitude <= 0) {
        return 0
    }

    // Get user preferences
    def spectrumType = settings?.solarSpectrum ?: "Direct Sunlight"
    def conversionType = settings?.conversionMethod ?: "Average Efficacy"

    // Conversion factors based on solar spectrum type (lumens per watt)
    def efficacy = 120.0 // Default average efficacy

    switch(spectrumType) {
        case "Direct Sunlight":
            efficacy = 120.0
            break
        case "Overcast Sky":
            efficacy = 140.0
            break
        case "Clear Sky":
            efficacy = 125.0
            break
        case "CIE Standard Illuminant D65":
            efficacy = 130.0
            break
    }

    // Apply conversion method
    def luxValue = 0.0
    
    switch(conversionType) {
        case "Average Efficacy":
            // Simple multiplication by luminous efficacy
            luxValue = irradiance * efficacy
            break
            
        case "Solar Constant Based":
            // Based on solar constant and altitude
            def solarConstant = 1367.0
            def dayOfYear = new Date().format("D").toInteger()
            def eccentricity = 1 + 0.033 * Math.cos(2 * Math.PI * (dayOfYear - 3) / 365.25)
            def altitudeRad = altitude * Math.PI / 180
            
            // Maximum possible lux at this altitude
            def maxLux = solarConstant * eccentricity * Math.sin(altitudeRad) * 120.0
            
            // Scale by actual irradiance ratio
            luxValue = (irradiance / (solarConstant * eccentricity * Math.sin(altitudeRad))) * maxLux
            break
            
        case "Photopic Luminous Efficacy":
            // More complex calculation considering photopic vision
            // Using simplified formula that varies with altitude
            def altitudeFactor = 0.8 + (altitude / 90.0) * 0.4
            luxValue = irradiance * efficacy * altitudeFactor
            break
    }

    // FIX FOR LINE 351: Convert both values to the same type before Math.max()
    def minValue = 0.0  // Use double instead of integer
    def luxDouble = luxValue.toDouble()  // Ensure luxValue is a double
    
    // Ensure reasonable values
    luxDouble = Math.max(minValue, luxDouble)
    
    // Round to nearest integer for lux value
    return Math.round(luxDouble)
}

// Additional helper method to ensure numeric type consistency
def ensureDouble(value) {
    if (value instanceof BigDecimal) {
        return value.toDouble()
    } else if (value instanceof Integer) {
        return value.toDouble()
    } else if (value instanceof Float) {
        return value.toDouble()
    } else if (value instanceof String && value.isNumber()) {
        return value.toDouble()
    } else {
        return value
    }
}