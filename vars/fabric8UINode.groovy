#!/usr/bin/groovy
def call(Map parameters = [:], body) {

    def defaultLabel = buildId('ui')
    def label = parameters.get('label', defaultLabel)

    assemblylineUITemplate(parameters) {
        node(label) {
            body()
        }
    }
}
