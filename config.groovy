#!/usr/bin/env groovy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def cli = new CliBuilder(usage: 'config <get|set|del> prop=value', stopAtNonOption: false)

cli.h(longOpt: 'help', 'Show usage information')
cli.c(longOpt: 'config-server', args:1, argName: 'server', 'Config server url')
cli.u(longOpt: 'credentials', args:1, argName: 'user:pass', 'Basic auth credentials')
cli.a(longOpt: 'app', args:1, argName: 'app', 'Application name')
cli.p(longOpt: 'profile', args:1, argName: 'profile', 'Profile')
cli.l(longOpt: 'label', args:1, argName: 'label', 'Label')

cli.n('Force numeric on property set')
cli.b('Force boolean on property set')

def options = cli.parse(args)

if (args.length == 0 || options.h) {
    cli.usage()
    return
}

server = options.c
credentials = options.u
forceNumeric = options.n
forceBool = options.b
app = options.a
def label = options.l
def profile = options.p

def subCommand = options.arguments()[0]

def url = "${server}/admin/config/${app}/document/${profile}/${label}"

def authString = credentials.getBytes().encodeBase64().toString()

if (subCommand == 'get') {

    def prop = null
    if (options.arguments()[1]) {
        prop = options.arguments()[1]
    }

    response = get(url, authString, prop)

    if (prop) {
        println response.properties.get(prop)
    } else {
        response.properties.each{ k, v -> println "${k}=${v}" }
    }


} else if (subCommand == 'set' || subCommand == 'del') {

    Object response = get(url, authString)

    def postObject = [
              "label": "${label}",
              "profile": "${profile}",
    ]

    postObject.properties = response.properties

    if (subCommand == 'set') {
        def toChange = options.arguments()[1].split('=')[0]
        def newValue = options.arguments()[1].split('=')[1]

        if (forceNumeric) {
            newValue = new Integer(newValue)
        } else if (forceBool) {
            newValue = new Boolean(newValue)
        }

        postObject.properties.put(toChange, newValue)
    } else {
        def toRemove = options.arguments()[1]
        postObject.properties.remove(toRemove)
    }

    def postUrl =  "${server}/admin/config/${app}/document"

    HttpURLConnection connection = new URL(postUrl).openConnection()
    connection.setDoOutput(true)
    connection.setRequestMethod("PUT")
    connection.setRequestProperty("Authorization",  "Basic " + authString)
    connection.setRequestProperty("Content-Type", "application/json")

    OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream())
    wr.write(JsonOutput.toJson(postObject))
    wr.flush()

    if (connection.responseCode != 204) {
        println "Error putting data"
        return
    }

    println "Ok!"
}

private Object get(url, authString, prop=null) {
    def getResponse = new URL(url).getText(requestProperties: [Authorization: "Basic " + authString])

    def jsonSlurper = new JsonSlurper()
    jsonSlurper.parseText(getResponse)
}