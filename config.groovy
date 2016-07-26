#!/usr/bin/env groovy

//groovy config-client.groovy https://config-service-admin-devstack-platform-dev.appls.boae.paas.gsnetcloud.corp:443 admin:admin sample master default app.disclaimer="Yeah!"

def cli = new CliBuilder(usage: 'config-client.groovy')

def server = args[0]
def credentials = args[1]
def app = args[2]
def label = args[3]
def profile = args[4]
def toChange = args[5].split('=')[0]
def newValue = args[5].split('=')[1]
def forceNumeric = args['--forceNumeric']

def url = "${server}/admin/config/${app}/document/${profile}/${label}"

def authString = credentials.getBytes().encodeBase64().toString()

def getResponse = new URL(url).getText(requestProperties: [Authorization: "Basic " + authString])

def jsonSlurper = new groovy.json.JsonSlurper()
def response = jsonSlurper.parseText(getResponse)

println "Properties before: ${response.properties}"

def postObject = [
  "label": "${label}",
  "profile": "${profile}",
]

postObject.properties = response.properties
if (forceNumeric) {
   println "number"
   newValue = new Integer(newValue)
}
postObject.properties.put(toChange, newValue)

def postUrl =  "${server}/admin/config/${app}/document"

HttpURLConnection connection = new URL(postUrl).openConnection()
connection.setDoOutput(true)
connection.setRequestMethod("PUT")
connection.setRequestProperty("Authorization",  "Basic " + authString)
connection.setRequestProperty("Content-Type", "application/json")
connection.setRequestProperty("Accept", "application/json")
connection.setDoInput(true)

OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream())
wr.write(groovy.json.JsonOutput.toJson(postObject))
wr.flush()

postResponse = connection.inputStream.withReader { Reader reader -> reader.text }
println postResponse
if (connection.responseCode != 204) {
   println "Error puttting data"
   return
}

println "Data changed"