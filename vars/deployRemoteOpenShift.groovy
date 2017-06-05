#!/usr/bin/groovy
def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def flow = new io.assemblyline.AssemblyLineCommands()

  container(name: 'clients') {
    sh "oc login ${config.url} --token=\$(cat /root/.oc/token) --insecure-skip-tls-verify=true"
    try{
      sh 'oc delete project assemblyline-test'
      waitUntil{
        // wait until the project has been deleted
        try{
          sh "oc get projects | cut -f 1 -d ' ' | grep assemblyline-test"
          echo 'openshift project assemblyline-test still exists, waiting until deleted'
        } catch (err) {
          echo "${err}"
          // project doesnt exist anymore so continue
          return true
        }
        return false
      }
    } catch (err) {
      // dont need to worry if there's no existing test environment to delete
    }
    sh 'oc new-project assemblyline-test'
    sh "goassemblyline deploy -y --docker-registry ${config.stagingDockerRegistry} --api-server ${config.url} --domain ${config.domain} --maven-repo https://oss.sonatype.org/content/repositories/staging/"

  }
}
