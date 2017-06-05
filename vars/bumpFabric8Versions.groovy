#!/usr/bin/groovy
def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def project = 'assemblyline'
  stage "bump ${project} versions"

  withEnv(["PATH+MAVEN=${tool 'maven-3.3.1'}/bin"]) {

    def flow = new io.assemblyline.AssemblyLineCommands()
    flow.setupWorkspace (project)

    def uid = UUID.randomUUID().toString()
    sh "git checkout -b versionUpdate${uid}"

    def updated = false
    try {
      // bump assemblyline release dependency versions
      def kubernetesModelVersion = flow.getReleaseVersion 'io/assemblyline/kubernetes-model'
      flow.searchAndReplaceMavenVersionProperty('<kubernetes-model.version>', kubernetesModelVersion)
      updated = true
    } catch (err) {
      echo "Already on the latest versions of kubernetes-model"
    }

    try {
      def kubernetesClientVersion = flow.getReleaseVersion 'io/assemblyline/kubernetes-client'
      flow.searchAndReplaceMavenVersionProperty('<kubernetes-client.version>', kubernetesClientVersion)
      updated = true
    } catch (err) {
      echo "Already on the latest versions of kubernetes-client"
    }
    // only make a pull request if we've updated a version
    if (updated) {
      sh "git push origin versionUpdate${uid}"
      return flow.createPullRequest("[CD] Update release dependencies","${project}","versionUpdate${uid}")
    } else {
      message = "assemblyline already on the latest release versions"
      hubot room: 'release', message: message
      return
    }
  }
}
