#!/usr/bin/groovy
import io.assemblyline.AssemblyLineCommands
def call(Map parameters = [:], body) {
    def flow = new AssemblyLineCommands()

    def defaultLabel = buildId('nodejs')
    def label = parameters.get('label', defaultLabel)

    def nodejsImage = parameters.get('nodejsImage', 'assemblyline/nodejs-builder:0.0.3')
    def inheritFrom = parameters.get('inheritFrom', 'base')
    def jnlpImage = (flow.isOpenShift()) ? 'assemblyline/jenkins-slave-base-centos7:0.0.1' : 'jenkinsci/jnlp-slave:2.62'

    def cloud = flow.getCloudConfig()

        podTemplate(cloud: cloud, label: label, inheritFrom: "${inheritFrom}",
                containers: [
                        //[name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}',  workingDir: '/home/jenkins/'],
                        [name: 'nodejs', image: "${nodejsImage}", command: '/bin/sh -c', args: 'cat', ttyEnabled: true,  workingDir: '/home/jenkins/']]) {
            body()
        }


}
