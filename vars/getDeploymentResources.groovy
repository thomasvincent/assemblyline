#!/usr/bin/groovy
import io.assemblyline.Utils
import io.assemblyline.AssemblyLineCommands

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def flow = new AssemblyLineCommands()
    def utils = new Utils()

    def expose = config.exposeApp ?: 'true'
    def requestCPU = config.resourceRequestCPU ?: '0'
    def requestMemory = config.resourceRequestMemory ?: '0'
    def limitCPU = config.resourceLimitMemory ?: '0'
    def limitMemory = config.resourceLimitMemory ?: '0'
    def yaml

    def isSha = ''
    if (flow.isOpenShift()){
        isSha = utils.getImageStreamSha(env.JOB_NAME)
    }

    def assemblylineRegistry = ''
    if (env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST){
        assemblylineRegistry = env.FABRIC8_DOCKER_REGISTRY_SERVICE_HOST+':'+env.FABRIC8_DOCKER_REGISTRY_SERVICE_PORT+'/'
    }

    def sha
    def list = """
---
apiVersion: v1
kind: List
items:
"""

def service = """
- apiVersion: v1
  kind: Service
  metadata:
    annotations:
      assemblyline.io/iconUrl: ${config.icon}
    labels:
      provider: assemblyline
      project: ${env.JOB_NAME}
      expose: '${expose}'
      version: ${config.version}
      group: quickstart
    name: ${env.JOB_NAME}
  spec:
    ports:
    - port: 80
      protocol: TCP
      targetPort: ${config.port}
    selector:
      project: ${env.JOB_NAME}
      provider: assemblyline
      group: quickstart
"""

def deployment = """
- apiVersion: extensions/v1beta1
  kind: Deployment
  metadata:
    annotations:
      assemblyline.io/iconUrl: ${config.icon}
    labels:
      provider: assemblyline
      project: ${env.JOB_NAME}
      version: ${config.version}
      group: quickstart
    name: ${env.JOB_NAME}
  spec:
    replicas: 1
    selector:
      matchLabels:
        provider: assemblyline
        project: ${env.JOB_NAME}
        group: quickstart
    template:
      metadata:
        labels:
          provider: assemblyline
          project: ${env.JOB_NAME}
          version: ${config.version}
          group: quickstart
      spec:
        containers:
        - env:
          - name: KUBERNETES_NAMESPACE
            valueFrom:
              fieldRef:
                fieldPath: metadata.namespace
          image: ${assemblylineRegistry}${env.KUBERNETES_NAMESPACE}/${env.JOB_NAME}:${config.version}
          imagePullPolicy: IfNotPresent
          name: ${env.JOB_NAME}
          ports:
          - containerPort: ${config.port}
            name: http
          resources:
            limits:
              cpu: ${requestCPU}
              memory: ${requestMemory}
            requests:
              cpu: ${limitCPU}
              memory: ${limitMemory}
          readinessProbe:
            httpGet:
              path: "/"
              port: ${config.port}
            initialDelaySeconds: 1
            timeoutSeconds: 5
            failureThreshold: 5
          livenessProbe:
            httpGet:
              path: "/"
              port: ${config.port}
            initialDelaySeconds: 180
            timeoutSeconds: 5
            failureThreshold: 5
        terminationGracePeriodSeconds: 2
"""

def deploymentConfig = """
- apiVersion: v1
  kind: DeploymentConfig
  metadata:
    annotations:
      assemblyline.io/iconUrl: ${config.icon}
    labels:
      provider: assemblyline
      project: ${env.JOB_NAME}
      version: ${config.version}
      group: quickstart
    name: ${env.JOB_NAME}
  spec:
    replicas: 1
    selector:
      provider: assemblyline
      project: ${env.JOB_NAME}
      group: quickstart
    template:
      metadata:
        labels:
          provider: assemblyline
          project: ${env.JOB_NAME}
          version: ${config.version}
          group: quickstart
      spec:
        containers:
        - env:
          - name: KUBERNETES_NAMESPACE
            valueFrom:
              fieldRef:
                fieldPath: metadata.namespace
          image: ${env.JOB_NAME}:${config.version}
          imagePullPolicy: IfNotPresent
          name: ${env.JOB_NAME}
          ports:
          - containerPort: ${config.port}
            name: http
          resources:
            limits:
              cpu: ${requestCPU}
              memory: ${requestMemory}
            requests:
              cpu: ${limitCPU}
              memory: ${limitMemory}
          readinessProbe:
            httpGet:
              path: "/"
              port: ${config.port}
            initialDelaySeconds: 1
            timeoutSeconds: 5
            failureThreshold: 5
          livenessProbe:
            httpGet:
              path: "/"
              port: ${config.port}
            initialDelaySeconds: 180
            timeoutSeconds: 5
            failureThreshold: 5
        terminationGracePeriodSeconds: 2
    triggers:
    - type: ConfigChange
    - imageChangeParams:
        automatic: true
        containerNames:
        - ${env.JOB_NAME}
        from:
          kind: ImageStreamTag
          name: ${env.JOB_NAME}:${config.version}
      type: ImageChange
"""

    def is = """
- apiVersion: v1
  kind: ImageStream
  metadata:
    name: ${env.JOB_NAME}
  spec:
    tags:
    - from:
        kind: ImageStreamImage
        name: ${env.JOB_NAME}@${isSha}
        namespace: ${utils.getNamespace()}
      name: ${config.version}
"""

  if (flow.isOpenShift()){
    yaml = list + service + is + deploymentConfig
  } else {
    yaml = list + service + deployment
  }

  echo 'using resources:\n' + yaml
  return yaml

  }
