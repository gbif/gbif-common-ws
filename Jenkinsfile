@Library('gbif-common-jenkins-pipelines') _

pipeline {
  agent any
  tools {
    maven 'Maven 3.9.9'
    jdk 'OpenJDK17'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
    skipStagesAfterUnstable()
    timestamps()
    disableConcurrentBuilds()
  }
  triggers {
    snapshotDependencies()
  }
  parameters {
    separator(name: "release_separator", sectionHeader: "Release Main Project Parameters")
    booleanParam(name: 'RELEASE', defaultValue: false, description: 'Do a Maven release')
    string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release version (optional)')
    string(name: 'DEVELOPMENT_VERSION', defaultValue: '', description: 'Development version (optional)')
    booleanParam(name: 'DRY_RUN_RELEASE', defaultValue: false, description: 'Dry Run Maven release')
  }
  environment {
    JETTY_PORT = utils.getPort()
  }
  stages {

    stage('Setup') {
      when {
        allOf {
          not { expression { params.RELEASE } };
          not { branch 'dev' };
          not { branch 'master' };
        }
      }
      steps {
        script {
          // Branch-qualified version for feature branches: workspace only, never committed.
          def suffix = env.BRANCH_NAME.replaceAll('[^a-zA-Z0-9.-]', '-').toUpperCase()
          sh """
            BASE_VERSION=\$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | sed 's/-SNAPSHOT//')
            mvn versions:set -DnewVersion=\${BASE_VERSION}-${suffix}-SNAPSHOT -DgenerateBackupPoms=false -DprocessAllModules=true
          """
        }
      }
    }

    stage('Maven build') {
       when {
        allOf {
          not { expression { params.RELEASE } };
        }
      }
      steps {
        withMaven(
            globalMavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
            mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540',
            traceability: true) {
                configFileProvider([
                    configFile(fileId: 'org.jenkinsci.plugins.configfiles.custom.CustomConfig1389220396351', variable: 'APPKEYS_TESTFILE')
                    ]) {
                    sh '''
                       mvn -B -U clean package dependency:analyze deploy -Darguments="-Dappkeys.testfile=$APPKEYS_TESTFILE"
                      '''
                  }
                }
      }
    }

    stage('Maven release') {
      when {
          allOf {
              expression { params.RELEASE };
              branch 'master';
          }
      }
      environment {
          RELEASE_ARGS = utils.createReleaseArgs(params.RELEASE_VERSION, params.DEVELOPMENT_VERSION, params.DRY_RUN_RELEASE)
      }
      steps {
          withMaven(
            globalMavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
            mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540',
            traceability: true) {
                configFileProvider([
                    configFile(fileId: 'org.jenkinsci.plugins.configfiles.custom.CustomConfig1389220396351', variable: 'APPKEYS_TESTFILE')
                    ]) {
                  git 'https://github.com/gbif/gbif-common-ws.git'
                  sh '''
                    mvn -B -Dresume=false release:prepare release:perform -Darguments="-Dappkeys.testfile=$APPKEYS_TESTFILE" $RELEASE_ARGS
                    '''
                }
            }
      }
    }
  }
    post {
      success {
        echo 'Pipeline executed successfully!'
      }
      failure {
        echo 'Pipeline execution failed!'
    }
  }
}
