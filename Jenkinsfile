// =============================================================================
// Pipeline de la plateforme — Jenkins sous WINDOWS
//   build + tests + SonarQube → images poussées sur ghcr.io → déploiement sur k3s (Ubuntu)
//
// Les étapes « bash » (images, déploiement, test de fumée) passent par Git Bash : les mêmes
// scripts fonctionnent sous Linux le jour où Jenkins y est déplacé (remplacer bat par sh).
//
// Préparation (une seule fois)
// ---------------------------
// Sur la machine Jenkins (Windows) :
//   - JDK 21, Maven, Docker Desktop (conteneurs Linux), Git pour Windows, kubectl, Python 3
//     dans le PATH du service Jenkins ; plugin SonarQube (serveur nommé « SonarQube »).
//   - Si Git Bash n'est pas dans C:\Program Files\Git, adapter GIT_BASH ci-dessous.
//   - Le service Jenkins doit tourner sous TON compte Windows (services.msc → Jenkins → Connexion),
//     pas sous « Système local » : sinon il n'a accès ni à Docker Desktop ni à tes outils.
// Credentials Jenkins :
//   - ghcr              (Username with password) : ton login GitHub + un token « classic »
//                        avec write:packages et read:packages
//   - k3s-kubeconfig    (Secret file) : /etc/rancher/k3s/k3s.yaml de l'Ubuntu, en remplaçant
//                        127.0.0.1 par l'adresse de l'Ubuntu si ce n'est pas une WSL en mode mirrored
//   - mtls-p12-password (Secret text) : mot de passe des keystores mTLS, identique d'un build à l'autre
// Sur l'Ubuntu : k3s installé et son port 6443 joignable depuis Windows.
// =============================================================================
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()      // un seul déploiement à la fois sur le cluster
        timeout(time: 90, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    parameters {
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Analyse SonarQube des modules Java')
        booleanParam(name: 'PUSH',      defaultValue: true, description: 'Construire et pousser les images sur ghcr.io')
        booleanParam(name: 'DEPLOY',    defaultValue: true, description: 'Déployer sur le k3s (nécessite PUSH ou un tag déjà poussé)')
        string(name: 'TARGET_HOST', defaultValue: 'localhost', description: 'Adresse de l\'Ubuntu qui expose les NodePorts (test de fumée, Keycloak)')
        string(name: 'PUBLIC_URL', defaultValue: 'https://lorrconnect.com', description: 'Adresse publique du site (tunnel Cloudflare). Vide = plateforme uniquement locale')
        string(name: 'KEYCLOAK_PUBLIC_URL', defaultValue: 'https://keycloak.lorrconnect.com', description: 'Adresse publique de Keycloak (vide = même adresse que PUBLIC_URL)')
    }

    environment {
        GIT_BASH     = 'C:\\Program Files\\Git\\bin\\bash.exe'
        MVN          = 'mvn --batch-mode --no-transfer-progress'
        REGISTRY     = 'ghcr.io/courtvi'
        MTLS_OUT_DIR = "${env.JENKINS_HOME}\\mtls"     // certificats conservés hors du workspace
        TARGET_HOST  = "${params.TARGET_HOST}"
        // Mêmes adresses que le déploiement depuis IntelliJ (script/.deploy.env) : sinon un build
        // Jenkins repasserait la plateforme en mode localhost et couperait lorrconnect.com
        PUBLIC_URL          = "${params.PUBLIC_URL}"
        KEYCLOAK_PUBLIC_URL = "${params.KEYCLOAK_PUBLIC_URL}"
    }

    stages {
        stage('Prérequis') {
            steps {
                bat '''
                    @echo off
                    java -version || exit /b 1
                    call %MVN% -v || exit /b 1
                    docker version --format "Docker {{.Server.Version}}" || exit /b 1
                    kubectl version --client || exit /b 1
                    "%GIT_BASH%" --version || exit /b 1
                '''
                script {
                    // Tag des images = commit court : chaque build déploie exactement ce qui a été testé
                    env.IMAGE_TAG = bat(returnStdout: true, script: '@git rev-parse --short HEAD').trim()
                    echo "Tag des images : ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build et tests') {
            steps {
                bat 'call %MVN% clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube') {
            when { expression { params.RUN_SONAR } }
            steps {
                withSonarQubeEnv('SonarQube') {
                    // Un projet Sonar par service, comme pour commande-service
                    bat '''
                        @echo off
                        for %%m in (eureka-server api-gateway produit-service client-service commande-service devis-service) do (
                            call %MVN% -pl %%m sonar:sonar -Dsonar.projectKey=%%m -Dsonar.projectName=%%m || exit /b 1
                        )
                    '''
                }
            }
        }

        stage('Images → ghcr.io') {
            when { expression { params.PUSH } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'ghcr', usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_TOKEN')]) {
                    bat '"%GIT_BASH%" script/build-push.sh'
                }
            }
        }

        stage('Déploiement k3s') {
            when { expression { params.DEPLOY } }
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'ghcr', usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_TOKEN'),
                    file(credentialsId: 'k3s-kubeconfig', variable: 'KUBECONFIG'),
                    string(credentialsId: 'mtls-p12-password', variable: 'P12_PASSWORD')
                ]) {
                    bat '"%GIT_BASH%" script/deploy-registry.sh'
                }
            }
        }

        stage('Test de fumée') {
            when { expression { params.DEPLOY } }
            steps {
                bat '"%GIT_BASH%" script/smoke-test.sh'
            }
        }
    }

    post {
        success { echo "Pipeline réussi ✅ — images ${env.REGISTRY}/*:${env.IMAGE_TAG}" }
        failure { echo 'Pipeline en échec ❌' }
    }
}
