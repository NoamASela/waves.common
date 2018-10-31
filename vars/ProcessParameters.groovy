import groovy.json.JsonSlurperClassic;
import groovy.json.JsonOutput;
import com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition;
import groovy.transform.Field;

// Global Static
@Field final String PARAMETERS_ROOT_PATH = "C:/Program Files (x86)/jenkins/parameters"; // "C:/Program Files (x86)/jenkins/parameters" | "/var/jenkins_home/params_forms";

public def HashMap call (boolean pShowSaveParameters, boolean pDefaultSaveParametersValue) {
    //This function is being called whenever a pipeline calls ProcessParameters
    // this is the behaviour when the filename is named: ProcessParameters.groovy)

    //Set this flag to true / false for automatic parameter save
    def boolean DefaultSaveParametersValue=pDefaultSaveParametersValue;

    //Set this flag to true / false to show the Save Parameters option to the user
    def boolean ShowSaveParameters=pShowSaveParameters;

    def HashMap UserParameters; //The user parameter object to return to the pipeline

    try {
        print "Saving parameters configured to: ${SaveParameters}"
        if (SaveParameters=="true") {SaveParameters=true}
    }
    catch (groovy.lang.MissingPropertyException e) { //SaveParameter object was not found in the user settings
        SaveParameters = DefaultSaveParametersValue  // using default value
        print "Couldnt find SaveParameters checkbox in user UI, using default value: ${SaveParameters}"
    }
    
    try {
        if (Form) { //Form is the object returned from the user UI. If it doesnt exist, it means automatic invocation.
            print "invoked manually"
            UserParameters = new JsonSlurperClassic().parseText(Form)
            if (SaveParameters==true && DetectParameterChange(Form)) {
                print "Writing parameters to file"
                WriteParametersToFile()
            }
            SaveParametersToJob(ShowSaveParameters,DefaultSaveParametersValue)
        }
    } catch (groovy.lang.MissingPropertyException e) { //When invoked automatically, the UserParameters object doesn't exist
                                                       // this code will catch the missing property exception
                                                       // which means automatic invocation
        print "invoked automatic"
        UserParameters = ReadParametersFromFile().startval //Updating the UserParameters from the file, since it was invoked automatically.
    }
    return UserParameters
}
private def HashMap ReadParametersFromFile() {
    // This function reads the parameters from a file and returns a json containing them.
    def String parametersFileName = GetParametersFileName()
    def File parametersFile = new File(parametersFileName)

    def String parametersText=""
    if (parametersFile.exists() && parametersFile.canRead()) {
        parametersText = parametersFile.text
    }
    else {
        println "Cannot read from file: ${parametersFileName}"
    }

    def HashMap returnedParameters = new JsonSlurperClassic().parseText(parametersText)
    return returnedParameters
}
private def Boolean WriteParametersToFile() {
    // This function writes the current user parameters to the relevant parameters.json file
    def String parametersFileName = GetParametersFileName()
    try {
            def HashMap newParameters = ReadParametersFromFile() //file parameters
            def FileWriter fileWriter = new FileWriter(parametersFileName)
            def HashMap currentParameters = new JsonSlurperClassic().parseText(Form) //user parameters

            newParameters.startval = currentParameters //adding the user values to the new parameters json file
            fileWriter.write(new JsonOutput().toJson(newParameters)); //writing the new values to the file
            fileWriter.close()
            return true
        } catch (IOException e) {
            print "Couldn't save to file: ${parametersFileName}"
            return false
        }
}
private def String GetParametersFileName() {
    // This function returns the parameters filename, or throw IOException if not found.
    def String fileSuffix = "json"
    def String parametersFileName = "${PARAMETERS_ROOT_PATH}/${JOB_NAME}.${fileSuffix}"
    def File parametersFile = new File(parametersFileName)
    if (!(parametersFile.exists() && parametersFile.canRead())) {
        throw (new IOException ("Cannot find or read from file: ${parametersFileName}"))
    }
    return parametersFileName
}
private def ExtendedChoiceParameterDefinition BuildParameters() {
    //This function creates a ExtendedChoiceParameterDefinition object with the user settings in the relevant json file.
    def String parametersJSONOut = new JsonOutput().toJson(ReadParametersFromFile())
    def String jsonGroovyScript = """
        import org.boon.Boon;
        def jsonEditorOptions = Boon.fromJson(/\${parametersJSONOut}/);
        return jsonEditorOptions;
    """
    def ExtendedChoiceParameterDefinition param = new ExtendedChoiceParameterDefinition(
                                    'Form', //String name,
                                    'PT_JSON', //String type,
                                    null, //String value,
                                    null, //String projectName,
                                    null, //String propertyFile,
                                    jsonGroovyScript, //String groovyScript,
                                    null, //String groovyScriptFile,
                                    "parametersJSONOut=$parametersJSONOut", //String bindings,
                                    '', //String groovyClasspath,
                                    null, //String propertyKey,
                                    null, //String defaultValue,
                                    null, //String defaultPropertyFile,
                                    null, //String defaultGroovyScript,
                                    null, //String defaultGroovyScriptFile,
                                    null, //String defaultBindings,
                                    null, //String defaultGroovyClasspath,
                                    null, //String defaultPropertyKey,
                                    null, //String descriptionPropertyValue,
                                    null, //String descriptionPropertyFile,
                                    null, //String descriptionGroovyScript,
                                    null, //String descriptionGroovyScriptFile,
                                    null, //String descriptionBindings,
                                    null, //String descriptionGroovyClasspath,
                                    null, //String descriptionPropertyKey,
                                    null, //String javascriptFile,
                                    null, //String javascript,
                                    false, //boolean saveJSONParameterToFile,
                                    false, //boolean quoteValue,
                                    5, //int visibleItemCount,
                                    '', //String description,
                                    ',' //String multiSelectDelimiter
                                )


    return param;
}
private def Boolean DetectParameterChange(pForm) {
    //This function compares the user parameters with the original file parameters.
    // if there's a change in the values(!) it will return true, else false.
    if (new JsonSlurperClassic().parseText(pForm) != ReadParametersFromFile().startval) 
    {
        print "detected parameters change!"
        return true
    }
    return false
}
private def void SaveParametersToJob (boolean pShowSaveParameters, boolean pDefaultSaveParametersValue) {
    //This function saves the parameters to the job, for the next run.
    ArrayList params = []
    ArrayList props = []

    params << BuildParameters()
    def saveParameters = booleanParam(defaultValue: pDefaultSaveParametersValue, description: '',name: "SaveParameters")
    if (pShowSaveParameters) {
        props << parameters(params + saveParameters)
    }
    else {
        SaveParameters = pDefaultSaveParametersValue
        props << parameters(params)
    }

    properties(props)
}