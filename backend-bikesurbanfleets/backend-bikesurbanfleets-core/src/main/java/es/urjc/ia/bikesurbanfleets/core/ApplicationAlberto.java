package es.urjc.ia.bikesurbanfleets.core;

import es.urjc.ia.bikesurbanfleets.common.util.JsonValidation;
import es.urjc.ia.bikesurbanfleets.common.util.JsonValidation.ValidationParams;
import es.urjc.ia.bikesurbanfleets.common.util.MessageGuiFormatter;
import es.urjc.ia.bikesurbanfleets.core.config.GlobalInfo;
import es.urjc.ia.bikesurbanfleets.core.config.*;
import es.urjc.ia.bikesurbanfleets.core.core.SimulationEngine;
import es.urjc.ia.bikesurbanfleets.core.exceptions.ValidationException;
import es.urjc.ia.bikesurbanfleets.defaultConfiguration.GlobalConfigurationParameters;
import es.urjc.ia.bikesurbanfleets.resultanalysis.SimulationResultAnalyser;
import es.urjc.ia.bikesurbanfleets.services.graphManager.GraphManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;

public class ApplicationAlberto {

    //Program parameters
    private static String globalSchema;
    private static String usersSchema;
    private static String stationsSchema;
    private static String globalConfig;
    private static String usersConfig;
    private static String stationsConfig;
    private static String historyOutputPath;
    private static String analysisOutputPath;
    private static String validator;
    private static boolean callFromFrontend;

    private static String PROJECT_HOME = "/Users/albertofernandez/git/Bike3S/";

    public static void main(String[] args) throws Exception {

        //Create auxiliary folder
        File auxiliaryDir = new File(GlobalConfigurationParameters.TEMP_DIR);
        if (!auxiliaryDir.exists()) {
            auxiliaryDir.mkdirs();
        }

        globalSchema = "";
        usersSchema = "";
        stationsSchema = "";
//        String test="informed";
        String test = "paperAT2018/inf";
        globalConfig = PROJECT_HOME + "Bike3STests/" + test + "/conf/global_configuration.json";
        usersConfig = PROJECT_HOME + "Bike3STests/" + test + "/conf/users_configuration.json";
        stationsConfig = PROJECT_HOME + "Bike3STests/" + test + "/conf/stations_configuration.json";
        historyOutputPath = PROJECT_HOME + "Bike3STests/" + test + "/hist";
        analysisOutputPath= PROJECT_HOME + "Bike3STests/" + test +"/analysis";
        validator = "";
        callFromFrontend = true;

        //     checkParams(); // If not valid, throws exception
        try {
            //1.read global configuration (and setup some changes)
            //to do maybe change graph parameters into the global config file
            ConfigJsonReader jsonReader = new ConfigJsonReader(globalConfig, stationsConfig, usersConfig);
            GlobalInfo globalInfo = jsonReader.readGlobalConfiguration();
            if (historyOutputPath != null) {
                globalInfo.setOtherHistoryOutputPath(historyOutputPath);
            }
            //2. load Graph Manager
            GraphManager graphManager=GraphManager.getGraphManager(globalInfo);

            //3. read stations and user configurations
            UsersConfig usersInfo = jsonReader.readUsersConfiguration();
            StationsConfig stationsInfo = jsonReader.readStationsConfiguration();

            //4. do simulation
            new SimulationEngine(globalInfo, stationsInfo, usersInfo, graphManager);

            //5. analyse the simulation results
            SimulationResultAnalyser sra = new SimulationResultAnalyser(analysisOutputPath, historyOutputPath,graphManager);
            sra.analyzeSimulation();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void checkParams() throws Exception {

        String exMessage = null; // Message for exceptions
        String warningMessage = null;
        if (hasAllSchemasAndConfig()) {

            ValidationParams vParams = new ValidationParams();
            vParams.setSchemaDir(globalSchema).setJsonDir(globalConfig).setJsValidatorDir(validator);
            String globalConfigValidation = validate(vParams);

            vParams.setSchemaDir(usersSchema).setJsonDir(usersConfig);
            String usersConfigValidation = validate(vParams);

            vParams.setSchemaDir(stationsSchema).setJsonDir(stationsConfig);
            String stationsConfigValidation = validate(vParams);

            System.out.println(globalConfigValidation);
            System.out.println(usersConfigValidation);
            System.out.println(stationsConfigValidation);

            if ((!globalConfigValidation.equals("OK")
                    || !usersConfigValidation.equals("OK") || !stationsConfigValidation.equals("OK"))) {

                exMessage = "JSON has errors \n Global configuration errors \n" + globalConfigValidation + "\n"
                        + "Stations configuration errors \n" + stationsConfigValidation + "\n"
                        + "Users configuration errors \n" + usersConfigValidation;

            } else if (globalConfigValidation.equals("NODE_NOT_INSALLED")) {

                exMessage = "Node is necessary to execute validator: " + validator + ". \n"
                        + "Verify if node is installed or install node";

            } else if (globalConfigValidation.equals("OK") && stationsConfigValidation.equals("OK")
                    && usersConfigValidation.equals("OK")) {

                System.out.println("Validation configuration input: OK\n");
            }
        } else if (globalConfig == null || stationsConfig == null || usersConfig == null) {
            exMessage = "You should specify a configuration file";
        } else if ((globalSchema == null || usersSchema == null || stationsSchema == null) && validator != null) {
            exMessage = "You should specify all schema paths";

        } else if (validator == null && !callFromFrontend) {
            warningMessage = "Warning: you don't specify a validator, configuration file will not be validated on backend";
        }

        if (exMessage != null) {
            System.out.println("Exception");
            throw new ValidationException(exMessage);
        }

        if (warningMessage != null) {
            System.out.println(warningMessage);
        }
    }

    private static boolean hasAllSchemasAndConfig() {
        return globalSchema != null && usersSchema != null && stationsSchema != null && globalConfig != null
                && stationsConfig != null && validator != null;
    }

    private static String validate(ValidationParams vParams) throws Exception {
        return JsonValidation.validate(vParams);
    }

}
