public def void call (String pP4User, HashMap pUserParameters) {
    dir ("${THIS_WORKSPACE}") {
        script {
            if (pUserParameters.Settings.CleanWorkspace) {
                print "Clean workspace and sync"
                p4sync charset: 'none', 
                credential: "${pP4User}", 
                format: "${THIS_WORKSPACE}",
                populate: forceClean( 
                    force: true, 
                    have: true, 
                    modtime: false, 
                    parallel: [enable: true, minbytes: '1024', minfiles: '1', threads: '4'],
                    pin: pUserParameters.Settings.Label, 
                    quiet: true, 
                    revert: false
                ), 
                source: depotSource(GenerateSourcesList(pUserParameters.Branches))
            } else {
                print "Sync only"
                p4sync charset: 'none', 
                credential: "${pP4User}",
                format: "${THIS_WORKSPACE}",
                populate: syncOnly( 
                    force: false, 
                    have: true, 
                    modtime: false, 
                    parallel: [enable: true, minbytes: '1024', minfiles: '1', threads: '4'],
                    pin: pUserParameters.Settings.Label, 
                    quiet: true, 
                    revert: false
                ), 
                source: depotSource(GenerateSourcesList(pUserParameters.Branches))
            }

            p4unshelve credential: "${pP4User}", 
            ignoreEmpty: true, 
            resolve: 'am', 
            shelf: pUserParameters.Settings.Shelveset, 
            tidy: false
        }
    }
}  

private def String GenerateSourcesList (pBranches) {
    //This function generates p4 source list based on the user parameters
    def sourcesList=""
    pBranches.each { URL -> 
        sourcesList += "${URL}\n"
    }
    return sourcesList;
}