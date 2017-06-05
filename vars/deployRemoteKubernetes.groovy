#!/usr/bin/groovy
def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  container(name: 'clients') {

    sh 'kubectl config set-credentials kube --username=\$(cat /root/.kc/user) --password=\$(cat /root/.kc/password)'
    sh "kubectl config set-cluster kube --insecure-skip-tls-verify=true --server=${config.url}"
    sh "kubectl config set-context kube --user=kube --namespace=${config.defaultNamespace} --cluster=kube"
    sh 'kubectl config use-context kube'

    try{
      sh 'kubectl delete namespace assemblyline-test'
      waitUntil{
        // wait until the project has been deleted
        try{
          sh "kubectl get namespace | cut -f 1 -d ' ' | grep assemblyline-test"
          echo 'kubectl namespace assemblyline-test still exists, waiting until deleted'
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
    sh 'kubectl create namespace assemblyline-test'
    sh "kubectl config set-context test --user=kube --namespace=assemblyline-test --cluster=kube"
    sh 'kubectl config use-context test'
    sh "goassemblyline deploy -y --docker-registry ${config.stagingDockerRegistry} --api-server ${config.url} --maven-repo https://oss.sonatype.org/content/repositories/staging/"

  }
}
