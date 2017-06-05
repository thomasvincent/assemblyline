#!/usr/bin/groovy
import io.assemblyline.AssemblyLineCommands
def call(Map parameters = [:], body) {
    def flow = new AssemblyLineCommands()

    def defaultLabel = buildId('clients')
    def label = parameters.get('label', defaultLabel)

    def clientsImage = parameters.get('clientsImage', 'assemblyline/builder-clients:0.9')
    def mavenImage = parameters.get('mavenImage', 'assemblyline/maven-builder:2.2.297')
    def inheritFrom = parameters.get('inheritFrom', 'base')
    def jnlpImage = (flow.isOpenShift()) ? 'assemblyline/jenkins-slave-base-centos7:0.0.1' : 'jenkinsci/jnlp-slave:2.62'

    def cloud = flow.getCloudConfig()

    podTemplate(cloud: cloud, label: label, serviceAccount: 'jenkins', inheritFrom: "${inheritFrom}",
            containers: [
                    //[name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}',  workingDir: '/home/jenkins/'],
                    [name   : 'clients', image: "${clientsImage}", command: '/bin/sh -c', args: 'cat', ttyEnabled: true,  workingDir: '/home/jenkins/',
                     envVars: [[key: 'TERM', value: 'dumb']]],
                    [name   : 'maven', image: "${mavenImage}", command: '/bin/sh -c', args: 'cat', ttyEnabled: true,  workingDir: '/home/jenkins/',
                     envVars: [
                             [key: 'MAVEN_OPTS', value: '-Duser.home=/root/']]]
            ],
            volumes: [
                    secretVolume(secretName: 'jenkins-maven-settings', mountPath: '/root/.m2'),
                    persistentVolumeClaim(claimName: 'jenkins-mvn-local-repo', mountPath: '/root/.mvnrepository'),
                    secretVolume(secretName: 'gke-service-account', mountPath: '/root/home/.gke')
            ]) {
        body()
    }
}
