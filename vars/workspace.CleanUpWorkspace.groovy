public def void call () {
    if (env.THIS_WORKSPACE) {
        DeleteWorkspace(env.THIS_WORKSPACE) //Deleting the workspace
        dir ("${WORKSPACE}/../locks") {
            sh "[ -f \"${THIS_WORKSPACE}\" ] && rm ${THIS_WORKSPACE}" //Cleaning the lock 
        }
    }
}

private void DeleteWorkspace(pWorkspaceName){
    try{
        //Sometimes the workspace is still in use even when aborting the build.
        // this might happen when P4 is synching during build abort and its process(es) are still working
        // thereof we retry deleting the workspace a few times with sleep in-between
        def retryAttempt = 0
        retry(10) {
            if (retryAttempt > 0) {sleep(60)}
            retryAttempt = retryAttempt + 1
            sh "rm -rf ${WORKSPACE}/${pWorkspaceName}/"  
        }
    } catch (e) {
        currentBuild.result = "FAILURE"
        println("Cannot delete ${WORKSPACE}/${pWorkspaceName}/")
    }
}
