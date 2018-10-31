public def void call () {
    def String dockerHostName = readFile('../hostname').trim()
    timeout(time: 30, unit: 'MINUTES') {
        lock ("JobLock-${JOB_NAME}-${dockerHostName}") {
            println "Maximum number of parallel execution per job is: ${MAX_CI_PARALLELISM}"
            def int lastAllowedWorkspace = MAX_CI_PARALLELISM as int
            def String thisws
            def boolean wsfound=false
            while (!wsfound){
                for (i = 1; i <= lastAllowedWorkspace; i++){
                    thisws ="${JOB_NAME}_${dockerHostName}_WS${i}"
                    if (fileExists("../locks/${thisws}")) {
                        println "$thisws is in use, looking for next available workspace..."
                    }
                    else{
                        //workspace not in use
                        wsfound=true
                        println "Found available workspace: ${thisws}. Using it."
                        break
                    }
                }
                if (wsfound) {
                    // Create a File object representing the thisws 
                    sh "touch ../locks/${thisws}"
                    env.THIS_WORKSPACE = thisws
                } else {
                        echo "Couldn't find an available workspace, sleeping 1 min till next attempt"
                        sleep 60                            
                } 
            } 
        }
    }
}