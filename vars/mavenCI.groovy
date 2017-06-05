#!/usr/bin/groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def flow = new io.assemblyline.AssemblyLineCommands()
    container(name: 'maven') {

        // update any versions that we want to override
        for ( v in config.pomVersionToUpdate ) {
            flow.searchAndReplaceMavenVersionProperty(v.key, v.value)
        }

        def skipTests = config.skipTests ?: false

        def profile
        if (flow.isOpenShift()) {
            profile = '-P openshift'
        } else {
            profile = '-P kubernetes'
        }

        stage ('Build + Unit test'){
            sh "mvn clean -e -U deploy -Dmaven.test.skip=${skipTests} ${profile}"
        }
        
        def s2iMode = flow.isOpenShiftS2I()
        echo "s2i mode: ${s2iMode}"
        def m = readMavenPom file: 'pom.xml'
        def version = m.version
        
        if (!s2iMode){
            stage ('Push snapshot image to registry'){
                if (flow.isSingleNode()){
                    echo 'Running on a single node, skipping docker push as not needed'

                    def groupId = m.groupId.split( '\\.' )
                    def user = groupId[groupId.size()-1].trim()
                    def artifactId = m.artifactId
                    sh "docker tag ${user}/${artifactId}:${version} ${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/${user}/${artifactId}:${version}"

                }else{
                    retry(3){
                        sh "mvn assemblyline:push -Ddocker.push.registry=${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}"
                    }
                }
            }
        }

        stage ('Integration Testing'){
            def utils = new io.assemblyline.Utils()
            def envTest = utils.environmentNamespace('test')
            sh "mvn org.apache.maven.plugins:maven-failsafe-plugin:2.18.1:integration-test -Dassemblyline.environment=${envTest} -Dit.test=*IT -DfailIfNoTests=false org.apache.maven.plugins:maven-failsafe-plugin:2.18.1:verify"
        }
        
        return version
    }
  }
