#!/usr/bin/groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (!config.pullRequestProject){
        error 'No project details provided to get Pull Request comments'
    }

    if (!env.CHANGE_ID){
        error 'No Change ID found, is this a Pull Request?'
    }

    def runtimeDir = pwd()
    def utils = new io.assemblyline.Utils()
    def downstreamassemblylineUIOrg = utils.getDownstreamProjectOverrides(config.pullRequestProject, env.CHANGE_ID, 'assemblyline-ui') ?: 'assemblylineio'
    echo "using ${downstreamassemblylineUIOrg}/assemblyline-ui"

    sh "git clone https://github.com/${downstreamassemblylineUIOrg}/assemblyline-ui"
    sh 'cd assemblyline-ui && npm install'
    sh "cd assemblyline-ui && npm install --save  ${runtimeDir}/dist"
    sh '''
        export FABRIC8_WIT_API_URL="https://api.openshift.io/api/"
        export FABRIC8_RECOMMENDER_API_URL="https://recommender.api.openshift.io"
        export FABRIC8_FORGE_API_URL="https://forge.api.openshift.io"
        export FABRIC8_SSO_API_URL="https://sso.openshift.io/"
        
        export OPENSHIFT_CONSOLE_URL="https://console.starter-us-east-2.openshift.com/console/"
        export WS_K8S_API_SERVER="api.starter-us-east-2.openshift.com:443"
        
        export PROXIED_K8S_API_SERVER="${WS_K8S_API_SERVER}"
        export OAUTH_ISSUER="https://${WS_K8S_API_SERVER}"
        export PROXY_PASS_URL="https://${WS_K8S_API_SERVER}"
        export OAUTH_AUTHORIZE_URI="https://${WS_K8S_API_SERVER}/oauth/authorize"
        export AUTH_LOGOUT_URI="https://${WS_K8S_API_SERVER}/connect/endsession?id_token={{id_token}}"

        cd assemblyline-ui && npm run build:prod
        '''
// TODO lets use a comment on the PR to denote whether or not to use prod or pre-prod?
/*
    sh '''
        export FABRIC8_WIT_API_URL="https://api.prod-preview.openshift.io/api/"
        export FABRIC8_RECOMMENDER_API_URL="https://api-bayesian.dev.rdu2c.assemblyline.io/api/v1/"
        export FABRIC8_FORGE_API_URL="https://forge.api.prod-preview.openshift.io"
        export FABRIC8_SSO_API_URL="https://sso.prod-preview.openshift.io/"

        export OPENSHIFT_CONSOLE_URL="https://console.free-int.openshift.com/console/"
        export WS_K8S_API_SERVER="api.free-int.openshift.com:443"

        cd assemblyline-ui && npm run build:prod
        '''
*/
    def shortCommitSha = getNewVersion {}
    def tempVersion= 'SNAPSHOT.' + shortCommitSha + env.BUILD_NUMBER
    return tempVersion
}
