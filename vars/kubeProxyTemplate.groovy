#!/usr/bin/groovy
import io.assemblyline.AssemblyLineCommands

def call(Map parameters = [:], body) {
    def flow = new AssemblyLineCommands()

    def defaultLabel = buildId('kubeProxy')

    def label = parameters.get('label', defaultLabel)
    def inheritFrom = parameters.get('inheritFrom', 'base')
    def jnlpImage = (flow.isOpenShift()) ? 'assemblyline/jenkins-slave-base-centos7:0.0.1' : 'jenkinsci/jnlp-slave:2.62'
    def kubectlProxyImage = parameters.get('kubeproxyImage', 'gcr.io/google_containers/kubectl:v0.18.0-350-gfb3305edcf6c1a')

    def cloud = flow.getCloudConfig()

    podTemplate(cloud: cloud, label: label, inheritFrom: "${inheritFrom}",
            containers: [
                    //[name: 'jnlp', image: "${jnlpImage}", args: '${computer.jnlpmac} ${computer.name}',  workingDir: '/home/jenkins/'],
                    [name: 'kubeproxy', image: "${kubectlProxyImage}", command: '/bin/sh -c', args: 'proxy', ttyEnabled: true,  workingDir: '/home/jenkins/']]) {
          body(
          )
      }
}
