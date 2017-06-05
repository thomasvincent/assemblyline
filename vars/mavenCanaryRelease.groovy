#!/usr/bin/groovy

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def flow = new io.assemblyline.AssemblyLineCommands()
    def utils = new io.assemblyline.Utils()

    def skipTests = config.skipTests ?: false

    def profile
    if (flow.isOpenShift()) {
        profile = '-P openshift'
    } else {
        profile = '-P kubernetes'
    }

    sh "git checkout -b ${env.JOB_NAME}-${config.version}"
    sh "mvn org.codehaus.mojo:versions-maven-plugin:2.2:set -U -DnewVersion=${config.version}"
    sh "mvn clean -e -U deploy -Dmaven.test.skip=${skipTests} ${profile}"


    junitResults(body);

    def buildName = ""
    try {
        buildName = utils.getValidOpenShiftBuildName()
    } catch (err) {
        echo "Failed to find buildName due to: ${err}"
    }

    if (buildName != null && !buildName.isEmpty()) {
        def buildUrl = "${env.BUILD_URL}"
        if (!buildUrl.isEmpty()) {
            utils.addAnnotationToBuild('assemblyline.io/jenkins.testReportUrl', "${buildUrl}testReport")
        }
        def changeUrl = env.CHANGE_URL
        if (changeUrl != null && !changeUrl.isEmpty()) {
            utils.addAnnotationToBuild('assemblyline.io/jenkins.changeUrl', changeUrl)
        }

        if (flow.hasService("bayesian-link")) {
            try {
                sh 'mvn io.github.stackinfo:stackinfo-maven-plugin:0.2:prepare'
                def response = bayesianAnalysis url: 'https://bayesian-link'
                if (response.success) {
                    utils.addAnnotationToBuild('assemblyline.io/bayesian.analysisUrl', response.getAnalysisUrl())
                } else {
                    error "Bayesian analysis failed ${response}"
                }
            } catch (err) {
                echo "Unable to run Bayesian analysis: ${err}"
            }
        }
    }

    //try sonarQube
    sonarQubeScanner(body);


    def s2iMode = flow.isOpenShiftS2I()
    echo "s2i mode: ${s2iMode}"

    if (!s2iMode){
        if (flow.isSingleNode()){
            echo 'Running on a single node, skipping docker push as not needed'
            def m = readMavenPom file: 'pom.xml'
            def groupId = m.groupId.split( '\\.' )
            def user = groupId[groupId.size()-1].trim()
            def artifactId = m.artifactId
            sh "docker tag ${user}/${artifactId}:${config.version} ${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/${user}/${artifactId}:${config.version}"

        }else{
            retry(3){
                sh "mvn assemblyline:push -Ddocker.push.registry=${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}"
            }
        }
    }

    if (flow.hasService("content-repository")) {
      try {
        //sh 'mvn site site:deploy'
        echo 'mvn site disabled'
      } catch (err) {
        // lets carry on as maven site isn't critical
        echo 'unable to generate maven site'
      }
    } else {
      echo 'no content-repository service so not deploying the maven site report'
    }
  }
