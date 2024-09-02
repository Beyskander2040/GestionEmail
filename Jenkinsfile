 pipeline {
      agent any

      tools {
          maven 'M2_HOME'
      }

         stages {
              stage('Checkout Git repository') {
                  steps {
                          echo 'Pulling'
                      git branch: 'skander', url:'https://github.com/Beyskander2040/GestionEmail.git'
                  }
              }
               stage('Maven Install') {
            steps {
                dir('Mail')
                sh 'mvn install'
            }
        }
         stage('Build package') {
            steps {
                dir('Mail')
                sh 'mvn package'
            }
        }
    
}
 }