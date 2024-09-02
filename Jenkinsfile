pipeline {
    agent any

    tools {
        maven 'M2_HOME'
    }

    stages {
        stage('Checkout Git repository') {
            steps {
                cleanWs() // Clean the workspace to ensure it's fresh
                echo 'Pulling code from Git repository'
                git branch: 'skander', url: 'https://github.com/Beyskander2040/GestionEmail.git'
            }
        }
        stage('Maven Install') {
            steps {
                // Run Maven install specifying the path if necessary
                sh 'mvn install'
            }
        }
        stage('Build package') {
            steps {
                sh 'mvn package'
            }
        }
    }
}
