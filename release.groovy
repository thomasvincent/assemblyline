#!/usr/bin/groovy
import io.assemblyline.AssemblyLineCommands

def tagDownstreamRepos() {
    def flow = new AssemblyLineCommands()
    def newVersion

    setWorkspace('assemblylineio/assemblyline-pipeline-library')
    newVersion = getJenkinsVersion()
    container(name: 'clients') {

        flow.pushTag(newVersion)

    }

    git 'https://github.com/assemblylineio/assemblyline-jenkinsfile-library.git'
    setWorkspace('assemblylineio/assemblyline-jenkinsfile-library')

    container(name: 'clients') {

        def uid = UUID.randomUUID().toString()
        sh "git checkout -b versionUpdate${uid}"

        sh "find -type f -name 'Jenkinsfile' | xargs sed -i -r 's/library@([0-9][0-9]{0,2}.[0-9][0-9]{0,2}(.[0-9][0-9]{0,2})|master)/library@v${newVersion}/g'"

        sh "git commit -a -m 'Updated Jenkinsfiles with new library version ${newVersion}'"

        flow.pushTag(newVersion)
    }

}

def getJenkinsVersion() {
    def m = readMavenPom file: 'pom.xml'
    def v = m.properties['assemblyline.devops.version']
    return v
}

def setWorkspace(String project) {
    sh "git remote set-url origin git@github.com:${project}.git"
    sh "git config user.email assemblyline-admin@googlegroups.com"
    sh "git config user.name assemblyline-release"
    sh 'chmod 600 /root/.ssh-git/ssh-key'
    sh 'chmod 600 /root/.ssh-git/ssh-key.pub'
    sh 'chmod 700 /root/.ssh-git'
}

return this
