#!/usr/bin/groovy
import io.assemblyline.AssemblyLineCommands

@Library('github.com/assemblylineio/assemblyline-pipeline-library@master')
def dummy
clientsNode {
    ws ('pipelines'){
        git 'https://github.com/assemblylineio/assemblyline-pipeline-library.git'

        def pipeline = load 'release.groovy'

        stage 'Tag'
        pipeline.tagDownstreamRepos()
    }
}