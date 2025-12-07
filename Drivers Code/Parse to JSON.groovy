/*
	Json Parser

	Copyright 2025 -> chrisbvt

*/

import groovy.json.JsonOutput

metadata {
	definition (
			name: "Parse to Json",
			namespace: "chrisb",
			author: "Chris B"
	) {
		capability "Actuator"

        attribute "jsonString", "STRING"
        attribute "fileString", "STRING"
        attribute "customHeader", "STRING"
        attribute "delimiter", "STRING"

        command "parseFile", ["STRING"]
        command "setCustomHeader", ["STRING"]
        command "setDelimiter", ["STRING"]
}

	preferences {
        input( name: "customHeader", type:"bool", title: "Use the Custom Header (no header at top of file for keys). You must set a custom header using the command page",defaultValue: false)
        input( name: "jsonList", type:"bool", title: "Parse each line into a json list with numbered keys",defaultValue: false)
		input( name: "logEnable", type:"bool", title: "Enable debug logging",defaultValue: false)
	}
}


def installed() {
	log.warn "installed..." 
    sendEvent(name: "delimiter", value: ",")
	updated()
}

def updated() {
	log.info "updated..."
	log.warn "debug logging is: ${logEnable == true}"
	if (logEnable) runIn(1800,logsOff)

	initialize()
}

def initialize() {

}

def parse(String description) { noCommands("parse") }

def setCustomHeader(header) {
    sendEvent(name: "customHeader", value: header)
}

def setDelimiter(delim) {
    sendEvent(name: "delimiter", value: delim)
}

def parseFile(fileName) {

    def fileContent
    def stringContent
    try {
        fileContent = downloadHubFile(fileName)
        stringContent = new String(fileContent, "UTF-8") 
        sendEvent(name:"fileString", value: stringContent)
        logDebug(stringContent)
    } catch (e) {
        log.error("Error reading file from File Manager: ${e.message}")
    }

    def lines = stringContent.readLines()   
    def numLines = lines.size()
    logDebug("number of lines is ${numLines}")
    def delimiter = device.currentValue("delimiter")
    def headerText = device.currentValue("customHeader")
    
    if (settings?.jsonList) {
        ct = 0
        String[] headers = (0..numLines).collect { it.toString() } as String[]
        def dataRows = lines[0..-1].collect { line ->          
            def rowMap = [:]
            def key = headers[ct]
            rowMap[key] = line
            ct++
            rowMap
        }
        def jsonString = JsonOutput.toJson(dataRows)
        sendJsonString(jsonString)
    } else if (!settings?.customHeader) {
        // Process the data rows to row maps       
            def headers = lines[0].split(delimiter)
            def dataRows = lines[1..-1].collect { line ->
                def values = line.split(delimiter)
                def rowMap = [:]
                headers.eachWithIndex { header, index ->
                    rowMap[header] = values[index]
                }
                rowMap
            }
            def jsonString = JsonOutput.toJson(dataRows)
            sendJsonString(jsonString)        
        } else {
            def headers = headerText.split(delimiter)        
            def dataRows = lines[0..-1].collect { line ->
                def values = line.split(delimiter)
                def rowMap = [:]
                headers.eachWithIndex { header, index ->
                    rowMap[header] = values[index]
                }
                rowMap
            }
            def jsonString = JsonOutput.toJson(dataRows)
            sendJsonString(jsonString)
        }

    
}

def sendJsonString(jsonString) {
    def quoteSpace = '" '
    def spaceQuote = ' "'
    def quote = '"'
    jsonString = jsonString.replace(quoteSpace, quote)
    jsonString = jsonString.replace(spaceQuote, quote)
    sendEvent(name: "jsonString", value: jsonString)  
}

private logDebug(msg) {
	if (settings?.logEnable) log.debug "${msg}"
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable",[value:"false",type:"bool"])
}