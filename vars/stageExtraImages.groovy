#!/usr/bin/groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    container(name: 'docker') {
      for(int i = 0; i < config.images.size(); i++){
        image = config.images[i]
        retry (3){
          sh "docker pull docker.io/assemblyline/${image}:latest"
          sh "docker tag docker.io/assemblyline/${image}:latest ${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/assemblyline/${image}:${config.tag}"
          sh "docker tag docker.io/assemblyline/${image}:latest docker.io/assemblyline/${image}:${config.tag}"
          sh "docker push ${env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST}:${env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT}/assemblyline/${image}:${config.tag}"
        }
      }
    }
  }
